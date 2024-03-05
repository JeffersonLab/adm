package org.jlab.adm.business.session;

import javax.ejb.Stateless;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.jlab.adm.persistence.model.RemoteCommandResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Stateless
public class SSHFacade {
    public RemoteCommandResult executeRemoteCommand(String username, String hostname, String command) throws IOException {
        SshClient client = SshClient.setUpDefaultClient();

        final int port = 22;
        final Duration verifyTimeout = Duration.ofSeconds(5);
        final Duration authTimeout = Duration.ofSeconds(5);

        String out;
        String err;

        client.start();

        try (ClientSession session = client.connect(username, hostname, port).verify(verifyTimeout).getSession()) {
            session.auth().verify(authTimeout);

            try(ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                ByteArrayOutputStream stderr = new ByteArrayOutputStream()) {

                session.executeRemoteCommand(command, stdout, stderr, StandardCharsets.UTF_8);

                out = stdout.toString(StandardCharsets.UTF_8);
                err = stderr.toString(StandardCharsets.UTF_8);

            } // try with resources automatically calls stdout/stderr .close()
        } // try with resources automatically calls session.close()

        client.stop();

        return new RemoteCommandResult(out, err);
    }
}
