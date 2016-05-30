package io.github.mincongh.batch;

import java.io.Serializable;

import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import io.github.mincongh.entity.Address;

/**
 * Batch item processor loads entities using entity IDs, provided by batch item
 * reader. Please notice that the process runs under multiple partitions, which
 * means the input IDs are provided by the item reader in the same partition. 
 * 
 * @author Mincong HUANG
 */
@Named
public class BatchItemProcessor implements ItemProcessor {
    
    @PersistenceContext(unitName = "us-address")
    private EntityManager em;
    
    /**
     * Process an input item into an output item. Here, the input item is an 
     * array of IDs and the output item is a list of entities mapped to these 
     * IDs. During the process, an injected entity manager is used to find out 
     * the entities in the database.
     * 
     * @param item the input item, an array of IDs
     * @return a list of entities mapped to the input IDs
     * @throws Exception thrown for any errors.
     */
    @Override
    public Object processItem(Object item) throws Exception {
        
        int[] ids = toIntArray((Serializable[]) item);
        
        CriteriaQuery<Address> q = em.getCriteriaBuilder().createQuery(Address.class);
        Root<Address> _address = q.from(Address.class);
        q.where(_address.get("addressId").in(ids));
        
        return em.createQuery(q).getResultList();
    }
    
    /**
     * Cast the serializable array into primitive integer array.
     * 
     * @param s serializable array
     * @return the primitive integer array
     */
    private int[] toIntArray(Serializable[] s){
        int[] array = new int[s.length];
        for(int i = 0; i < s.length; i++) {
            array[i] = (int) s[i];
        }
        return array;
      }
}
