package io.github.mincongh.service;

import io.github.mincongh.entity.Address;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Service for Address entity.
 * 
 * @author Mincong HUANG
 */
public class AddressService {

	private EntityManagerFactory emf;
	private EntityManager em;

	/**
	 * Constructor with parameter(s). Here, the entity manager factory is
	 * passed to service. Then, factory builds the entity manager for different
	 * entity-operations.
	 * 
	 * @param emf the entity manager factory
	 */
	public AddressService(EntityManagerFactory emf) {
		this.emf = emf;
	}

	/**
	 * Open the entity manager for different transactions. If the entity
	 * manager does not exist, then it will be created first.
	 *
	 * @return whether the entity manager is opened.
	 */
	public boolean open() {

		if (em == null) {
			em = emf.createEntityManager();
		}
		return em.isOpen();
	}

	/**
	 * Close the entity manager.
	 * 
	 * @return whether the entity manager is closed.
	 */
	public boolean close() {

		em.close();
		return em.isOpen();
	}

	/**
	 * Get a list of addresses. The fetched records are limited at 10 entities.
	 * Rollback if there's any exception.
	 *
	 * @return a list of addresses
	 */
	public List<Address> getAddresses() {

		List<Address> addresses = null;
		try {
			// Start a new trasaction
			em.getTransaction().begin();
			// Get list of addresses
			addresses = em.createQuery("SELECT a FROM Address a")
					.setMaxResults(10)
					.getResultList();
			// Transaction finished, commit
			em.getTransaction().commit();

		} catch (Exception e) {
			// print exception message then rollback
			e.printStackTrace();
			em.getTransaction().rollback();
		}
		return addresses;
	}
}
