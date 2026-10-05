package org.jlab.adm.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.jlab.adm.business.session.DeployerFacade;
import org.jlab.adm.persistence.entity.App;
import org.jlab.adm.persistence.entity.AppEnv;
import org.jlab.adm.persistence.entity.DeployJob;
import org.jlab.smoothness.business.exception.UserFriendlyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class JobStatusTest {

  private static final Instant START = Instant.parse("2026-10-05T14:00:00.123Z");
  private static final Instant END = Instant.parse("2026-10-05T14:00:05Z");

  private final AppEnv appEnv =
      new AppEnv(new App("testapp", null), "dev", "ci", "testuser", "sshd", 22, "deploy.sh");

  @Test
  void finishedJob() {
    DeployJob job =
        new DeployJob(
            appEnv,
            "2.0.16",
            Date.from(START),
            Date.from(END),
            3,
            "out\n",
            "err\n",
            "java.io.IOException");
    job.setDeployJobId(BigInteger.valueOf(123));

    JsonObject json = JobStatus.toJson(job);

    assertEquals(123, json.getInt("jobId"));
    assertEquals("testapp", json.getString("app"));
    assertEquals("dev", json.getString("env"));
    assertEquals("2.0.16", json.getString("version"));
    assertEquals(START, OffsetDateTime.parse(json.getString("start")).toInstant());
    assertEquals(END, OffsetDateTime.parse(json.getString("end")).toInstant());
    assertEquals(3, json.getInt("exitCode"));
    assertEquals("out\n", json.getString("out"));
    assertEquals("err\n", json.getString("err"));
    assertEquals("java.io.IOException", json.getString("error"));
  }

  @Test
  void runningJobHasNoEndOrExitCode() {
    DeployJob job = new DeployJob(appEnv, "2.0.16");
    job.setDeployJobId(BigInteger.ONE);

    JsonObject json = JobStatus.toJson(job);

    assertTrue(json.containsKey("start"));
    for (String name : new String[] {"end", "exitCode", "out", "err", "error"}) {
      assertEquals(JsonValue.NULL, json.get(name), name);
    }
  }

  @Test
  void longOutputKeepsItsEnd() {
    String out = "a".repeat(JobStatus.MAX_TEXT_BYTES) + "the error";
    DeployJob job = new DeployJob(appEnv, "1.0.0", Date.from(START), null, 1, out, out, out);
    job.setDeployJobId(BigInteger.ONE);

    JsonObject json = JobStatus.toJson(job);

    for (String name : new String[] {"out", "err", "error"}) {
      String text = json.getString(name);
      assertEquals(JobStatus.MAX_TEXT_BYTES, text.length(), name);
      assertTrue(text.endsWith("aaathe error"), name);
    }
  }

  @Test
  void tailKeepsShortText() {
    assertEquals("abc", JobStatus.tail("abc", 3));
    assertEquals("", JobStatus.tail("", 3));
    assertNull(JobStatus.tail(null, 3));
  }

  @Test
  void tailCutsAtCharacters() {
    // é is 2 bytes and € is 3 in UTF-8
    assertEquals("bc", JobStatus.tail("abc", 2));
    assertEquals("€", JobStatus.tail("é€", 4));
    assertEquals("é€", JobStatus.tail("é€", 5));
    assertEquals("", JobStatus.tail("€", 2));
    assertTrue(JobStatus.tail("€".repeat(100), 100).getBytes(StandardCharsets.UTF_8).length <= 100);
  }

  @Test
  void respondsWithJob() throws Exception {
    DeployJob job = new DeployJob(appEnv, "1.0.0");
    job.setDeployJobId(BigInteger.valueOf(42));

    Response response = get("42", id -> BigInteger.valueOf(42).equals(id) ? job : null);

    assertEquals(200, response.status);
    assertEquals(42, response.json().getInt("jobId"));
    assertEquals("application/json", response.contentType);
    assertEquals("UTF-8", response.encoding);
    assertEquals("no-store", response.headers.get("Cache-Control"));
  }

  @Test
  void unknownJobIsNotFound() throws Exception {
    Response response = get("43", id -> null);

    assertEquals(404, response.status);
    assertEquals("Deploy job 43 not found", response.json().getString("exception"));
  }

  @Test
  void unauthorizedIsForbidden() throws Exception {
    Response response =
        get(
            "42",
            id -> {
              throw new UserFriendlyException("User x is not authorized to view deploy job 42");
            });

    assertEquals(403, response.status);
    assertEquals(
        "User x is not authorized to view deploy job 42", response.json().getString("exception"));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "abc", "-1", "1.0", " 1", "12345678901234567890123"})
  void invalidIdIsBadRequest(String id) throws Exception {
    Response response =
        get(
            id,
            jobId -> {
              throw new AssertionError("looked up " + jobId);
            });

    assertEquals(400, response.status);
    assertEquals("Parameter id must be a job ID", response.json().getString("exception"));
  }

  private interface Finder {
    DeployJob find(BigInteger id) throws UserFriendlyException;
  }

  private static final class Response {
    int status = 200;
    String contentType;
    String encoding;
    final Map<String, String> headers = new HashMap<>();
    final StringWriter body = new StringWriter();

    JsonObject json() {
      return Json.createReader(new StringReader(body.toString())).readObject();
    }
  }

  private static Response get(String id, Finder finder) throws Exception {
    JobStatus servlet = new JobStatus();
    servlet.deployerFacade =
        new DeployerFacade() {
          @Override
          public DeployJob findJob(BigInteger jobId) throws UserFriendlyException {
            return finder.find(jobId);
          }
        };

    HttpServletRequest request =
        (HttpServletRequest)
            Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) ->
                    switch (method.getName()) {
                      case "getParameter" -> "id".equals(args[0]) ? id : null;
                      default -> throw new UnsupportedOperationException(method.getName());
                    });

    Response response = new Response();
    PrintWriter writer = new PrintWriter(response.body);

    HttpServletResponse servletResponse =
        (HttpServletResponse)
            Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (proxy, method, args) ->
                    switch (method.getName()) {
                      case "setStatus" -> {
                        response.status = (Integer) args[0];
                        yield null;
                      }
                      case "setContentType" -> {
                        response.contentType = (String) args[0];
                        yield null;
                      }
                      case "setCharacterEncoding" -> {
                        response.encoding = (String) args[0];
                        yield null;
                      }
                      case "setHeader" -> {
                        response.headers.put((String) args[0], (String) args[1]);
                        yield null;
                      }
                      case "getWriter" -> writer;
                      default -> throw new UnsupportedOperationException(method.getName());
                    });

    servlet.doGet(request, servletResponse);

    return response;
  }
}
