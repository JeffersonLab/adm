package org.jlab.adm.business.session;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.jlab.adm.persistence.entity.AppEnv;
import org.jlab.adm.persistence.entity.DeployJob;
import org.jlab.adm.presentation.controller.Deploy;
import org.jlab.smoothness.business.exception.UserFriendlyException;

@Stateless
public class SSHFacade {

  private static final Logger LOGGER = Logger.getLogger(Deploy.class.getName());

  final Duration verifyTimeout = Duration.ofSeconds(5);
  final Duration authTimeout = Duration.ofSeconds(5);
  final Duration openTimeout = Duration.ofSeconds(5);
  // A deploy command can hang forever, such as on a prompt to accept a certificate.
  // Not final so tests can shorten it.
  Duration commandTimeout = Duration.ofMinutes(10);

  @EJB DeployJobFacade deployJobFacade;

  // No transaction while the command runs, as it can outlast the transaction timeout;
  // deployJobFacade.edit uses its own
  @Asynchronous
  @PermitAll
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void asyncExecuteRemoteCommand(DeployJob job) throws UserFriendlyException {
    try {
      executeRemoteCommand(job);
    } catch (Throwable t) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      t.printStackTrace(pw);

      job.setStackTrace(sw.toString());
    }

    job.setEnd(new Date());

    deployJobFacade.edit(job);
  }

  /**
   * Runs the deploy command on each of the env's hosts in turn, and stops at the first that fails,
   * so a version that fails on one host doesn't reach the rest. The job keeps the last exit code.
   */
  private void executeRemoteCommand(DeployJob job) throws IOException {

    AppEnv env = job.getAppEnv();
    List<String> hostnames = env.getHostnames();
    String command = env.getDeployCommand();

    // The template expectation is to append version as last argument to the deploy command
    command = command + " " + job.getVersion();

    // With several hosts, each host's output is headed by its name
    boolean severalHosts = hostnames.size() > 1;

    // One timeout for the whole job, so clients wait no longer for several hosts than for one
    Instant deadline = Instant.now().plus(commandTimeout);

    SshClient client = createClient();

    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    client.start();

    try {
      for (int i = 0; i < hostnames.size(); i++) {
        String hostname = hostnames.get(i);
        ByteArrayOutputStream hostStdout = new ByteArrayOutputStream();
        ByteArrayOutputStream hostStderr = new ByteArrayOutputStream();

        // Not a previous host's, if this one fails to run or finish
        job.setExitCode(null);

        try {
          Duration timeLeft = Duration.between(Instant.now(), deadline);

          // Don't start a command on the next host if the job is out of time
          if (timeLeft.isNegative() || timeLeft.isZero()) {
            throw timeout();
          }

          job.setExitCode(
              executeOnHost(client, env, hostname, command, timeLeft, hostStdout, hostStderr));
        } catch (IOException | RuntimeException e) {
          // Such as an unknown host, which is a RuntimeException
          if (severalHosts) {
            throw new IOException(failedOn(hostnames, i), e);
          }

          throw e;
        } finally {
          // Record output whatever happened, as it often explains a failure
          append(stdout, severalHosts, hostname, hostStdout, true);
          append(stderr, severalHosts, hostname, hostStderr, false);
        }

        if (job.getExitCode() != 0) {
          break;
        }
      }
    } finally {
      job.setOut(stdout.toString(StandardCharsets.UTF_8));
      job.setErr(stderr.toString(StandardCharsets.UTF_8));

      client.stop();
      client.close();
    }
  }

  /**
   * Runs the command on one host, waiting at most the timeout for it, and returns its exit code.
   */
  private Integer executeOnHost(
      SshClient client,
      AppEnv env,
      String hostname,
      String command,
      Duration timeout,
      ByteArrayOutputStream stdout,
      ByteArrayOutputStream stderr)
      throws IOException {
    String username = env.getRunServiceUsername();
    int port = env.getPort();

    LOGGER.log(
        Level.INFO, "execute " + username + "@" + hostname + ":" + port + " \"" + command + "\"");

    try (ClientSession session =
        client.connect(username, hostname, port).verify(verifyTimeout).getSession()) {
      session.auth().verify(authTimeout);

      // Use an exec channel instead of session.executeRemoteCommand, which throws if the exit code
      // is not 0, losing the exit code (and our stdout/stderr) in the process
      try (ChannelExec channel = session.createExecChannel(command)) {
        channel.setOut(stdout);
        channel.setErr(stderr);
        // No input, so a command that asks for some, such as a prompt to accept a certificate,
        // fails at once instead of waiting until it times out
        channel.setIn(InputStream.nullInputStream());
        channel.open().verify(openTimeout);

        Set<ClientChannelEvent> events =
            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), timeout);

        if (events.contains(ClientChannelEvent.TIMEOUT)) {
          throw timeout();
        }

        Integer exitCode = channel.getExitStatus();

        if (exitCode == null) {
          throw new IOException("No exit code received; exit signal: " + channel.getExitSignal());
        }

        return exitCode;
      } // try with resources automatically calls channel.close()
    } // try with resources automatically calls session.close()
  }

  /**
   * Adds a host's output to the job's. With several hosts, each host's stdout is headed by its
   * name, even if empty, to show which hosts the job tried; its stderr only if it has some.
   */
  private static void append(
      ByteArrayOutputStream all,
      boolean severalHosts,
      String hostname,
      ByteArrayOutputStream host,
      boolean evenIfEmpty) {
    if (severalHosts && (evenIfEmpty || host.size() > 0)) {
      byte[] bytes = all.toByteArray();

      // Start the header on its own line
      if (bytes.length > 0 && bytes[bytes.length - 1] != '\n') {
        all.write('\n');
      }

      all.writeBytes(("== " + hostname + " ==\n").getBytes(StandardCharsets.UTF_8));
    }

    all.writeBytes(host.toByteArray());
  }

  private SocketTimeoutException timeout() {
    return new SocketTimeoutException(
        "Deploy command did not finish within " + commandTimeout.toSeconds() + " seconds");
  }

  /** Says which host failed, and which hosts after it the job didn't deploy to */
  private static String failedOn(List<String> hostnames, int failed) {
    String message = "Deploy to " + hostnames.get(failed) + " failed";
    List<String> rest = hostnames.subList(failed + 1, hostnames.size());

    if (!rest.isEmpty()) {
      message = message + "; not deployed to " + String.join(", ", rest);
    }

    return message;
  }

  /** Tests override this to use their own key and server. */
  SshClient createClient() {
    return SshClient.setUpDefaultClient();
  }
}
