package org.jlab.adm.business.session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.PermitAll;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.jlab.adm.persistence.entity.AppEnv;
import org.jlab.adm.persistence.entity.RemoteCommandResult;
import org.jlab.adm.presentation.controller.Deploy;
import org.jlab.smoothness.business.exception.UserFriendlyException;

@Stateless
public class SSHFacade {

  private static final Logger LOGGER = Logger.getLogger(Deploy.class.getName());

  final Duration verifyTimeout = Duration.ofSeconds(5);
  final Duration authTimeout = Duration.ofSeconds(5);

  @EJB RemoteCommandResultFacade remoteCommandResultFacade;

  @Asynchronous
  @PermitAll
  public void asyncExecuteRemoteCommand(AppEnv env, String version) throws UserFriendlyException {
    RemoteCommandResult result = null;
    Date start = new Date();

    try {
      result = executeRemoteCommand(env, version);
    } catch (Throwable t) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      t.printStackTrace(pw);

      result = new RemoteCommandResult(sw.toString());
    }

    result.setAppEnv(env);
    result.setStart(start);
    result.setEnd(new Date());

    remoteCommandResultFacade.create(result);
  }

  private RemoteCommandResult executeRemoteCommand(AppEnv env, String version) throws IOException {

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
    String stackTrace = null;

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

    return new RemoteCommandResult(exitCode, out, err);
  }
}
