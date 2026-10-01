package org.jlab.adm.business.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ejb.SessionContext;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import org.jlab.adm.persistence.entity.App;
import org.jlab.adm.persistence.entity.AppEnv;
import org.jlab.adm.persistence.entity.DeployJob;
import org.jlab.smoothness.business.exception.UserFriendlyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DeployerFacadeTest {

  private static final String APP = "testapp";
  private static final String ENV = "local-demo";
  private static final String SERVICE_USER = "deployer-service";

  private final AppEnv appEnv =
      new AppEnv(new App(APP, null), ENV, SERVICE_USER, "testuser", "sshd", 22, "deploy.sh");

  /** Jobs handed to the SSHFacade to run */
  private final List<DeployJob> started = new ArrayList<>();

  @Test
  void requestServiceUserCanDeploy() throws Exception {
    BigInteger jobId = facade(SERVICE_USER, false).deploy(ENV, APP, "1.2.3");

    assertEquals(BigInteger.ONE, jobId);
    assertEquals(1, started.size());
    assertEquals(appEnv, started.get(0).getAppEnv());
    assertEquals("1.2.3", started.get(0).getVersion());
  }

  @Test
  void adminCanDeploy() throws Exception {
    facade("someone", true).deploy(ENV, APP, "1.2.3");

    assertEquals(1, started.size());
  }

  @Test
  void otherUserCannotDeploy() {
    UserFriendlyException e =
        assertThrows(
            UserFriendlyException.class, () -> facade("someone", false).deploy(ENV, APP, "1.2.3"));

    assertTrue(e.getMessage().contains("not authorized"), e.getMessage());
    assertTrue(started.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "anonymous", "ANONYMOUS"})
  void anonymousCannotDeploy(String caller) {
    UserFriendlyException e =
        assertThrows(
            UserFriendlyException.class, () -> facade(caller, true).deploy(ENV, APP, "1.2.3"));

    assertTrue(e.getMessage().contains("authenticate"), e.getMessage());
    assertTrue(started.isEmpty());
  }

  @Test
  void unknownAppEnvIsRejected() {
    UserFriendlyException e =
        assertThrows(
            UserFriendlyException.class,
            () -> facade(SERVICE_USER, false).deploy("prod", APP, "1.2.3"));

    assertTrue(e.getMessage().contains("AppEnv not found"), e.getMessage());
    assertTrue(started.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.0.0", "10.20.30", "1.0.0-rc.1", "1.0.0+build.5", "1.0.0-alpha-1+abc"})
  void semverIsAccepted(String version) throws Exception {
    facade(SERVICE_USER, false).deploy(ENV, APP, version);

    assertEquals(version, started.get(0).getVersion());
  }

  // The version is appended to the deploy command run in a shell, so anything else is rejected
  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "1.0",
        "v1.0.0",
        "01.0.0",
        "1.0.0 ",
        "1.0.0\n",
        "1.0.0; rm -rf /",
        "1.0.0 && reboot",
        "1.0.0|id",
        "1.0.0-$(id)",
        "1.0.0-`id`"
      })
  void nonSemverIsRejected(String version) {
    UserFriendlyException e =
        assertThrows(
            UserFriendlyException.class,
            () -> facade(SERVICE_USER, false).deploy(ENV, APP, version));

    assertTrue(e.getMessage().contains("semver"), e.getMessage());
    assertTrue(started.isEmpty());
  }

  private DeployerFacade facade(String caller, boolean admin) {
    DeployerFacade facade = new DeployerFacade();

    facade.context = sessionContext(caller, admin);

    facade.appEnvFacade =
        new AppEnvFacade() {
          @Override
          public AppEnv find(String app, String env) {
            return APP.equals(app) && ENV.equals(env) ? appEnv : null;
          }
        };

    facade.deployJobFacade =
        new DeployJobFacade() {
          @Override
          public BigInteger createReturnId(DeployJob job) {
            job.setDeployJobId(BigInteger.ONE);
            return job.getDeployJobId();
          }
        };

    facade.sshFacade =
        new SSHFacade() {
          @Override
          public void asyncExecuteRemoteCommand(DeployJob job) {
            started.add(job);
          }
        };

    return facade;
  }

  private static SessionContext sessionContext(String caller, boolean admin) {
    return (SessionContext)
        Proxy.newProxyInstance(
            SessionContext.class.getClassLoader(),
            new Class<?>[] {SessionContext.class},
            (proxy, method, args) ->
                switch (method.getName()) {
                  case "getCallerPrincipal" -> (Principal) () -> caller;
                  case "isCallerInRole" -> admin && "adm-admin".equals(args[0]);
                  default -> throw new UnsupportedOperationException(method.getName());
                });
  }
}
