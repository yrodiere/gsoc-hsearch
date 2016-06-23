package org.hibernate.search.jsr352.test.session;

import java.util.List;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.test.entity.Address;
import org.hibernate.search.jsr352.test.entity.Stock;
import org.hibernate.search.query.dsl.QueryBuilder;

/**
 * Search Session bean is used for searching target entities in the persistence
 * context using hibernate search.
 * 
 * @author Mincong HUANG
 */
@Stateful
public class SearchSession {
    
    public static final String KEYWORD = "keyword";
    public static final String FUZZY = "fuzzy";
    public static final String WILDCARD = "wildcard";
    
    // This particular EntityManager is injected as an EXTENDED persistence
    // context, which simply means that the EntityManager is created when the
    // @Stateful bean is created and destroyed when the @Stateful bean is
    // destroyed. Simply put, the data in the EntityManager is cached for the
    // lifetime of the @Stateful bean
    @PersistenceContext(unitName = "jsr352", type = PersistenceContextType.EXTENDED)
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
    public List<Stock> getStockes() {
        return entityManager
            .createNamedQuery("Stock.findAll")
            .setMaxResults(1000)
            .getResultList();
    }
    
    @SuppressWarnings("unchecked")
    public List<Address> search(String queryType, String searchString) {
        
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
        org.apache.lucene.search.Query luceneQuery = 
                buildQuery(queryBuilder, queryType, searchString);
        
        // then cast the lucene search query into JPA query
        javax.persistence.Query jpaQuery = fullTextEntityManager
                .createFullTextQuery(luceneQuery, Address.class)
                .setMaxResults(10);
        
        // get the results
        return jpaQuery.getResultList();
    }
    
    /**
     * Use different type of query.
     * 
     * @param builder query builder
     * @param queryType the type of query. For instant, "fuzzy", "keyword" 
     *        and "wildcard" queries are supported. Default value is "keyword"
     *        query.
     * @param searchString the search string given by the user.
     * @return the converted lucene query
     */
    private org.apache.lucene.search.Query buildQuery(QueryBuilder builder, 
            String queryType, String searchString) {
        org.apache.lucene.search.Query query;
        // prevent null case
        if (queryType == null) {
            queryType = "";
        }
        
        switch (queryType) {
            
            // With a fuzzy search, keywords match against fields even when
            // they are off by one or more characters. The number of off-char
            // is called "Edit Distance".
            case SearchSession.FUZZY :
                query = builder
                        .keyword()
                        .fuzzy()
                        .onFields("name", "type")
                        .matching(searchString)
                        .createQuery();
                break;
            
            // Lucene supports single and multiple character wildcard searches 
            // within single terms (not within phrase queries). 
            // To perform a single character wildcard search, use the "?" symbol
            // To perform a multiple character wildcard search, use the "*"
            // symbol
            case SearchSession.WILDCARD :
                query = builder
                        .keyword()
                        .wildcard()
                        .onFields("name", "type")
                        .matching(searchString)
                        .createQuery();
                break;
            
            // The most basic form of search. As the name suggests, this query
            // type searches for one or more particular words.
            case SearchSession.KEYWORD :
            default:
                query = builder
                        .keyword()
                        .onFields("name", "type")
                        .matching(searchString)
                        .createQuery();
                break;
        }
        return query;
    }
}
