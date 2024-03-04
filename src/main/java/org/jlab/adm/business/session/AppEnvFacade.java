package org.jlab.adm.business.session;

import org.jlab.adm.org.jlab.adm.persistence.entity.AppEnv;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
}
