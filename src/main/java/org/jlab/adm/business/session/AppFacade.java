package org.jlab.adm.business.session;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jlab.adm.persistence.entity.App;
import org.jlab.smoothness.business.exception.UserFriendlyException;

import java.math.BigInteger;

@Stateless
public class AppFacade extends AbstractFacade<App> {
  @PersistenceContext(unitName = "webappPU")
  protected EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public AppFacade() {
    super(App.class);
  }

  @RolesAllowed("adm-admin")
  public void addApp(String name, String docUrl) throws UserFriendlyException {
    if (name == null || name.isEmpty()) {
      throw new UserFriendlyException("Name cannot be empty");
    }

    App app = new App(name, docUrl);

    create(app);
  }

  @RolesAllowed({"adm-admin"})
  public void editApp(
          BigInteger appId,
          String name,
          String docUrl)
          throws UserFriendlyException {

    if (appId == null) {
      throw new UserFriendlyException("appId cannot be empty");
    }

    App app = find(appId);

    if (app == null) {
      throw new UserFriendlyException("app not found with id: " + appId);
    }

    app.setName(name);
    app.setDocUrl(docUrl);

    edit(app);
  }

  @RolesAllowed("adm-admin")
  public void removeSoftware(BigInteger appId) throws UserFriendlyException {
    if (appId == null) {
      throw new UserFriendlyException("appId cannot be empty");
    }

    App app = find(appId);

    if (app == null) {
      throw new UserFriendlyException("App not found with id: " + appId);
    }

    remove(app);
  }
}
