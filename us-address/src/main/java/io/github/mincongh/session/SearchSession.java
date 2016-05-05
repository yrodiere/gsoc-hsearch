package io.github.mincongh.session;

import java.util.List;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

import io.github.mincongh.entity.Address;

/**
 * Search Session bean is used for searching target entities in the persistence
 * context using hibernate search.
 * 
 * @author Mincong HUANG
 */
@Stateful
public class SearchSession {
    
    // This particular EntityManager is injected as an EXTENDED persistence
    // context, which simply means that the EntityManager is created when the
    // @Stateful bean is created and destroyed when the @Stateful bean is
    // destroyed. Simply put, the data in the EntityManager is cached for the
    // lifetime of the @Stateful bean
    @PersistenceContext(unitName = "us-address", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;
    
    private FullTextEntityManager fullTextEntityManager;
    
    public SearchSession() {
    }

    @SuppressWarnings("unchecked")
    public List<Address> getAddresses() {
        return entityManager
            .createQuery("SELECT a FROM Address a WHERE a.type = :type")
            .setParameter("type", "Rd")
            .setMaxResults(1000)
            .getResultList();
    }
    
    @SuppressWarnings("unchecked")
    public List<Address> search(String searchString) {
        
        // get hiberante search full-text entity manager via 
        // the javax persistence entity manager
        fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        
        // construct a DSL query builder
        QueryBuilder queryBuilder = fullTextEntityManager
                .getSearchFactory()
                .buildQueryBuilder()
                .forEntity(Address.class)
                .get();
        
        // get a lucuene search query via query builder
        org.apache.lucene.search.Query luceneQuery = queryBuilder
                // keyword query
                .keyword()
                // this search will be applied to the following fields
                .onFields("name", "type")
                // put the searchString into the matching criteria
                .matching(searchString)
                // create
                .createQuery();
        
        // then cast the lucene search query into JPA query
        javax.persistence.Query jpaQuery = fullTextEntityManager
                .createFullTextQuery(luceneQuery, Address.class)
                .setMaxResults(10);
        
        // get the results
        return jpaQuery.getResultList();
    }
}
