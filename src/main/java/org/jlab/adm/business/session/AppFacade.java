package org.jlab.adm.business.session;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jlab.adm.persistence.entity.App;

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
}
