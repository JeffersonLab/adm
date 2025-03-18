package org.jlab.adm.business.session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.PermitAll;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.jlab.adm.persistence.entity.RemoteCommandResult;
import org.jlab.adm.presentation.controller.Deploy;
import org.jlab.smoothness.business.exception.UserFriendlyException;

@Stateless
public class SSHFacade {

  private static final Logger LOGGER = Logger.getLogger(Deploy.class.getName());

  final Duration verifyTimeout = Duration.ofSeconds(5);
  final Duration authTimeout = Duration.ofSeconds(5);

  @Asynchronous
  @PermitAll
  public void asyncExecuteRemoteCommand(String username, String hostname, int port, String command)
      throws UserFriendlyException {
    RemoteCommandResult result = null;
    Instant start = Instant.now();

    try {
      result = executeRemoteCommand(username, hostname, port, command);
    } catch (Throwable t) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      t.printStackTrace(pw);

      result = new RemoteCommandResult(sw.toString());
    }

    result.setStart(start);
    result.setEnd(Instant.now());
  }

  private RemoteCommandResult executeRemoteCommand(
      String username, String hostname, int port, String command) throws IOException {
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
