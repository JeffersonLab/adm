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
import org.jlab.adm.persistence.entity.DeployJob;

@Stateless
public class DeployJobFacade extends AbstractFacade<DeployJob> {
  @PersistenceContext(unitName = "webappPU")
  protected EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public DeployJobFacade() {
    super(DeployJob.class);
  }

  private List<Predicate> getFilters(
      CriteriaBuilder cb, Root<DeployJob> root, BigInteger jobId, String appName) {
    List<Predicate> filters = new ArrayList<>();

    if (jobId != null) {
      filters.add(cb.equal(root.get("deployJobId"), jobId));
    }

    if (appName != null && !appName.isEmpty()) {
      filters.add(
          cb.like(cb.lower(root.get("appEnv").get("app").get("name")), appName.toLowerCase()));
    }

    return filters;
  }

  @PermitAll
  public List<DeployJob> filterList(BigInteger jobId, String appName, int offset, int max) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<DeployJob> cq = cb.createQuery(DeployJob.class);
    Root<DeployJob> root = cq.from(DeployJob.class);
    cq.select(root);

    List<Predicate> filters = getFilters(cb, root, jobId, appName);

    if (!filters.isEmpty()) {
      cq.where(cb.and(filters.toArray(new Predicate[] {})));
    }

    List<Order> orders = new ArrayList<>();
    Path p0 = root.get("deployJobId");
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
  public long countList(BigInteger jobId, String appName) {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<DeployJob> root = cq.from(DeployJob.class);

    List<Predicate> filters = getFilters(cb, root, jobId, appName);

    if (!filters.isEmpty()) {
      cq.where(cb.and(filters.toArray(new Predicate[] {})));
    }

    cq.select(cb.count(root));
    TypedQuery<Long> q = getEntityManager().createQuery(cq);
    return q.getSingleResult();
  }

  @PermitAll
  public BigInteger createReturnId(DeployJob result) {
    create(result);
    em.flush();
    return result.getDeployJobId();
  }
}
