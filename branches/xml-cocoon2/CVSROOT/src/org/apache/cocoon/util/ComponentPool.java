/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.cocoon.util;

import org.apache.avalon.Poolable;
import org.apache.avalon.ThreadSafe;
import org.apache.avalon.util.pool.ThreadSafePool;
import org.apache.avalon.util.pool.ObjectFactory;
import org.apache.avalon.util.pool.PoolController;
import org.apache.avalon.util.pool.Resizable;
import org.apache.cocoon.ComponentFactory;

/**
 * This is a implementation of <code>Pool</code> for SitemapComponents
 * that is thread safe.
 *
 * @author <a href="mailto:Giacomo.Pati@pwr.ch">Giacomo Pati</a>
 */
public class ComponentPool extends ThreadSafePool implements Resizable {

    public final static int DEFAULT_POOL_SIZE = 16;

    public ComponentPool(final ObjectFactory factory) throws Exception {
        super(factory, DEFAULT_POOL_SIZE/2, DEFAULT_POOL_SIZE);
    }

    public ComponentPool(final ObjectFactory factory,
                         final int initial) throws Exception {
        super(factory, initial, initial);
    }

    public ComponentPool(final ObjectFactory factory,
                         final int initial,
                         final int maximum) throws Exception {
        super(factory, initial, maximum);
    }

    public synchronized void grow(int amount) {
        if (m_currentCount >= m_max) {
            m_max += amount;
        } else {
            m_max = Math.max(m_currentCount + amount, m_max);
        }

        while (m_currentCount < m_max) {
            try {
                 m_ready.add( m_factory.newInstance() );
                 m_currentCount++;
            } catch (Exception e) {
                getLogger().debug("Error growing the pool", e);
            }
        }

        notify();
    }

    public synchronized void shrink(int amount) {
        m_max -= amount;

        while (m_currentCount > m_max) {
            m_ready.remove(0);
        }

        notify();
    }
}
