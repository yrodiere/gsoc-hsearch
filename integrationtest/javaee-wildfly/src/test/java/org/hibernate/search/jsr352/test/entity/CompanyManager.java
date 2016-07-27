/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.test.entity;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;

/**
 * @author Mincong Huang
 */
@Stateless
public class CompanyManager {

	@PersistenceContext(name = "h2")
	private EntityManager em;

	public void persist(Company company) {
		em.persist( company );
	}

	public List<Company> findCompanyByName(String name) {
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		Query luceneQuery = ftem.getSearchFactory().buildQueryBuilder()
				.forEntity( Company.class ).get()
				.keyword().onField( "name" ).matching( name )
				.createQuery();
		@SuppressWarnings("unchecked")
		List<Company> result = ftem.createFullTextQuery( luceneQuery ).getResultList();
		return result;
	}

	public long rowCount() {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = builder.createQuery( Long.class );
		cq.select( builder.count( cq.from( Company.class ) ) );
		return em.createQuery( cq ).getSingleResult();
	}

	public EntityManager getEntityManager() {
		return em;
	}
}
