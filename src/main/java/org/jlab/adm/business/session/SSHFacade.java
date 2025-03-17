package org.jlab.adm.business.session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.jlab.adm.persistence.model.RemoteCommandResult;
import org.jlab.adm.presentation.controller.Deploy;

@Stateless
public class SSHFacade {

  private static final Logger LOGGER = Logger.getLogger(Deploy.class.getName());

  final Duration verifyTimeout = Duration.ofSeconds(5);
  final Duration authTimeout = Duration.ofSeconds(5);

  public RemoteCommandResult executeRemoteCommand(
      String username, String hostname, int port, String command) throws IOException {
    LOGGER.log(
        Level.INFO, "execute " + username + "@" + hostname + ":" + port + " \"" + command + "\"");
    SshClient client = SshClient.setUpDefaultClient();

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

    return new RemoteCommandResult(out, err);
  }
}
