package org.jlab.adm.business.session;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import org.jlab.adm.persistence.entity.RemoteCommandResult;

@Stateless
public class RemoteCommandResultFacade extends AbstractFacade<RemoteCommandResult> {
  @PersistenceContext(unitName = "admPU")
  protected EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public RemoteCommandResultFacade() {
    super(RemoteCommandResult.class);
  }

  private List<Predicate> getFilters(
      CriteriaBuilder cb, Root<RemoteCommandResult> root, String appName) {
    List<Predicate> filters = new ArrayList<>();

    if (appName != null && !appName.isEmpty()) {
      filters.add(
          cb.like(cb.lower(root.get("appEnv").get("app").get("name")), appName.toLowerCase()));
    }

    return filters;
  }

  @PermitAll
  public List<RemoteCommandResult> filterList(String appName, int offset, int max) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<RemoteCommandResult> cq = cb.createQuery(RemoteCommandResult.class);
    Root<RemoteCommandResult> root = cq.from(RemoteCommandResult.class);
    cq.select(root);

    List<Predicate> filters = getFilters(cb, root, appName);

    if (!filters.isEmpty()) {
      cq.where(cb.and(filters.toArray(new Predicate[] {})));
    }

    List<Order> orders = new ArrayList<>();
    Path p0 = root.get("remoteCommandResultId");
    Order o0 = cb.desc(p0);
    orders.add(o0);
    cq.orderBy(orders);
    return getEntityManager()
        .createQuery(cq)
        .setFirstResult(offset)
        .setMaxResults(max)
        .getResultList();
  }

  @PermitAll
  public long countList(String appName) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<RemoteCommandResult> root = cq.from(RemoteCommandResult.class);

    List<Predicate> filters = getFilters(cb, root, appName);

    if (!filters.isEmpty()) {
      cq.where(cb.and(filters.toArray(new Predicate[] {})));
    }

    cq.select(cb.count(root));
    TypedQuery<Long> q = getEntityManager().createQuery(cq);
    return q.getSingleResult();
  }

  @PermitAll
  public BigInteger createReturnId(RemoteCommandResult result) {
    create(result);
    em.flush();
    return result.getRemoteCommandResultId();
  }
}
