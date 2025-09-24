package org.jlab.adm.business.session;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.client.SshClient;
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

  @EJB DeployJobFacade deployJobFacade;

  @Asynchronous
  @PermitAll
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

  private void executeRemoteCommand(DeployJob job) throws IOException {

    AppEnv env = job.getAppEnv();
    String version = job.getVersion();
    String username = env.getRunServiceUsername();
    String hostname = env.getHostname();
    int port = env.getPort();
    String command = env.getDeployCommand();

    // The template expectation is to append version as last argument to the deploy command
    command = command + " " + version;

    LOGGER.log(
        Level.INFO, "execute " + username + "@" + hostname + ":" + port + " \"" + command + "\"");
    SshClient client = SshClient.setUpDefaultClient();

    int exitCode = 0;
    String out;
    String err;

    client.start();

    try (ClientSession session =
        client.connect(username, hostname, port).verify(verifyTimeout).getSession()) {
      session.auth().verify(authTimeout);

      try (ByteArrayOutputStream stdout = new ByteArrayOutputStream();
          ByteArrayOutputStream stderr = new ByteArrayOutputStream()) {

        // Throws RemoteException if exitcode != 0
        session.executeRemoteCommand(command, stdout, stderr, StandardCharsets.UTF_8);

        out = stdout.toString(StandardCharsets.UTF_8);
        err = stderr.toString(StandardCharsets.UTF_8);
      } // try with resources automatically calls stdout/stderr .close()
    } // try with resources automatically calls session.close()

    client.stop();
    client.close();

    job.setExitCode(exitCode);
    job.setOut(out);
    job.setErr(err);
  }
}
