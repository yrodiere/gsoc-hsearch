package io.github.mincongh.servlet;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

import io.github.mincongh.entity.Animal;

@WebServlet("/search")
public class AnimalSearchServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @PersistenceContext(unitName = "zoo-jpa")
    protected EntityManagerFactory emFactory;
    protected EntityManager entityManager;
    protected FullTextEntityManager fullTextEntityManager;
    
    private QueryBuilder queryBuilder;   
    private boolean isIndexed = false;
    
    public AnimalSearchServlet() {
        emFactory = Persistence.createEntityManagerFactory("zoo-jpa");
    }
    
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }
    
    /**
     * Find animals using hibernate search
     * 
     * @throws IOException 
     * @throws ServletException
     */
    protected void doPost(HttpServletRequest request, 
            HttpServletResponse response) throws ServletException, IOException {
        
        List<Animal> animals = null;
        entityManager = emFactory.createEntityManager();
        entityManager.getTransaction().begin();
        
        String searchQuery = request.getParameter("q");
        if (searchQuery != null) {
            System.out.println("Query string is " + searchQuery);
            animals = search(searchQuery);
            if (animals != null) {
                for (Animal a: animals) {
                    System.out.println(a);
                }
            } else {
                System.out.println("No result found.");
            }
        } else {
            System.out.println("Cannot get query string.");
        }
        
        entityManager.getTransaction().commit();
        entityManager.close();
        
        // place search result to request
        request.setAttribute("animals", animals);
        
        // Pass the request object to the JSP / JSTL view for rendering
        getServletContext().getRequestDispatcher("/WEB-INF/page/search.jsp")
                .forward(request, response);
    }
    
    @SuppressWarnings("unchecked")
    private List<Animal> search(String searchQuery) {
        
        fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        
        if (!isIndexed) {
            rebuildIndex(fullTextEntityManager);
            isIndexed = true;
        }
        
        queryBuilder = fullTextEntityManager
                .getSearchFactory()
                .buildQueryBuilder()
                .forEntity(Animal.class)
                .get();
        
        // Hibernate Search translates under the covers into a Lucene Search
//      org.apache.lucene.search.Query luceneQuery = keywordQuery(searchQuery);
//      org.apache.lucene.search.Query luceneQuery = fuzzyQuery(searchQuery);
        org.apache.lucene.search.Query luceneQuery = wildcardQuery(searchQuery);
        
        
        // Lucene search results can be translated into a standard
        // org.hibernate.Query object and use the same as any normal database
        // query
        javax.persistence.Query jpaQuery = fullTextEntityManager
                .createFullTextQuery(luceneQuery, Animal.class);
        
        return jpaQuery.getResultList();
    }
    
    /**
     * When introducing Hibernate Search in an existing application, you have
     * to create an initial Lucene indexfor data already present in your 
     * database. Here, an <code>EntityManager</code> is used to rebuild the
     * index, because this application use JPA 2.1.
     * 
     * @param fullTextEntityManager
     */
    private void rebuildIndex(FullTextEntityManager fullTextEntityManager) {
        try {
            fullTextEntityManager.createIndexer().startAndWait();
        } catch (InterruptedException e) {
            System.out.println("Index operation is interrupted.");
            e.printStackTrace();
        }
    }
    
    /**
     * The most basic form of search. As the name suggests, this query type
     * searches for one or more particular words.
     * 
     * @param searchString search string, e.g. "cat", "cat a"
     * @return
     */
    private Query keywordQuery(String searchString) {
        return queryBuilder
                .keyword()
                .onFields("name", "type")
                .matching(searchString)
                .createQuery();
    }
    
    /**
     * With a fuzzy search, keywords match against fields even when they are
     * off by one or more characters. Check 
     * <a href="https://en.wikipedia.org/wiki/Edit_distance">wikipedia</a>
     * to know more about the "Edit Distance".
     * 
     * @param searchString search string, e.g. "cag~"
     * @return
     */
    private Query fuzzyQuery(String searchString) {
        return queryBuilder
                .keyword()
                .fuzzy()
//              .withThreshold(0.7f)        // deprecated
//                                          // use withEditDistanceUpTo(int)
                .withEditDistanceUpTo(2)    // default 2 (can be 0, 1, 2)
                .onFields("name", "type")
                .matching(searchString)
                .createQuery();
    }
    
    /**
     * Lucene supports single and multiple character wildcard searches within
     * single terms (not within phrase queries)
     * <p>
     * <ul>
     *   <li>
     *     To perform a single character wildcard search, 
     *     user the "?" symbol
     *   </li>
     *   <li>
     *     To perform a multiple character wildcard search, 
     *     user the "*" symbol
     *   </li>
     * @param searchString search string, e.g. "c?t", "d*"
     * @return
     */
    private Query wildcardQuery(String searchString) {
        return queryBuilder
                .keyword()
                .wildcard()
                .onFields("name", "type")
                .matching(searchString)
                .createQuery();
    }
}
