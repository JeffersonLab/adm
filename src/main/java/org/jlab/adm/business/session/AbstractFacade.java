package org.jlab.adm.business.session;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFacade<T> {
    private final Class<T> entityClass;

    public AbstractFacade(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @PermitAll
    public Class<T> getEntityClass() {
        return entityClass;
    }

    protected abstract EntityManager getEntityManager();

    @PermitAll
    public void create(T entity) {
        getEntityManager().persist(entity);
    }

    @PermitAll
    public T edit(T entity) {
        return getEntityManager().merge(entity);
    }

    @PermitAll
    public void remove(T entity) {
        getEntityManager().remove(getEntityManager().merge(entity));
    }

    @PermitAll
    public T find(Object id) {
        return getEntityManager().find(entityClass, id);
    }

    @PermitAll
    public List<T> findAll() {
        CriteriaQuery<T> cq = getEntityManager().getCriteriaBuilder().createQuery(entityClass);
        cq.select(cq.from(entityClass));
        return getEntityManager().createQuery(cq).getResultList();
    }

    @PermitAll
    public List<T> findAll(OrderDirective... directives) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityClass);
        Root<T> root = cq.from(entityClass);
        cq.select(root);
        List<Order> orders = new ArrayList<Order>();
        for (OrderDirective ob : directives) {
            Order o;

            Path p = root.get(ob.field);

            if (ob.asc) {
                o = cb.asc(p);
            } else {
                o = cb.desc(p);
            }

            orders.add(o);
        }
        cq.orderBy(orders);
        return getEntityManager().createQuery(cq).getResultList();
    }

    @PermitAll
    public List<T> findRange(int[] range) {
        CriteriaQuery<T> cq = getEntityManager().getCriteriaBuilder().createQuery(entityClass);
        cq.select(cq.from(entityClass));
        TypedQuery<T> q = getEntityManager().createQuery(cq);
        q.setMaxResults(range[1] - range[0]);
        q.setFirstResult(range[0]);
        return q.getResultList();
    }

    @PermitAll
    public long count() {
        CriteriaQuery<Long> cq = getEntityManager().getCriteriaBuilder().createQuery(Long.class);
        Root<T> rt = cq.from(entityClass);
        cq.select(getEntityManager().getCriteriaBuilder().count(rt));
        TypedQuery<Long> q = getEntityManager().createQuery(cq);
        return q.getSingleResult();
    }

    @PermitAll
    public static class OrderDirective {

        private final String field;
        private final boolean asc;

        public OrderDirective(String field) {
            this(field, true);
        }

        public OrderDirective(String field, boolean asc) {
            this.field = field;
            this.asc = asc;
        }

        public String getField() {
            return field;
        }

        public boolean isAsc() {
            return asc;
        }
    }
}
