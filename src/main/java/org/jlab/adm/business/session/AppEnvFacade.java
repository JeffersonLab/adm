package org.jlab.adm.business.session;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.jlab.adm.persistence.entity.App;
import org.jlab.adm.persistence.entity.AppEnv;
import org.jlab.smoothness.business.exception.UserFriendlyException;

@Stateless
public class AppEnvFacade extends AbstractFacade<AppEnv> {
  @EJB AppFacade appFacade;

  @PersistenceContext(unitName = "webappPU")
  protected EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public AppEnvFacade() {
    super(AppEnv.class);
  }

  @PermitAll
  public AppEnv find(String app, String env) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<AppEnv> cq = cb.createQuery(AppEnv.class);
    Root<AppEnv> root = cq.from(AppEnv.class);
    cq.select(root);

    List<Predicate> filters = new ArrayList<>();

    filters.add(cb.equal(root.get("app").get("name"), app));
    filters.add(cb.equal(root.get("name"), env));

    cq.where(cb.and(filters.toArray(new Predicate[] {})));

    List<AppEnv> entityList = getEntityManager().createQuery(cq).getResultList();

    AppEnv result = null;

    if (entityList != null && !entityList.isEmpty()) {
      result = entityList.get(0);

      // unique SQL constraint should enforce this
      assert entityList.size() == 1;
    }

    return result;
  }

  @RolesAllowed("adm-admin")
  public void removeAppEnv(BigInteger appEnvId) throws UserFriendlyException {
    if (appEnvId == null) {
      throw new UserFriendlyException("appEnvId cannot be empty");
    }

    AppEnv appEnv = find(appEnvId);

    if (appEnv == null) {
      throw new UserFriendlyException("AppEnv not found with id: " + appEnvId);
    }

    remove(appEnv);
  }

  @RolesAllowed("adm-admin")
  public void addAppEnv(
      String appName,
      String envName,
      String requestUsername,
      String deployUsername,
      String deployHostname,
      Integer deployPort,
      String deployCommand)
      throws UserFriendlyException {
    if (appName == null || appName.isEmpty()) {
      throw new UserFriendlyException("App Name cannot be empty");
    }

    if (envName == null || envName.isEmpty()) {
      throw new UserFriendlyException("Env Name cannot be empty");
    }

    if (requestUsername == null || requestUsername.isEmpty()) {
      throw new UserFriendlyException("Request Username cannot be empty");
    }

    if (deployUsername == null || deployUsername.isEmpty()) {
      throw new UserFriendlyException("Deploy Username cannot be empty");
    }

    if (deployHostname == null || deployHostname.isEmpty()) {
      throw new UserFriendlyException("Deploy Hostname cannot be empty");
    }

    if (deployPort == null || deployPort < 0) {
      throw new UserFriendlyException("Deploy Port must be positive integer");
    }

    if (deployCommand == null || deployCommand.isEmpty()) {
      throw new UserFriendlyException("Deploy Command cannot be empty");
    }

    App app = appFacade.findByName(appName);

    if (app == null) {
      throw new UserFriendlyException("App not found with name: " + appName);
    }

    AppEnv env =
        new AppEnv(
            app,
            envName,
            requestUsername,
            deployUsername,
            deployHostname,
            deployPort,
            deployCommand);

    create(env);
  }

  @RolesAllowed("adm-admin")
  public void editAppEnv(
      BigInteger appEnvId,
      String appName,
      String envName,
      String requestUsername,
      String deployUsername,
      String deployHostname,
      Integer deployPort,
      String deployCommand)
      throws UserFriendlyException {
    if (appName == null || appName.isEmpty()) {
      throw new UserFriendlyException("App Name cannot be empty");
    }

    if (envName == null || envName.isEmpty()) {
      throw new UserFriendlyException("Env Name cannot be empty");
    }

    if (requestUsername == null || requestUsername.isEmpty()) {
      throw new UserFriendlyException("Request Username cannot be empty");
    }

    if (deployUsername == null || deployUsername.isEmpty()) {
      throw new UserFriendlyException("Deploy Username cannot be empty");
    }

    if (deployHostname == null || deployHostname.isEmpty()) {
      throw new UserFriendlyException("Deploy Hostname cannot be empty");
    }

    if (deployPort == null || deployPort < 0) {
      throw new UserFriendlyException("Deploy Port must be positive integer");
    }

    if (deployCommand == null || deployCommand.isEmpty()) {
      throw new UserFriendlyException("Deploy Command cannot be empty");
    }

    App app = appFacade.findByName(appName);

    if (app == null) {
      throw new UserFriendlyException("App not found with name: " + appName);
    }

    AppEnv env = find(appEnvId);

    if (env == null) {
      throw new UserFriendlyException("Env not found with ID: " + appEnvId);
    }

    env.setName(envName);
    env.setApp(app);
    env.setRequestServiceUsername(requestUsername);
    env.setRunServiceUsername(deployUsername);
    env.setHostname(deployHostname);
    env.setPort(deployPort);
    env.setDeployCommand(deployCommand);

    edit(env);
  }

  @PermitAll
  public List<AppEnv> filterList(String envName, String appName, int offset, int max) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<AppEnv> cq = cb.createQuery(AppEnv.class);
    Root<AppEnv> root = cq.from(AppEnv.class);
    cq.select(root);

    List<Predicate> filters = getFilters(cb, cq, root, envName, appName);

    if (!filters.isEmpty()) {
      cq.where(cb.and(filters.toArray(new Predicate[] {})));
    }

    List<Order> orders = new ArrayList<>();
    Path<String> p0 = root.get("name");
    Order o0 = cb.asc(p0);
    orders.add(o0);
    cq.orderBy(orders);
    return getEntityManager()
        .createQuery(cq)
        .setFirstResult(offset)
        .setMaxResults(max)
        .getResultList();
  }

  private List<Predicate> getFilters(
      CriteriaBuilder cb,
      CriteriaQuery<? extends Object> cq,
      Root<AppEnv> root,
      String envName,
      String appName) {
    List<Predicate> filters = new ArrayList<>();

    if (envName != null && !envName.isEmpty()) {
      envName = envName.replaceAll("\\*", "%");
      filters.add(cb.like(cb.lower(root.get("name")), appName.toLowerCase()));
    }

    if (appName != null && !appName.isEmpty()) {
      appName = appName.replaceAll("\\*", "%");
      filters.add(cb.like(cb.lower(root.get("app").get("name")), appName.toLowerCase()));
    }

    return filters;
  }

  @PermitAll
  public long countList(String envName, String appName) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<AppEnv> root = cq.from(AppEnv.class);

    List<Predicate> filters = getFilters(cb, cq, root, envName, appName);

    if (!filters.isEmpty()) {
      cq.where(cb.and(filters.toArray(new Predicate[] {})));
    }

    cq.select(cb.count(root));
    TypedQuery<Long> q = getEntityManager().createQuery(cq);
    return q.getSingleResult();
  }
}
