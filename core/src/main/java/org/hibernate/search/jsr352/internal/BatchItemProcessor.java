package org.hibernate.search.jsr352.internal;

import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Named;
import org.jboss.logging.Logger;

/**
 *
 * @author Mincong HUANG
 */
@Named
public class BatchItemProcessor implements ItemProcessor {

    private static final Logger logger = Logger.getLogger(BatchItemProcessor.class);

    @Override
    public Object processItem(Object item) throws Exception {
        logger.info("processing item ...");
        return item;
    }
}
