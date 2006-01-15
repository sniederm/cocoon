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
package org.apache.cocoon.blocks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.logger.Logger;
import org.apache.cocoon.Modifiable;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.blocks.util.CoreUtil;
import org.apache.cocoon.blocks.util.LoggerUtil;
import org.apache.cocoon.blocks.util.ServletConfigurationWrapper;
import org.apache.cocoon.components.LifecycleHelper;
import org.apache.cocoon.components.source.SourceUtil;
import org.apache.cocoon.components.source.impl.DelayedRefreshSourceWrapper;
import org.apache.cocoon.core.Core;
import org.apache.cocoon.core.Settings;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.impl.URLSource;
import org.xml.sax.InputSource;

/**
 * @version $Id$
 */
public class BlocksManager
    extends
        HttpServlet
    implements
        Blocks,
        Modifiable { 

    public static String ROLE = BlocksManager.class.getName();
    private Context context;
    private BlocksContext blocksContext;

    private Source wiringFile;
    private HashMap blocks = new HashMap();
    private HashMap mountedBlocks = new HashMap();
    private Logger logger;    

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        this.blocksContext = new BlocksContext(this.getServletContext(), this);
        
        CoreUtil coreUtil = new CoreUtil(this.getServletConfig(), BlockConstants.WIRING);
        Core core = coreUtil.getCore();
        Settings settings = coreUtil.getSettings();
        this.context = core.getContext();
        LoggerUtil loggerUtil = new LoggerUtil(this.getServletConfig(), this.context, settings);
        this.logger = loggerUtil.getCocoonLogger();
        this.getLogger().debug("Initializing the Blocks Manager");
        
        InputSource is = null;
        try {
            this.getLogger().debug("Wiring file: " + this.getServletContext().getResource(BlockConstants.WIRING));
            URLSource urlSource = new URLSource();
            urlSource.init(this.getServletContext().getResource(BlockConstants.WIRING), null);
            this.wiringFile = new DelayedRefreshSourceWrapper(urlSource, 1000);
            is = SourceUtil.getInputSource(this.wiringFile);
        } catch (IOException e) {
            throw new ServletException("Could not open configuration file: " + BlockConstants.WIRING, e);
        } catch (ProcessingException e) {
            throw new ServletException("Could not open configuration file: " + BlockConstants.WIRING, e);                 
        }
        
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        Configuration wiring = null;
        try {
            wiring = builder.build(is);
        } catch (Exception e) {
            throw new ServletException("Could not create configuration from file: " + BlockConstants.WIRING, e);                  
        }
        
        ServletConfig blocksConfig =
            new ServletConfigurationWrapper(this.getServletConfig(), this.blocksContext);
        
        Configuration[] blockConfs = wiring.getChildren("block");
                
        try {
            // Create and store all blocks
            for (int i = 0; i < blockConfs.length; i++) {
                Configuration blockConf = blockConfs[i];
                this.getLogger().debug("Creating " + blockConf.getName() +
                                       " id=" + blockConf.getAttribute("id") +
                                       " location=" + blockConf.getAttribute("location"));
                BlockManager blockManager = new BlockManager();
                blockManager.init(blocksConfig);
                blockManager.setBlocks(this);
                LifecycleHelper.setupComponent(blockManager,
                                               this.getLogger(),
                                               this.context,
                                               null,
                                               blockConf);
                this.blocks.put(blockConf.getAttribute("id"), blockManager);
                String mountPath = blockConf.getChild("mount").getAttribute("path", null);
                if (mountPath != null) {
                    this.mountedBlocks.put(fixPath(mountPath), blockManager);
                    this.getLogger().debug("Mounted block " + blockConf.getAttribute("id") +
                                           " at " + mountPath);
                }
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
    
    public void destroy() {
        Iterator blocksIter = this.blocks.entrySet().iterator();
        while (blocksIter.hasNext()) {
            LifecycleHelper.dispose(blocksIter.next());
        }
        this.blocks = null;
        this.mountedBlocks = null;
        super.destroy();
    }
    
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String servletPath = trimPath(request.getServletPath());
        String pathInfo = trimPath(request.getPathInfo());        
        String uri = servletPath + pathInfo;

        if (uri.length() == 0) {
            /* empty relative URI
                 -> HTTP-redirect from /cocoon to /cocoon/ to avoid
                    StringIndexOutOfBoundsException when calling
                    "".charAt(0)
               else process URI normally
            */
            String prefix = request.getRequestURI();
            if (prefix == null) {
                prefix = "";
            }

            response.sendRedirect(response.encodeRedirectURL(prefix + "/"));
            return;
        }
        
        RequestDispatcher dispatcher = this.blocksContext.getRequestDispatcher(pathInfo);
        if (dispatcher == null)
            throw new ServletException("No block mounted at " + pathInfo);

        dispatcher.forward(request, response);
    }
    
    private Logger getLogger() {
        return this.logger;
    }

    // Blocks specific methods
    
    public Block getBlock(String blockId) {
        return (Block)this.blocks.get(blockId);
    }
    
    public Block getMountedBlock(String uri) {
        return (Block)this.mountedBlocks.get(uri);
    }
    
    // Modified interface
    
    /**
     * Queries the class to estimate its ergodic period termination.
     *
     * @param date a <code>long</code> value
     * @return a <code>boolean</code> value
     */
    public boolean modifiedSince(long date) {
        return date < this.wiringFile.getLastModified();
    }
    
    /**
     * Utility function to ensure that the parts of the request URI not is null
     * and not ends with /
     * @param path
     * @return the trimmed path
     */
    private static String trimPath(String path) {
        if (path == null)
                return "";
        int length = path.length();
        if (length > 0 && path.charAt(length - 1) == '/')
                path = path.substring(0, length - 1);
        return path;
    }
    
    /**
     * If a block is mounted on "/" it should be registred at "" to get the servlet
     * path right
     * @param path
     * @return fixed path
     */
    private static String fixPath(String path) {
        return "/".equals(path) ? "" : path;
    }
}
