package org.jlab.adm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.json.JsonObject;
import java.net.http.HttpResponse;
import java.util.Map;
import org.jlab.adm.Adm.Job;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Deploys to the sshd container through the app, as users and as CI's service account. */
class DeployIT {

  private static String echoEnv;
  private static String failEnv;
  private static String promptEnv;
  private static String badHostEnv;

  @BeforeAll
  static void addAppEnvs() throws Exception {
    Adm.awaitReady();

    echoEnv = Adm.addAppEnv(Adm.SERVICE_ACCOUNT, "sshd", "echo deployed");
    failEnv =
        Adm.addAppEnv(Adm.SERVICE_ACCOUNT, "sshd", "sh -c 'echo out; echo err >&2; exit 3' #");
    promptEnv =
        Adm.addAppEnv(
            Adm.SERVICE_ACCOUNT,
            "sshd",
            "sh -c 'printf \"Accept? \"; read -r x || { echo no answer >&2; exit 1; }' #");
    badHostEnv = Adm.addAppEnv(Adm.SERVICE_ACCOUNT, "nosuchhost", "echo");
  }

  @AfterAll
  static void removeAppEnvs() throws Exception {
    for (String env : new String[] {echoEnv, failEnv, promptEnv, badHostEnv}) {
      if (env != null) {
        Adm.removeAppEnv(env);
      }
    }
  }

  @Test
  void adminDeploysDemoEnv() throws Exception {
    // The demo env's command is "touch /tmp/hello && echo"
    Job job = Adm.deployAndWait(Adm.adminToken(), Adm.DEMO_ENV, "1.0.0");

    assertEquals(new Job(0, "1.0.0\n", "", ""), job);
  }

  @Test
  void serviceAccountDeploysLikeCi() throws Exception {
    Job job = Adm.deployAndWait(Adm.serviceAccountToken(), echoEnv, "2.3.4-rc.1");

    assertEquals(new Job(0, "deployed 2.3.4-rc.1\n", "", ""), job);
  }

  @Test
  void failedCommandKeepsExitCodeAndOutput() throws Exception {
    Job job = Adm.deployAndWait(Adm.serviceAccountToken(), failEnv, "1.0.0");

    assertEquals(new Job(3, "out\n", "err\n", ""), job);
  }

  @Test
  void promptGetsNoInput() throws Exception {
    Job job = Adm.deployAndWait(Adm.serviceAccountToken(), promptEnv, "1.0.0");

    assertEquals(new Job(1, "Accept? ", "no answer\n", ""), job);
  }

  @Test
  void unknownHostIsRecorded() throws Exception {
    Job job = Adm.deployAndWait(Adm.serviceAccountToken(), badHostEnv, "1.0.0");

    assertNull(job.exitCode());
    assertEquals("", job.out());
    assertTrue(job.stackTrace().contains("nosuchhost"), job.stackTrace());
  }

  @Test
  void otherUserIsRefused() throws Exception {
    // jadams has only the adm-user role, and is not the env's request user
    JsonObject result = Adm.deploy(Adm.userToken("jadams"), echoEnv, Adm.APP, "1.0.0");

    assertEquals(
        "User jadams is not authorized to deploy app testapp to env " + echoEnv,
        result.getString("exception"));
  }

  @Test
  void invalidVersionIsRefused() throws Exception {
    JsonObject result =
        Adm.deploy(Adm.serviceAccountToken(), echoEnv, Adm.APP, "1.0.0; touch /tmp/pwned");

    assertEquals("Version string must be semver formatted", result.getString("exception"));
  }

  @Test
  void missingVersionIsRefused() throws Exception {
    JsonObject result = Adm.deploy(Adm.serviceAccountToken(), echoEnv, Adm.APP, null);

    assertEquals("Version string must be semver formatted", result.getString("exception"));
  }

  @Test
  void unknownEnvIsRefused() throws Exception {
    JsonObject result = Adm.deploy(Adm.serviceAccountToken(), "no-such-env", Adm.APP, "1.0.0");

    assertEquals(
        "AppEnv not found for app testapp and env no-such-env", result.getString("exception"));
  }

  @Test
  void noTokenRedirectsToLogin() throws Exception {
    HttpResponse<String> response =
        Adm.post("/deploy", null, Map.of("env", echoEnv, "app", Adm.APP, "ver", "1.0.0"));

    assertEquals(302, response.statusCode());
    String location = response.headers().firstValue("Location").orElse(null);
    assertNotNull(location);
    assertTrue(location.contains("/protocol/openid-connect/auth"), location);
  }
}
