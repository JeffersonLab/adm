package org.jlab.adm.business.session;

import org.jlab.adm.persistence.entity.AppEnv;
import org.jlab.adm.persistence.model.RemoteCommandResult;
import org.jlab.smoothness.business.exception.UserFriendlyException;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import java.io.IOException;

@Stateless
public class DeployerFacade {

    @Resource
    SessionContext context;

    @EJB
    SSHFacade sshFacade;

    @EJB
    AppEnvFacade appEnvFacade;

    @PermitAll
    public RemoteCommandResult deploy(String env, String app, String ver) throws UserFriendlyException, IOException {

        String username = context.getCallerPrincipal().getName();

        if(username == null || username.isEmpty() || username.equalsIgnoreCase("ANONYMOUS")) {
            throw new UserFriendlyException("You must authenticate before issuing a deploy command");
        }

        AppEnv appEnv = appEnvFacade.find(app, env);

        if(appEnv == null) {
            throw new UserFriendlyException("AppEnv not found for app " + app + " and env " + env);
        }

        String authorizedGroupname = appEnv.getAuthorizedGroupname();
        String serviceUsername = appEnv.getServiceUsername();
        String hostname = appEnv.getHostname();
        int port = appEnv.getPort();
        String command = appEnv.getDeployCommand();

        if(!context.isCallerInRole(authorizedGroupname) && !context.isCallerInRole("adm-admin")) {
            throw new UserFriendlyException("User " + username + " is not authorized to deploy app " + app + " to env " + env);
        }

        RemoteCommandResult result = sshFacade.executeRemoteCommand(serviceUsername, hostname, port, command);

        return result;
    }
}