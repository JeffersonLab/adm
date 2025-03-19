package org.jlab.adm.business.session;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
}
