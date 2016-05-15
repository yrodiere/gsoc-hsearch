package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import javax.batch.api.chunk.ItemReader;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import io.github.mincongh.entity.Address;

/**
 * 
 * @author Mincong HUANG
 */
@Named
public class AddressReader implements ItemReader {

    // Entity manager is used for fetching entities from persistence context.
    @PersistenceContext(unitName = "us-address")
    private EntityManager em;

    // Cache for entities
    // TODO: Are these variables necessary ?
    private List<Address> items = null;
    private Iterator<Address> iterator;

    private final int MAX_RESULTS = 1000;

    @Override
    public Serializable checkpointInfo() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }

    /**
     * When open() is called, the address entities will be read from
     * persistence context. Then they will be stored temporarily in this class
     * as a list. TODO: maybe we should do it in a better way
     * 
     * @param checkpoint the checkpoint given by the batch runtime. It is used
     *         when the job execution is restarted.
     * @throws Exception when any exception is thrown during the open process
     */
    @Override
    @SuppressWarnings("unchecked")
    public void open(Serializable checkpoint) throws Exception {
        items = em.createQuery("SELECT a FROM Address a WHERE address_id <= 1000")
                .setMaxResults(MAX_RESULTS)
                .getResultList();
        iterator = items.iterator();
    }

    /**
     * Read items from the item list until the last one is reached.
     */
    @Override
    public Object readItem() throws Exception {
        return iterator.hasNext() ? iterator.next() : null;
    }
}
