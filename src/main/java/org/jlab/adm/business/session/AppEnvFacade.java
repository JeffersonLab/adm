package org.jlab.adm.business.session;

import org.jlab.adm.persistence.entity.AppEnv;

import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class AppEnvFacade extends AbstractFacade<AppEnv> {
    @PersistenceContext(unitName = "admPU")
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

        cq.where(cb.and(filters.toArray(new Predicate[]{})));

        List<AppEnv> entityList = getEntityManager().createQuery(cq).getResultList();

        AppEnv result = null;

        if(entityList != null && !entityList.isEmpty()) {
            result = entityList.get(0);

            // unique SQL constraint should enforce this
            assert entityList.size() == 1;
        }

        return result;
    }
}
