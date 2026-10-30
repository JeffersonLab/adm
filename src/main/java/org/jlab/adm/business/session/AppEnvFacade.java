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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

  /**
   * Adds an app env, to an existing app or, with newApp, to a new app made in the same transaction:
   * if the env can't be added, the new app isn't either.
   *
   * @param newApp true to create the app, which must not exist yet; false to use an existing app
   * @param docUrl the new app's documentation URL, or null; ignored unless newApp
   */
  @RolesAllowed("adm-admin")
  public void addAppEnv(
      String appName,
      boolean newApp,
      String docUrl,
      String envName,
      String requestUsername,
      String deployUsername,
      String deployHostname,
      Integer deployPort,
      String deployCommand)
      throws UserFriendlyException {
    validate(
        appName,
        envName,
        requestUsername,
        deployUsername,
        deployHostname,
        deployPort,
        deployCommand);

    App app;

    if (newApp) {
      app = appFacade.addApp(appName, docUrl);
    } else {
      app = findApp(appName);

      if (find(appName, envName) != null) {
        throw new UserFriendlyException("App " + appName + " already has an env named " + envName);
      }
    }

    AppEnv env =
        new AppEnv(
            app,
            envName,
            requestUsername,
            deployUsername,
            joinHostnames(deployHostname),
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
    validate(
        appName,
        envName,
        requestUsername,
        deployUsername,
        deployHostname,
        deployPort,
        deployCommand);

    App app = findApp(appName);

    AppEnv env = find(appEnvId);

    if (env == null) {
      throw new UserFriendlyException("Env not found with ID: " + appEnvId);
    }

    AppEnv existing = find(appName, envName);

    if (existing != null && !existing.getAppEnvId().equals(appEnvId)) {
      throw new UserFriendlyException("App " + appName + " already has an env named " + envName);
    }

    env.setName(envName);
    env.setApp(app);
    env.setRequestServiceUsername(requestUsername);
    env.setRunServiceUsername(deployUsername);
    env.setHostname(joinHostnames(deployHostname));
    env.setPort(deployPort);
    env.setDeployCommand(deployCommand);

    edit(env);
  }

  /** Refuses a record with a blank field, or a port SSH can't use, whether added or edited. */
  static void validate(
      String appName,
      String envName,
      String requestUsername,
      String deployUsername,
      String deployHostname,
      Integer deployPort,
      String deployCommand)
      throws UserFriendlyException {
    requireNonBlank("App Name", appName);
    requireNonBlank("Env Name", envName);
    requireNonBlank("Request Username", requestUsername);
    requireNonBlank("Deploy Username", deployUsername);
    requireNonBlank("Deploy Hostname", deployHostname);
    validateHostnames(deployHostname);

    if (deployPort == null || deployPort < 1 || deployPort > 65535) {
      throw new UserFriendlyException(
          "Deploy Port must be a whole number from 1 to 65535 (SSH usually uses 22)");
    }

    requireNonBlank("Deploy Command", deployCommand);
  }

  /** Refuses a host listed twice, which would deploy to it twice, or a list too long to save. */
  private static void validateHostnames(String deployHostname) throws UserFriendlyException {
    List<String> hosts = AppEnv.parseHostnames(deployHostname);

    // Such as only commas
    if (hosts.isEmpty()) {
      throw new UserFriendlyException("Deploy Hostname cannot be empty");
    }

    Set<String> seen = new HashSet<>();

    for (String host : hosts) {
      // Host names are case-insensitive
      if (!seen.add(host.toLowerCase(Locale.ROOT))) {
        throw new UserFriendlyException("Deploy Hostname lists " + host + " more than once");
      }
    }

    if (joinHostnames(deployHostname).length() > AppEnv.MAX_HOSTNAME_LENGTH) {
      throw new UserFriendlyException(
          "Deploy Hostname cannot be longer than " + AppEnv.MAX_HOSTNAME_LENGTH + " characters");
    }
  }

  /** Saves a list of hosts in one form, whichever separators were typed: "host1, host2" */
  static String joinHostnames(String deployHostname) {
    return String.join(", ", AppEnv.parseHostnames(deployHostname));
  }

  private static void requireNonBlank(String field, String value) throws UserFriendlyException {
    if (value == null || value.isBlank()) {
      throw new UserFriendlyException(field + " cannot be empty");
    }
  }

  /** Finds an existing app by its exact name, so a typo can't select or create another app. */
  private App findApp(String appName) throws UserFriendlyException {
    App app = appFacade.findByName(appName);

    if (app == null) {
      App similar = appFacade.findByNameIgnoreCase(appName);

      if (similar != null) {
        throw new UserFriendlyException(
            "App not found with name: " + appName + " (did you mean " + similar.getName() + "?)");
      }

      throw new UserFriendlyException(
          "App not found with name: "
              + appName
              + " (choose an existing app, or Create new App to add it)");
    }

    return app;
  }

  @PermitAll
  public List<AppEnv> filterList(
      String appName, String envName, String hostname, int offset, int max) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<AppEnv> cq = cb.createQuery(AppEnv.class);
    Root<AppEnv> root = cq.from(AppEnv.class);
    cq.select(root);

    List<Predicate> filters = getFilters(cb, cq, root, appName, envName, hostname);

    if (!filters.isEmpty()) {
      cq.where(cb.and(filters.toArray(new Predicate[] {})));
    }

    List<Order> orders = new ArrayList<>();
    Path<String> p0 = root.get("app");
    Path<String> p1 = root.get("name");
    Order o0 = cb.asc(p0);
    orders.add(o0);
    Order o1 = cb.asc(p1);
    orders.add(o1);
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
      String appName,
      String envName,
      String hostname) {
    List<Predicate> filters = new ArrayList<>();

    if (appName != null && !appName.isEmpty()) {
      appName = appName.replaceAll("\\*", "%");
      filters.add(cb.like(cb.lower(root.get("app").get("name")), appName.toLowerCase()));
    }

    if (envName != null && !envName.isEmpty()) {
      envName = envName.replaceAll("\\*", "%");
      filters.add(cb.like(cb.lower(root.get("name")), envName.toLowerCase()));
    }

    if (hostname != null && !hostname.isEmpty()) {
      hostname = hostname.replaceAll("\\*", "%").toLowerCase();
      Expression<String> hosts = cb.lower(root.get("hostname"));
      // Any of the env's hosts, which are saved as "host1, host2"
      filters.add(
          cb.or(
              cb.like(hosts, hostname),
              cb.like(hosts, hostname + ", %"),
              cb.like(hosts, "%, " + hostname),
              cb.like(hosts, "%, " + hostname + ", %")));
    }

    return filters;
  }

  @PermitAll
  public long countList(String appName, String envName, String hostname) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<AppEnv> root = cq.from(AppEnv.class);

    List<Predicate> filters = getFilters(cb, cq, root, appName, envName, hostname);

    if (!filters.isEmpty()) {
      cq.where(cb.and(filters.toArray(new Predicate[] {})));
    }

    cq.select(cb.count(root));
    TypedQuery<Long> q = getEntityManager().createQuery(cq);
    return q.getSingleResult();
  }
}
