package org.jlab.adm.business.session;

import java.io.IOException;
import java.math.BigInteger;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import org.jlab.adm.persistence.entity.AppEnv;
import org.jlab.adm.persistence.entity.RemoteCommandResult;
import org.jlab.smoothness.business.exception.UserFriendlyException;

@Stateless
public class DeployerFacade {

  @Resource SessionContext context;

  @EJB SSHFacade sshFacade;

  @EJB AppEnvFacade appEnvFacade;

  @EJB RemoteCommandResultFacade remoteCommandResultFacade;

  @PermitAll
  public BigInteger deploy(String env, String app, String ver)
      throws UserFriendlyException, IOException {

    String username = context.getCallerPrincipal().getName();

    if (username == null || username.isEmpty() || username.equalsIgnoreCase("ANONYMOUS")) {
      throw new UserFriendlyException("You must authenticate before issuing a deploy command");
    }

    AppEnv appEnv = appEnvFacade.find(app, env);

    if (appEnv == null) {
      throw new UserFriendlyException("AppEnv not found for app " + app + " and env " + env);
    }

    String requestServiceUsername = appEnv.getRequestServiceUsername();

    if (!username.equals(requestServiceUsername) && !context.isCallerInRole("adm-admin")) {
      throw new UserFriendlyException(
          "User " + username + " is not authorized to deploy app " + app + " to env " + env);
    }

    // Ensure version string is semver, and therefore also unlikely a shell command
    validateSemver(ver);

    RemoteCommandResult result = new RemoteCommandResult(appEnv);

    BigInteger remoteCommandResultId = remoteCommandResultFacade.createReturnId(result);

    sshFacade.asyncExecuteRemoteCommand(result, ver);

    return remoteCommandResultId;
  }

  private void validateSemver(String ver) throws UserFriendlyException {
    final String regex =
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";
    final Pattern p = Pattern.compile(regex);

    if (!p.matcher(ver).matches()) {
      throw new UserFriendlyException("Version string must be semver formatted");
    }
  }
}
