package org.jlab.adm.business.session;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
