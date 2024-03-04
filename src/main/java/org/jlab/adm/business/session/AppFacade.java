package org.jlab.adm.business.session;

import org.jlab.adm.org.jlab.adm.persistence.entity.App;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class AppFacade extends AbstractFacade<App> {
    @PersistenceContext(unitName = "admPU")
    protected EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public AppFacade() {
        super(App.class);
    }
}
