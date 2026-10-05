package org.jlab.adm.business.session;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import java.io.IOException;
import java.math.BigInteger;
import java.util.regex.Pattern;
import org.jlab.adm.persistence.entity.AppEnv;
import org.jlab.adm.persistence.entity.DeployJob;
import org.jlab.smoothness.business.exception.UserFriendlyException;

@Stateless
public class DeployerFacade {

  @Resource SessionContext context;

  @EJB SSHFacade sshFacade;

  @EJB AppEnvFacade appEnvFacade;

  @EJB DeployJobFacade deployJobFacade;

  @PermitAll
  public BigInteger deploy(String env, String app, String ver)
      throws UserFriendlyException, IOException {

    String username = authenticatedUsername("issuing a deploy command");

    AppEnv appEnv = appEnvFacade.find(app, env);

    if (appEnv == null) {
      throw new UserFriendlyException("AppEnv not found for app " + app + " and env " + env);
    }

    if (!mayDeploy(username, appEnv)) {
      throw new UserFriendlyException(
          "User " + username + " is not authorized to deploy app " + app + " to env " + env);
    }

    // Ensure version string is semver, and therefore also unlikely a shell command
    validateSemver(ver);

    DeployJob job = new DeployJob(appEnv, ver);

    BigInteger jobId = deployJobFacade.createReturnId(job);

    sshFacade.asyncExecuteRemoteCommand(job);

    return jobId;
  }

  /**
   * Finds a deploy job, to report its status. Those who may deploy to its env may see it.
   *
   * @return the job, or null if there is none with this ID
   * @throws UserFriendlyException if the caller may not see the job
   */
  @PermitAll
  public DeployJob findJob(BigInteger jobId) throws UserFriendlyException {
    String username = authenticatedUsername("viewing a deploy job");

    DeployJob job = deployJobFacade.find(jobId);

    if (job != null && !mayDeploy(username, job.getAppEnv())) {
      throw new UserFriendlyException(
          "User " + username + " is not authorized to view deploy job " + jobId);
    }

    return job;
  }

  private String authenticatedUsername(String action) throws UserFriendlyException {
    String username = context.getCallerPrincipal().getName();

    if (username == null || username.isEmpty() || username.equalsIgnoreCase("ANONYMOUS")) {
      throw new UserFriendlyException("You must authenticate before " + action);
    }

    return username;
  }

  // The env's request service user, such as CI's service account, and admins
  private boolean mayDeploy(String username, AppEnv appEnv) {
    return username.equals(appEnv.getRequestServiceUsername())
        || context.isCallerInRole("adm-admin");
  }

  private void validateSemver(String ver) throws UserFriendlyException {
    final String regex =
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";
    final Pattern p = Pattern.compile(regex);

    if (ver == null || !p.matcher(ver).matches()) {
      throw new UserFriendlyException("Version string must be semver formatted");
    }
  }
}
