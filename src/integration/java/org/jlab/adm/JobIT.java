package org.jlab.adm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.json.JsonObject;
import java.math.BigInteger;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Reads deploy jobs' status from the job endpoint, as CI does to wait for a deploy's result. */
class JobIT {

  private static String failEnv;
  private static String slowEnv;
  private static String longEnv;
  private static String badHostEnv;

  @BeforeAll
  static void addAppEnvs() throws Exception {
    Adm.awaitReady();

    failEnv =
        Adm.addAppEnv(Adm.SERVICE_ACCOUNT, "sshd", "sh -c 'echo out; echo err >&2; exit 3' #");
    slowEnv = Adm.addAppEnv(Adm.SERVICE_ACCOUNT, "sshd", "sh -c 'sleep 3; echo done' #");
    longEnv =
        Adm.addAppEnv(Adm.SERVICE_ACCOUNT, "sshd", "sh -c 'yes a | head -c 70000; echo end' #");
    badHostEnv = Adm.addAppEnv(Adm.SERVICE_ACCOUNT, "nosuchhost", "echo");
  }

  @AfterAll
  static void removeAppEnvs() throws Exception {
    for (String env : new String[] {failEnv, slowEnv, longEnv, badHostEnv}) {
      if (env != null) {
        Adm.removeAppEnv(env);
      }
    }
  }

  @Test
  void finishedJobReportsResult() throws Exception {
    String token = Adm.serviceAccountToken();
    BigInteger jobId = Adm.startDeploy(token, failEnv, "2.0.16");

    JsonObject status = Adm.awaitJobStatus(jobId, token);

    assertEquals(jobId, status.getJsonNumber("jobId").bigIntegerValue());
    assertEquals(Adm.APP, status.getString("app"));
    assertEquals(failEnv, status.getString("env"));
    assertEquals("2.0.16", status.getString("version"));
    OffsetDateTime start = OffsetDateTime.parse(status.getString("start"));
    OffsetDateTime end = OffsetDateTime.parse(status.getString("end"));
    assertTrue(!end.isBefore(start), start + " to " + end);
    assertEquals(3, status.getInt("exitCode"));
    assertEquals("out\n", status.getString("out"));
    assertEquals("err\n", status.getString("err"));
    assertTrue(status.isNull("error"), status.toString());
  }

  @Test
  void runningJobHasNoEndYet() throws Exception {
    String token = Adm.serviceAccountToken();
    BigInteger jobId = Adm.startDeploy(token, slowEnv, "1.0.0");

    JsonObject running = Adm.json(Adm.job(token, jobId.toString()));

    assertNotNull(running.getString("start"));
    assertTrue(running.isNull("end"), running.toString());
    assertTrue(running.isNull("exitCode"), running.toString());

    JsonObject finished = Adm.awaitJobStatus(jobId, token);

    assertEquals(0, finished.getInt("exitCode"));
    assertEquals("done\n", finished.getString("out"));
  }

  @Test
  void longOutputKeepsItsEnd() throws Exception {
    String token = Adm.serviceAccountToken();

    JsonObject status = Adm.awaitJobStatus(Adm.startDeploy(token, longEnv, "1.0.0"), token);

    String out = status.getString("out");
    assertEquals(64 * 1024, out.getBytes(StandardCharsets.UTF_8).length);
    assertTrue(out.endsWith("a\na\nend\n"), out.substring(out.length() - 20));
  }

  @Test
  void unknownHostIsReportedAsError() throws Exception {
    String token = Adm.serviceAccountToken();

    JsonObject status = Adm.awaitJobStatus(Adm.startDeploy(token, badHostEnv, "1.0.0"), token);

    assertTrue(status.isNull("exitCode"), status.toString());
    assertTrue(status.getString("error").contains("nosuchhost"), status.getString("error"));
  }

  @Test
  void adminCanViewJob() throws Exception {
    BigInteger jobId = Adm.startDeploy(Adm.serviceAccountToken(), failEnv, "1.0.0");

    JsonObject status = Adm.awaitJobStatus(jobId, Adm.adminToken());

    assertEquals(3, status.getInt("exitCode"));
  }

  @Test
  void otherUserIsForbidden() throws Exception {
    BigInteger jobId = Adm.startDeploy(Adm.serviceAccountToken(), failEnv, "1.0.0");

    // jadams has only the adm-user role, and is not the env's request user
    HttpResponse<String> response = Adm.job(Adm.userToken("jadams"), jobId.toString());

    assertEquals(403, response.statusCode(), response.body());
    assertEquals(
        "User jadams is not authorized to view deploy job " + jobId,
        Adm.json(response).getString("exception"));
  }

  @Test
  void unknownJobIsNotFound() throws Exception {
    HttpResponse<String> response = Adm.job(Adm.serviceAccountToken(), "999999999999");

    assertEquals(404, response.statusCode(), response.body());
    assertEquals("Deploy job 999999999999 not found", Adm.json(response).getString("exception"));
  }

  @Test
  void invalidIdIsBadRequest() throws Exception {
    HttpResponse<String> response = Adm.job(Adm.serviceAccountToken(), "1; drop table");

    assertEquals(400, response.statusCode(), response.body());
    assertEquals("Parameter id must be a job ID", Adm.json(response).getString("exception"));
  }

  @Test
  void noTokenRedirectsToLogin() throws Exception {
    HttpResponse<String> response = Adm.job(null, "1");

    assertEquals(302, response.statusCode());
    String location = response.headers().firstValue("Location").orElse(null);
    assertNotNull(location);
    assertTrue(location.contains("/protocol/openid-connect/auth"), location);
  }

  @Test
  void invalidTokenIsUnauthorized() throws Exception {
    // A token whose signature does not verify, which the server treats as it does an expired one:
    // a 401 that a polling client can fail on, not a redirect to the login page
    String token = Adm.serviceAccountToken();
    String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "AAAA";

    HttpResponse<String> response = Adm.job(tampered, "1");

    assertEquals(401, response.statusCode(), response.body());
    String challenge = response.headers().firstValue("WWW-Authenticate").orElse("");
    assertTrue(challenge.contains("invalid_token"), challenge);
  }
}
