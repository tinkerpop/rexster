package com.tinkerpop.rexster.util;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationComparator;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.util.Iterator;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class HierarchicalConfigurationComparator implements ConfigurationComparator {
    @Override
    public boolean compare(Configuration h1, Configuration h2) {
        Iterator itty = h2.getKeys();
        while(itty.hasNext()) {
            final String nextKey = (String) itty.next();
            if (!h1.containsKey(nextKey)) {
                return false;
            }
        }

        // this pass is a reverse of the above which tries to fail fast by checking for new keys in "that". this
        // pass looks for keys which exist in "this" but not "that" but also checks the values of those fields.
        itty = h1.getKeys();
        while(itty.hasNext()) {
            final String nextKey = (String) itty.next();
            if (!h2.containsKey(nextKey)) {
                return false;
            }

            final Object val2 = h2.getProperty(nextKey);
            final Object val1 = h1.getProperty(nextKey);
            if (val1 instanceof HierarchicalConfiguration && val2 instanceof HierarchicalConfiguration) {
                if (!compare((HierarchicalConfiguration) val1, (HierarchicalConfiguration) val2)) return false;
            } else {
                if (!h1.getProperty(nextKey).equals(h2.getProperty(nextKey))) return false;
            }
        }

        return true;
    }
}
