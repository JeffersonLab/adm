package org.jlab.adm.business.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.jlab.adm.persistence.entity.App;
import org.jlab.adm.persistence.entity.AppEnv;
import org.jlab.adm.persistence.entity.DeployJob;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Runs deploy commands against an in-process SSH server whose commands are scripted below. */
class SSHFacadeTest {

  private static final String USERNAME = "testuser";

  private static KeyPair clientKey;
  private static SshServer server;

  /** The job as saved at the end */
  private DeployJob saved;

  @BeforeAll
  static void startServer() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
    generator.initialize(256);
    clientKey = generator.generateKeyPair();

    server = SshServer.setUpDefaultServer();
    server.setHost("localhost");
    server.setPort(0);
    server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
    server.setPublickeyAuthenticator(
        (username, key, session) ->
            USERNAME.equals(username) && KeyUtils.compareKeys(key, clientKey.getPublic()));
    server.setCommandFactory((channel, command) -> new ScriptedCommand(command));
    server.start();
  }

  @AfterAll
  static void stopServer() throws IOException {
    server.stop();
  }

  @Test
  void versionIsAppendedToCommand() throws Exception {
    DeployJob job = run(facade(), USERNAME, "echo deploying myapp");

    assertEquals(0, job.getExitCode());
    assertEquals("deploying myapp 1.2.3\n", job.getOut());
    assertEquals("", job.getErr());
    assertNull(job.getStackTrace());
  }

  @Test
  void nonzeroExitKeepsExitCodeAndOutput() throws Exception {
    DeployJob job = run(facade(), USERNAME, "fail");

    assertEquals(3, job.getExitCode());
    assertEquals("out\n", job.getOut());
    assertEquals("err\n", job.getErr());
    assertNull(job.getStackTrace());
  }

  @Test
  void promptGetsNoInput() throws Exception {
    DeployJob job = run(facade(), USERNAME, "prompt");

    assertEquals(1, job.getExitCode());
    assertEquals("Accept certificate? ", job.getOut());
    assertEquals("no answer\n", job.getErr());
    assertNull(job.getStackTrace());
  }

  @Test
  void commandTimesOut() throws Exception {
    SSHFacade facade = facade();
    facade.commandTimeout = Duration.ofSeconds(1);

    DeployJob job = run(facade, USERNAME, "hang");

    assertNull(job.getExitCode());
    assertEquals("partial\n", job.getOut());
    assertTrue(
        job.getStackTrace().startsWith("java.net.SocketTimeoutException"), job.getStackTrace());
  }

  @Test
  void failedLoginIsRecorded() throws Exception {
    DeployJob job = run(facade(), "nobody", "echo");

    assertNull(job.getExitCode());
    assertEquals("", job.getOut());
    assertEquals("", job.getErr());
    assertNotNull(job.getStackTrace());
  }

  private DeployJob run(SSHFacade facade, String username, String deployCommand) throws Exception {
    AppEnv env =
        new AppEnv(
            new App("testapp", null),
            "test",
            "deployer-service",
            username,
            "localhost",
            server.getPort(),
            deployCommand);
    DeployJob job = new DeployJob(env, "1.2.3");

    facade.asyncExecuteRemoteCommand(job);

    assertSame(job, saved);
    assertNotNull(job.getEnd());

    return job;
  }

  private SSHFacade facade() {
    SSHFacade facade =
        new SSHFacade() {
          @Override
          SshClient createClient() {
            // Use the test key and server, and nothing from the user's ~/.ssh
            SshClient client = SshClient.setUpDefaultClient();
            client.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(clientKey));
            client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
            client.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);
            return client;
          }
        };
    // Short enough that a command left waiting fails the test quickly
    facade.commandTimeout = Duration.ofSeconds(5);

    facade.deployJobFacade =
        new DeployJobFacade() {
          @Override
          public DeployJob edit(DeployJob job) {
            saved = job;
            return job;
          }
        };

    return facade;
  }

  /** A remote command whose behavior is chosen by its first word. */
  private static class ScriptedCommand implements Command {
    private final String command;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback exitCallback;
    private Thread thread;

    ScriptedCommand(String command) {
      this.command = command;
    }

    @Override
    public void setInputStream(InputStream in) {
      this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
      this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
      this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback exitCallback) {
      this.exitCallback = exitCallback;
    }

    @Override
    public void start(ChannelSession channel, Environment env) {
      thread = new Thread(this::run);
      thread.start();
    }

    @Override
    public void destroy(ChannelSession channel) {
      thread.interrupt();
    }

    private void run() {
      String[] words = command.split(" ", 2);
      try {
        switch (words[0]) {
          case "echo" -> {
            write(out, words.length > 1 ? words[1] + "\n" : "\n");
            exitCallback.onExit(0);
          }
          case "fail" -> {
            write(out, "out\n");
            write(err, "err\n");
            exitCallback.onExit(3);
          }
          case "prompt" -> {
            write(out, "Accept certificate? ");
            if (in.read() == -1) {
              write(err, "no answer\n");
              exitCallback.onExit(1);
            } else {
              exitCallback.onExit(0);
            }
          }
          case "hang" -> {
            write(out, "partial\n");
            Thread.sleep(Duration.ofMinutes(1).toMillis());
            exitCallback.onExit(0);
          }
          default -> {
            write(err, "unknown command: " + command + "\n");
            exitCallback.onExit(127);
          }
        }
      } catch (IOException | InterruptedException e) {
        // The channel was closed
      }
    }

    private static void write(OutputStream stream, String text) throws IOException {
      stream.write(text.getBytes(StandardCharsets.UTF_8));
      stream.flush();
    }
  }
}
