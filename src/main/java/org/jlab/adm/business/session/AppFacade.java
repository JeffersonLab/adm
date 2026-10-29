package org.jlab.adm.business.session;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.jlab.adm.persistence.entity.App;
import org.jlab.smoothness.business.exception.UserFriendlyException;

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
  public App addApp(String name, String docUrl) throws UserFriendlyException {
    if (name == null || name.isEmpty()) {
      throw new UserFriendlyException("Name cannot be empty");
    }

    // Names differing only in case would look like the same app in lists and deploy requests
    App existing = findByNameIgnoreCase(name);

    if (existing != null) {
      throw new UserFriendlyException("App already exists with name: " + existing.getName());
    }

    App app = new App(name, docUrl);

    create(app);

    return app;
  }

  @RolesAllowed({"adm-admin"})
  public void editApp(BigInteger appId, String name, String docUrl) throws UserFriendlyException {

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

  @PermitAll
  public List<App> filterList(String appName, int offset, int max) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<App> cq = cb.createQuery(App.class);
    Root<App> root = cq.from(App.class);
    cq.select(root);

    List<Predicate> filters = getFilters(cb, cq, root, appName);

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
      CriteriaBuilder cb, CriteriaQuery<? extends Object> cq, Root<App> root, String appName) {
    List<Predicate> filters = new ArrayList<>();

    if (appName != null && !appName.isEmpty()) {
      appName = appName.replaceAll("\\*", "%");
      filters.add(cb.like(cb.lower(root.get("name")), appName.toLowerCase()));
    }

    return filters;
  }

  @PermitAll
  public long countList(String appName) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<App> root = cq.from(App.class);

    List<Predicate> filters = getFilters(cb, cq, root, appName);

    if (!filters.isEmpty()) {
      cq.where(cb.and(filters.toArray(new Predicate[] {})));
    }

    cq.select(cb.count(root));
    TypedQuery<Long> q = getEntityManager().createQuery(cq);
    return q.getSingleResult();
  }

  /** Finds the app with exactly this name; unlike filterList, no wildcards or case folding. */
  @PermitAll
  public App findByName(String appName) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<App> cq = cb.createQuery(App.class);
    Root<App> root = cq.from(App.class);
    cq.select(root);
    cq.where(cb.equal(root.get("name"), appName));

    List<App> appList = getEntityManager().createQuery(cq).getResultList();

    return appList.isEmpty() ? null : appList.get(0);
  }

  @PermitAll
  public App findByNameIgnoreCase(String appName) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<App> cq = cb.createQuery(App.class);
    Root<App> root = cq.from(App.class);
    cq.select(root);
    cq.where(cb.equal(cb.lower(root.get("name")), appName.toLowerCase()));

    List<App> appList = getEntityManager().createQuery(cq).setMaxResults(1).getResultList();

    return appList.isEmpty() ? null : appList.get(0);
  }

  /** Names of the apps that contain the term, ignoring case, for autocomplete. */
  @PermitAll
  public List<String> searchNames(String term, int max) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<String> cq = cb.createQuery(String.class);
    Root<App> root = cq.from(App.class);
    Path<String> name = root.get("name");
    cq.select(name);

    if (term != null && !term.isEmpty()) {
      String pattern =
          "%"
              + term.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
              + "%";
      cq.where(cb.like(cb.lower(name), pattern, '\\'));
    }

    cq.orderBy(cb.asc(name));

    return getEntityManager().createQuery(cq).setMaxResults(max).getResultList();
  }
}
