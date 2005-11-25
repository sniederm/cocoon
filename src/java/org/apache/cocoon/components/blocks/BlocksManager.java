/*
 * Copyright 1999-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.components.blocks;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.components.LifecycleHelper;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.environment.internal.EnvironmentHelper;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceResolver;

/**
 * @version $Id$
 */
public class BlocksManager
    extends AbstractLogEnabled
    implements Configurable, Contextualizable, Disposable, Initializable, Serviceable, ThreadSafe { 

    public static String ROLE = BlocksManager.class.getName();
    private ServiceManager serviceManager;
    private Context context;

    private String wiringFile;
    private HashMap blocks = new HashMap();
    private TreeMap mountedBlocks = new TreeMap(new InverseLexicographicalOrder());

    public void service(ServiceManager manager) throws ServiceException {
        this.serviceManager = manager;
    }

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public void configure(Configuration config)
    throws ConfigurationException {
        this.wiringFile = config.getAttribute("file");
    }

    public void initialize() throws Exception {
        getLogger().debug("Initializing the Blocks Manager");

        SourceResolver resolver = null;
        Source source = null;
        Configuration wiring = null;

        // Read the wiring file
        try {
            resolver = (SourceResolver) this.serviceManager.lookup(SourceResolver.ROLE);
            source = resolver.resolveURI(this.wiringFile);
            DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
            wiring = builder.build(source.getInputStream(), source.getURI());
        } finally {
            if (resolver != null) {
                resolver.release(source);
            }
            this.serviceManager.release(resolver);
        }

        Configuration[] blockConfs = wiring.getChildren("block");

        // Create and store all blocks
        for (int i = 0; i < blockConfs.length; i++) {
            Configuration blockConf = blockConfs[i];
            getLogger().debug("Creating " + blockConf.getName() +
                              " id=" + blockConf.getAttribute("id") +
                              " location=" + blockConf.getAttribute("location"));
            BlockManager blockManager = new BlockManager();
            blockManager.setBlocksManager(this);
            LifecycleHelper.setupComponent(blockManager,
                                           this.getLogger(),
                                           this.context,
                                           this.serviceManager,
                                           blockConf);
            this.blocks.put(blockConf.getAttribute("id"), blockManager);
            String mountPath = blockConf.getChild("mount").getAttribute("path", null);
            if (mountPath != null) {
                this.mountedBlocks.put(mountPath, blockManager);
                getLogger().debug("Mounted block " + blockConf.getAttribute("id") +
                                  " at " + mountPath);
            }
        }
    }

    public void dispose() {
        Iterator blocksIter = this.blocks.entrySet().iterator();
        while (blocksIter.hasNext()) {
            LifecycleHelper.dispose(blocksIter.next());
        }
        this.blocks = null;
        this.mountedBlocks = null;
        this.serviceManager = null;
    }

    /* 
       The BlocksManager could be merged with the Cocoon object and be
       responsible for all processing. In that case it should
       implement Processor, at the moment it is called from a protocol
       and delagates to a BlockManager, so there is no point in
       implementing the whole Processor interface.

       The largest mount point that is a prefix of the URI is
       chosen. The implementation could be made much more efficient.
    */
    public boolean process(Environment environment) throws Exception {
        String uri = environment.getURI();
        String oldPrefix = environment.getURIPrefix();
        String oldURI = uri;
        // The mount points start with '/' make sure that the URI also
        // does, so that they are compareable.
        if (uri.length() == 0 || uri.charAt(0) != '/') {
            uri = "/" + uri;
        }
        // All mount points that are before or equal to the URI in
        // lexicographical order. This includes all prefixes.
        Map possiblePrefixes = mountedBlocks.tailMap(uri);
        Iterator possiblePrefixesIt = possiblePrefixes.entrySet().iterator();
        BlockManager block = null;
        String mountPoint = null;
        // Find the largest prefix to the uri
        while (possiblePrefixesIt.hasNext()) {
            Map.Entry entry = (Map.Entry) possiblePrefixesIt.next();
            mountPoint = (String)entry.getKey();
            if (uri.startsWith(mountPoint)) {
                block = (BlockManager)entry.getValue();
                break;
            }
        }
        if (block == null) {
            return false;
        } else {
            // Resolve the URI relative to the mount point
            uri = uri.substring(mountPoint.length());
            getLogger().debug("Enter processing in block at " + mountPoint);
            try {
                environment.setURI("", uri);
                // It is important to set the current block each time
                // a new block is entered, this is used for the block
                // protocol
                EnvironmentHelper.enterProcessor(block, null, environment);
                return block.process(environment);
            } finally {
                EnvironmentHelper.leaveProcessor();
                environment.setURI(oldPrefix, oldURI);
                getLogger().debug("Leaving processing in block at " + mountPoint);
            }
        }
    }

    public Block getBlock(String blockId) {
	return (Block)this.blocks.get(blockId);
    }

    private static class InverseLexicographicalOrder implements Comparator {
        public int compare(Object o1, Object o2) {
            return ((String)o2).compareTo((String)o1);
        }
    }
}
