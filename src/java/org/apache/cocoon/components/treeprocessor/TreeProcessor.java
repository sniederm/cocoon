/*
 * Copyright 1999-2004 The Apache Software Foundation.
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
package org.apache.cocoon.components.treeprocessor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.excalibur.component.RoleManageable;
import org.apache.avalon.excalibur.component.RoleManager;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.Recomposable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.SAXConfigurationHandler;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.Processor;
import org.apache.cocoon.components.ChainedConfiguration;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.components.ExtendedComponentSelector;
import org.apache.cocoon.components.LifecycleHelper;
import org.apache.cocoon.components.container.ComponentManagerWrapper;
import org.apache.cocoon.components.source.SourceUtil;
import org.apache.cocoon.components.source.impl.DelayedRefreshSourceWrapper;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.environment.ForwardRedirector;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.internal.EnvironmentHelper;
import org.apache.cocoon.environment.wrapper.EnvironmentWrapper;
import org.apache.cocoon.environment.wrapper.MutableEnvironmentFacade;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceResolver;

/**
 * Interpreted tree-traversal implementation of a pipeline assembly language.
 *
 * @author <a href="mailto:sylvain@apache.org">Sylvain Wallez</a>
 * @version CVS $Id: TreeProcessor.java,v 1.32 2004/05/26 01:31:06 joerg Exp $
 */

