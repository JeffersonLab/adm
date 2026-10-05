package org.jlab.adm.presentation.controller;

import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlab.adm.business.session.DeployerFacade;
import org.jlab.adm.persistence.entity.AppEnv;
import org.jlab.adm.persistence.entity.DeployJob;
import org.jlab.smoothness.business.exception.UserFriendlyException;

/** Reports a deploy job's status as JSON, so clients such as CI can wait for a deploy's result. */
@WebServlet(
    name = "JobStatus",
    urlPatterns = {"/job"})
public class JobStatus extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(JobStatus.class.getName());

  /** Longer output is cut to its end, which usually holds the error */
  static final int MAX_TEXT_BYTES = 64 * 1024;

  @EJB DeployerFacade deployerFacade;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String id = request.getParameter("id");

    // The DEPLOY_JOB_ID column holds up to 22 digits
    if (id == null || !id.matches("\\d{1,22}")) {
      write(response, HttpServletResponse.SC_BAD_REQUEST, error("Parameter id must be a job ID"));
      return;
    }

    BigInteger jobId = new BigInteger(id);
    DeployJob job;

    try {
      job = deployerFacade.findJob(jobId);
    } catch (UserFriendlyException e) {
      write(response, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
      return;
    }

    if (job == null) {
      write(
          response, HttpServletResponse.SC_NOT_FOUND, error("Deploy job " + jobId + " not found"));
      return;
    }

    write(response, HttpServletResponse.SC_OK, toJson(job));
  }

  /** The job's status: its end and exit code are null until it finishes */
  static JsonObject toJson(DeployJob job) {
    AppEnv appEnv = job.getAppEnv();
    JsonObjectBuilder json = Json.createObjectBuilder();

    json.add("jobId", job.getDeployJobId());
    add(json, "app", appEnv.getApp().getName());
    add(json, "env", appEnv.getName());
    add(json, "version", job.getVersion());
    add(json, "start", timestamp(job.getStart()));
    add(json, "end", timestamp(job.getEnd()));

    if (job.getExitCode() == null) {
      json.addNull("exitCode");
    } else {
      json.add("exitCode", job.getExitCode());
    }

    add(json, "out", tail(job.getOut(), MAX_TEXT_BYTES));
    add(json, "err", tail(job.getErr(), MAX_TEXT_BYTES));
    // Why the command failed to run or finish, such as an unknown host or a timeout
    add(json, "error", tail(job.getStackTrace(), MAX_TEXT_BYTES));

    return json.build();
  }

  /** The end of the text, at most maxBytes long in UTF-8 */
  static String tail(String text, int maxBytes) {
    if (text == null) {
      return null;
    }

    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

    if (bytes.length <= maxBytes) {
      return text;
    }

    int start = bytes.length - maxBytes;

    // Start at a character, not inside one: UTF-8 continuation bytes are 10xxxxxx
    while (start < bytes.length && (bytes[start] & 0xC0) == 0x80) {
      start++;
    }

    return new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8);
  }

  // Jobs are stored in the server's local time; the offset makes the time unambiguous to clients
  private static String timestamp(Date date) {
    if (date == null) {
      return null;
    }

    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
        date.toInstant().atZone(ZoneId.systemDefault()).truncatedTo(ChronoUnit.MILLIS));
  }

  private static void add(JsonObjectBuilder json, String name, String value) {
    if (value == null) {
      json.addNull(name);
    } else {
      json.add(name, value);
    }
  }

  private static JsonObject error(String message) {
    return Json.createObjectBuilder().add("exception", message).build();
  }

  private static void write(HttpServletResponse response, int status, JsonObject json)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    // Clients poll for the status until the job ends
    response.setHeader("Cache-Control", "no-store");

    PrintWriter pw = response.getWriter();

    pw.write(json.toString());

    pw.flush();

    if (pw.checkError()) {
      LOGGER.log(Level.SEVERE, "PrintWriter Error");
    }
  }
}
