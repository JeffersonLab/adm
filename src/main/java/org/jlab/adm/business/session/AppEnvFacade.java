package org.jlab.adm.business.session;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import org.jlab.adm.persistence.entity.AppEnv;

@Stateless
public class AppEnvFacade extends AbstractFacade<AppEnv> {
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
}