public class TreeProcessor
    extends AbstractLogEnabled
    implements ThreadSafe,
               Processor,
               Composable,
               Configurable,
               RoleManageable,
               Contextualizable,
               Disposable,
               Initializable {

    public static final String COCOON_REDIRECT_ATTR = "sitemap:cocoon-redirect";

    private static final String XCONF_URL =
        "resource://org/apache/cocoon/components/treeprocessor/treeprocessor-builtins.xml";

    /** The parent TreeProcessor, if any */
    protected TreeProcessor parent;

    /** The context */
    protected Context context;

    /** The component manager */
    protected ComponentManager manager;

    /** The role manager */
    protected RoleManager roleManager;

    /** The language used by this processor */
    protected String language;

    /** Selector of TreeBuilders, the hint is the language name */
    protected ExtendedComponentSelector builderSelector;

    /** The root node of the processing tree */
    protected ProcessingNode rootNode;

    /** The list of processing nodes that should be disposed when disposing this processor */
    protected List disposableNodes;

    /** Last modification time */
    protected long lastModified = 0;

    /** The source of the tree definition */
    protected DelayedRefreshSourceWrapper source;

    /** Delay for <code>sourceLastModified</code>. */
    protected long lastModifiedDelay;

    /** The current language configuration */
    protected Configuration currentLanguage;

    /** The file to process */
    protected String fileName;

    /** Check for reload? */
    protected boolean checkReload;

    /** The component configurations from the sitemap (if any) */
    protected Configuration componentConfigurations;
    
    /** The different sitemap component configurations */
    protected Map sitemapComponentConfigurations;
    
    /** The component manager for the sitemap */
    protected ComponentManager sitemapComponentManager;
    
    /** A service manager wrapper */
    protected ServiceManager serviceManager;
    
    /** The source resolver */
    protected SourceResolver resolver;
    
    /** The environment helper */
    private EnvironmentHelper environmentHelper;

    /**
     * Create a TreeProcessor.
     */
    public TreeProcessor() {
        // Language can be overriden in the configuration.
        this.language = "sitemap";

        this.checkReload = true;
        this.lastModifiedDelay = 1000;
    }

    /**
     * Create a child processor for a given language
     */
    protected TreeProcessor(TreeProcessor parent, 
                            DelayedRefreshSourceWrapper sitemapSource, 
                            boolean checkReload, 
                            String prefix) 
    throws Exception {
        this.parent = parent;
        this.language = (language == null) ? parent.language : language;

        // Copy all that can be copied from the parent
        this.enableLogging(parent.getLogger());
        this.context = parent.context;
        this.roleManager = parent.roleManager;
        this.source = sitemapSource;
        this.builderSelector = parent.builderSelector;
        this.checkReload = checkReload;
        this.lastModifiedDelay = parent.lastModifiedDelay;
        
        // We have our own CM
        this.manager = parent.sitemapComponentManager;
        this.resolver = (SourceResolver)this.manager.lookup(SourceResolver.ROLE);
        this.environmentHelper = new EnvironmentHelper(parent.environmentHelper);
        // Setup environment helper
        ContainerUtil.enableLogging(this.environmentHelper, this.getLogger());
        ContainerUtil.service(this.environmentHelper, new ComponentManagerWrapper(this.manager));
        this.environmentHelper.changeContext(sitemapSource, prefix);
    }

    /**
     * Create a new child of this processor (used for mounting submaps).
     *
     * @return a new child processor.
     */
    public TreeProcessor createChildProcessor(String src, 
                                              boolean checkReload,
                                              String  prefix) 
    throws Exception {
        DelayedRefreshSourceWrapper delayedSource = new DelayedRefreshSourceWrapper(
            this.resolver.resolveURI(src), this.lastModifiedDelay);
        return new TreeProcessor(this, delayedSource, checkReload, prefix);
    }

    /* (non-Javadoc)
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(org.apache.avalon.framework.context.Context)
     */
    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    /* (non-Javadoc)
     * @see org.apache.avalon.framework.component.Composable#compose(org.apache.avalon.framework.component.ComponentManager)
     */
    public void compose(ComponentManager manager) throws ComponentException {
        this.manager = manager;
        this.resolver = (SourceResolver)this.manager.lookup(SourceResolver.ROLE);
    }

    /* (non-Javadoc)
     * @see org.apache.avalon.excalibur.component.RoleManageable#setRoleManager(org.apache.avalon.excalibur.component.RoleManager)
     */
    public void setRoleManager(RoleManager rm) {
        this.roleManager = rm;
    }

    /* (non-Javadoc)
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        // setup the environment helper
        if (this.environmentHelper == null ) {
            this.environmentHelper = new EnvironmentHelper(
                (String) this.context.get(ContextHelper.CONTEXT_ROOT_URL));
        }
        ContainerUtil.enableLogging(this.environmentHelper,getLogger());
        ContainerUtil.service(this.environmentHelper, new ComponentManagerWrapper(manager));
    }

/*
  <processor>
    <reload delay="10"/>
    <root-language name="sitemap"/>
    <language>...</language>
  </processor>
*/
    public void configure(Configuration config)
    throws ConfigurationException {
        this.fileName = config.getAttribute("file", null);
        this.checkReload = config.getAttributeAsBoolean("check-reload", true);

        Configuration rootLangConfig = config.getChild("root-language", false);
        if (rootLangConfig != null) {
            this.language = rootLangConfig.getAttribute("name");
        }

        // Obtain the configuration file, or use the XCONF_URL if none
        // is defined
        String xconfURL = config.getAttribute("config", XCONF_URL);

        // Reload check delay. Default is 1 second.
        this.lastModifiedDelay = config.getChild("reload").getAttributeAsLong("delay", 1000L);

        // Read the builtin languages definition file
        Configuration builtin;
        try {
            Source source = this.resolver.resolveURI( xconfURL );
            try {
                SAXConfigurationHandler handler = new SAXConfigurationHandler();
                SourceUtil.toSAX( new ComponentManagerWrapper(this.manager), source, null, handler);
                builtin = handler.getConfiguration();
            } finally {
                this.resolver.release( source );
            }
        } catch(Exception e) {
            String msg = "Error while reading " + xconfURL + ": " + e.getMessage();
            throw new ConfigurationException(msg, e);
        }

        // Create a selector for tree builders of all languages
        this.builderSelector = new ExtendedComponentSelector(Thread.currentThread().getContextClassLoader());
        try {
            LifecycleHelper.setupComponent(this.builderSelector,
                getLogger(),
                this.context,
                this.manager,
                this.roleManager,
                builtin
            );
        } catch(ConfigurationException ce) {
            throw ce;
        } catch(Exception e) {
            throw new ConfigurationException("Could not setup builder selector", e);
        }
    }

    /**
     * Process the given <code>Environment</code> producing the output.
     * @return If the processing is successfull <code>true</code> is returned.
     *         If not match is found in the sitemap <code>false</code>
     *         is returned.
     * @throws org.apache.cocoon.ResourceNotFoundException If a sitemap component tries
     *                                   to access a resource which can not
     *                                   be found, e.g. the generator
     *         ConnectionResetException  If the connection was reset
     */
    public boolean process(Environment environment) throws Exception {
        InvokeContext context = new InvokeContext();

        context.enableLogging(getLogger());

        try {
            return process(environment, context);
        } finally {
            context.dispose();
        }
    }

    /**
     * Do the actual processing, be it producing the response or just building the pipeline
     * @param environment
     * @param context
     * @return true if the pipeline was successfully built, false otherwise.
     * @throws Exception
     */
    protected boolean process(Environment environment, InvokeContext context)
    throws Exception {

        // first, check for sitemap changes
        if (this.rootNode == null ||
            (this.checkReload && this.source.getLastModified() > this.lastModified)) {
            setupRootNode(environment);
        }

        // and now process
        EnvironmentHelper.enterProcessor(this, this.serviceManager, environment);

        final Redirector oldRedirector = context.getRedirector();

        // Build a redirector
        TreeProcessorRedirector redirector = new TreeProcessorRedirector(environment, context);
        setupLogger(redirector);
        context.setRedirector(redirector);

        try {
            boolean success = this.rootNode.invoke(environment, context);
            
            return success;

        } finally {
            EnvironmentHelper.leaveProcessor();
            // Restore old redirector 
            context.setRedirector(oldRedirector);
        }
    }
        
    private boolean handleCocoonRedirect(String uri, Environment environment, InvokeContext context) throws Exception {
        
        // Build an environment wrapper
        // If the current env is a facade, change the delegate and continue processing the facade, since
        // we may have other redirects that will in turn also change the facade delegate
        
        MutableEnvironmentFacade facade = environment instanceof MutableEnvironmentFacade ?
            ((MutableEnvironmentFacade)environment) : null;
        
        if (facade != null) {
            // Consider the facade delegate (the real environment)
            environment = facade.getDelegate();
        }
        
        // test if this is a call from flow
        boolean isRedirect = (environment.getObjectModel().remove("cocoon:forward") == null);
        Environment newEnv = new ForwardEnvironmentWrapper(environment, uri, getLogger());
        if ( isRedirect ) {
            ((ForwardEnvironmentWrapper)newEnv).setInternalRedirect(true);
        }
        
        if (facade != null) {
            // Change the facade delegate
            facade.setDelegate((EnvironmentWrapper)newEnv);
            newEnv = facade;
        }
        
        // Get the processor that should process this request
        TreeProcessor processor;
        if (getRootProcessor().getContext().equals(this.getContext())) {
            processor = (TreeProcessor)getRootProcessor();
        } else {
            processor = this;
        }
        
        // Process the redirect
// No more reset since with TreeProcessorRedirector, we need to pop values from the redirect location
//        context.reset();
        return processor.process(newEnv, context);
    }
    
    /**
     * Process the given <code>Environment</code> to assemble
     * a <code>ProcessingPipeline</code>.
     * @since 2.1
     */
    public InternalPipelineDescription buildPipeline(Environment environment)
    throws Exception {
        InvokeContext context = new InvokeContext( true );

        context.enableLogging(getLogger());
        context.setLastProcessor(this);
        try {
            if ( process(environment, context) ) {
                return context.getInternalPipelineDescription(environment);
            } else {
                return null;
            }
        } finally {
            context.dispose();
        }
    }
      
    /* (non-Javadoc)
     * @see org.apache.cocoon.Processor#getRootProcessor()
     */
    public Processor getRootProcessor() {
        TreeProcessor result = this;
        while(result.parent != null) {
            result = result.parent;
        }
        
        return result;
    }

    /**
     * Set the sitemap component configurations
     */
    public void setComponentConfigurations(Configuration componentConfigurations) {
        this.componentConfigurations = componentConfigurations;
        this.sitemapComponentConfigurations = null;
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.Processor#getComponentConfigurations()
     */
    public Map getComponentConfigurations() {
        // do we have the sitemap configurations prepared for this processor?
        if ( null == this.sitemapComponentConfigurations ) {
            
            synchronized (this) {

                if ( this.sitemapComponentConfigurations == null ) {
                    // do we have configurations?
                    final Configuration[] childs = (this.componentConfigurations == null 
                                                     ? null 
                                                     : this.componentConfigurations.getChildren());
                    
                    if ( null != childs ) {
        
                        if ( null == this.parent ) {
                            this.sitemapComponentConfigurations = new HashMap(12);
                        } else {
                            // copy all configurations from parent
                            this.sitemapComponentConfigurations = new HashMap(this.parent.getComponentConfigurations()); 
                        }
                        
                        // and now check for new configurations
                        for(int m = 0; m < childs.length; m++) {
                            
                            final String r = this.roleManager.getRoleForName(childs[m].getName());
                            this.sitemapComponentConfigurations.put(r, new ChainedConfiguration(childs[m], 
                                                                             (ChainedConfiguration)this.sitemapComponentConfigurations.get(r)));
                        }
                    } else {
                        // we don't have configurations
                        if ( null == this.parent ) {
                            this.sitemapComponentConfigurations = Collections.EMPTY_MAP;
                        } else {
                            // use configuration from parent
                            this.sitemapComponentConfigurations = this.parent.getComponentConfigurations(); 
                        }
                    }
                }
            }
        }
        return this.sitemapComponentConfigurations;
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.Processor#getContext()
     */
    public String getContext() {
        return this.environmentHelper.getContext();
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.Processor#getEnvironmentHelper()
     */
    public org.apache.cocoon.environment.SourceResolver getSourceResolver() {
        return this.environmentHelper;
    }

    public EnvironmentHelper getEnvironmentHelper() {
        return this.environmentHelper;   
    }

    protected synchronized void setupRootNode(Environment env) throws Exception {

        // Now that we entered the synchronized area, recheck what's already
        // been checked in process().
        if (this.rootNode != null && source.getLastModified() <= this.lastModified) {
            // Nothing changed
            return;
        }

        long startTime = System.currentTimeMillis();

        // Dispose the previous tree, if any
        disposeTree();

        // Get a builder
        TreeBuilder builder = (TreeBuilder)this.builderSelector.select(this.language);
        ProcessingNode root;
        EnvironmentHelper.enterProcessor(this, new ComponentManagerWrapper(this.manager), env);
        try {
            if (builder instanceof Recomposable) {
                ((Recomposable)builder).recompose(this.manager);
            }
            builder.setProcessor(this);
            if (this.fileName == null) {
                this.fileName = builder.getFileName();
            }

            if (this.source == null) {
                this.source = new DelayedRefreshSourceWrapper(this.resolver.resolveURI(this.fileName), lastModifiedDelay);
            }
            root = builder.build(this.source);

            this.sitemapComponentManager = builder.getSitemapComponentManager();
            this.serviceManager = new ComponentManagerWrapper(this.sitemapComponentManager);
            
            this.disposableNodes = builder.getDisposableNodes();
        } finally {
            EnvironmentHelper.leaveProcessor();
            this.builderSelector.release(builder);
        }

        this.lastModified = System.currentTimeMillis();

        if (getLogger().isDebugEnabled()) {
            double time = (this.lastModified - startTime) / 1000.0;
            getLogger().debug("TreeProcessor built in " + time + " secs from " + source.getURI());
        }

        // Finished
        this.rootNode = root;
    }

    public void dispose() {
        disposeTree();
        if (this.parent == null) {
            // root processor : dispose the builder selector
            this.builderSelector.dispose();
        }
        if ( this.manager != null ) {
            if ( this.source != null ) {
                this.resolver.release(this.source.getSource());
                this.source = null;
            }
            this.manager.release(this.resolver);
            this.resolver = null;
            this.manager = null;
        }
    }

    /**
     * Dispose all nodes in the tree that are disposable
     */
    protected void disposeTree() {
        if (this.disposableNodes != null) {
            // we must dispose the nodes in reverse order
            // otherwise selector nodes are freed before the components node
            for(int i=this.disposableNodes.size()-1; i>-1; i--) {
                ((Disposable)disposableNodes.get(i)).dispose();
            }
            this.disposableNodes = null;
        }
    }
    
    private class TreeProcessorRedirector extends ForwardRedirector {
        
        private InvokeContext context;
        public TreeProcessorRedirector(Environment env, InvokeContext context) {
            super(env);
            this.context = context;
        }
        
        protected void cocoonRedirect(String uri) throws IOException, ProcessingException {
            try {
                TreeProcessor.this.handleCocoonRedirect(uri, this.env, this.context);
            } catch(IOException ioe) {
                throw ioe;
            } catch(ProcessingException pe) {
                throw pe;
            } catch(RuntimeException re) {
                throw re;
            } catch(Exception ex) {
                throw new ProcessingException(ex);
            }
        }
    }
    
    /**
     * Local extension of EnvironmentWrapper to propagate otherwise blocked
     * methods to the actual environment.
     */
    private static final class ForwardEnvironmentWrapper extends EnvironmentWrapper {

        public ForwardEnvironmentWrapper(Environment env,
            String uri, Logger logger) throws MalformedURLException {
            super(env, uri, logger);
        }

        public void setStatus(int statusCode) {
            environment.setStatus(statusCode);
        }

        public void setContentLength(int length) {
            environment.setContentLength(length);
        }

        public void setContentType(String contentType) {
            environment.setContentType(contentType);
        }

        public String getContentType() {
            return environment.getContentType();
        }

        public boolean isResponseModified(long lastModified) {
            return environment.isResponseModified(lastModified);
        }
        
        public void setResponseIsNotModified() {
            environment.setResponseIsNotModified();
        }
    }

}
