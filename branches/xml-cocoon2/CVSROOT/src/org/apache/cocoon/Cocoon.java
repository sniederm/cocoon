/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.avalon.activity.Disposable;
import org.apache.avalon.activity.Initializable;
import org.apache.avalon.component.Component;
import org.apache.avalon.component.Composable;
import org.apache.avalon.configuration.Configurable;
import org.apache.avalon.configuration.Configuration;
import org.apache.avalon.configuration.ConfigurationException;
import org.apache.avalon.configuration.SAXConfigurationHandler;
import org.apache.avalon.context.Context;
import org.apache.avalon.context.ContextException;
import org.apache.avalon.context.Contextualizable;
import org.apache.avalon.logger.AbstractLoggable;
import org.apache.avalon.thread.ThreadSafe;
import org.apache.cocoon.Modifiable;
import org.apache.cocoon.components.language.generator.CompiledComponent;
import org.apache.cocoon.components.language.generator.ProgramGenerator;
import org.apache.cocoon.components.parser.Parser;
import org.apache.cocoon.components.pipeline.EventPipeline;
import org.apache.cocoon.components.pipeline.StreamPipeline;
import org.apache.cocoon.components.store.FilesystemStore;
import org.apache.cocoon.components.url.URLFactory;
import org.apache.cocoon.components.url.URLFactory;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.serialization.Serializer;
import org.apache.cocoon.sitemap.Manager;
import org.apache.cocoon.util.ClassUtils;
import org.apache.cocoon.util.NetUtils;
import org.apache.excalibur.component.DefaultComponentManager;
import org.apache.excalibur.component.DefaultRoleManager;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The Cocoon Object is the main Kernel for the entire Cocoon system.
 *
 * @author <a href="mailto:fumagalli@exoffice.com">Pierpaolo Fumagalli</a> (Apache Software Foundation, Exoffice Technologies)
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @version CVS $Revision: 1.4.2.80 $ $Date: 2001-04-27 15:14:11 $
 */
public class Cocoon extends AbstractLoggable implements ThreadSafe, Component, Initializable, Disposable, Modifiable, Processor, Contextualizable {
    /** The application context */
    private Context context;

    /** The configuration file */
    private URL configurationFile;

    /** The sitemap file */
    private String sitemapFileName;

    /** The configuration tree */
    private Configuration configuration;

    /** The sitemap manager */
    private Manager sitemapManager;

    /** The classpath (null if not available) */
    private String classpath;

    /** The working directory (null if not available) */
    private File workDir;

    /** Check reloading of sitemap */
    private boolean checkSitemapReload = true;

    /** reload sitemap asynchron */
    private boolean reloadSitemapAsynchron = true;

    /** The component manager. */
    private DefaultComponentManager componentManager;

    /** flag for disposed or not */
    private boolean disposed = false;

    /** Create a new <code>Cocoon</code> instance. */
    public Cocoon() throws ConfigurationException {
        // Set the system properties needed by Xalan2.
        setSystemProperties();
    }

    public void contextualize(Context context) throws ContextException {
        if (this.context == null) {
            this.context = context;
            this.classpath = (String)context.get(Constants.CONTEXT_CLASSPATH);
            this.workDir = (File)context.get(Constants.CONTEXT_WORK_DIR);
            this.configurationFile = (URL)context.get(Constants.CONTEXT_CONFIG_URL);
        }
    }

    public void initialize() throws Exception {
        this.componentManager = new DefaultComponentManager();
        this.componentManager.setLogger(getLogger());
        this.componentManager.contextualize(this.context);

        getLogger().debug("New Cocoon object.");
        // Setup the default parser, for parsing configuration.
        // If one need to use a different parser, set the given system property
        String parser = System.getProperty(Constants.PARSER_PROPERTY, Constants.DEFAULT_PARSER);
        getLogger().debug("Using parser: " + parser);

        try {
            this.componentManager.addComponent(Roles.PARSER, ClassUtils.loadClass(parser), null);
        } catch (Exception e) {
            getLogger().error("Could not load parser, Cocoon object not created.", e);
            throw new ConfigurationException("Could not load parser " + parser, e);
        }

        try {
            getLogger().debug("Creating Repository with this directory: " + this.workDir);
            FilesystemStore repository = new FilesystemStore();
            repository.setLogger(getLogger());
            repository.setDirectory(this.workDir);
            this.componentManager.addComponentInstance(Roles.REPOSITORY, repository);
        } catch (IOException e) {
            getLogger().error("Could not create repository!", e);
            throw new ConfigurationException("Could not create the repository!", e);
        }

        getLogger().debug("Classpath = " + classpath);
        getLogger().debug("Work directory = " + workDir.getCanonicalPath());
        this.configure();
    }

    /** Configure this <code>Cocoon</code> instance. */
    public void configure() throws ConfigurationException, ContextException {
        Parser p = null;
        Configuration roleConfig = null;

        try {
            p = (Parser)this.componentManager.lookup(Roles.PARSER);
            SAXConfigurationHandler b = new SAXConfigurationHandler();
            ClassLoader cl = (ClassLoader) this.context.get(Constants.CONTEXT_CLASS_LOADER);
            InputStream inputStream = ClassUtils.getResource("org/apache/cocoon/cocoon.roles").openStream();
            InputSource is = new InputSource(inputStream);
            p.setContentHandler(b);
            is.setSystemId(this.configurationFile.toExternalForm());
            p.parse(is);
            roleConfig = b.getConfiguration();
        } catch (Exception e) {
            getLogger().error("Could not configure Cocoon environment", e);
            throw new ConfigurationException("Error trying to load configurations", e);
        } finally {
            if (p != null) this.componentManager.release((Component) p);
        }

        DefaultRoleManager drm = new DefaultRoleManager();
        drm.setLogger(getLogger());
        drm.configure(roleConfig);
        this.componentManager.setRoleManager(drm);
        roleConfig = null;

        try {
            p = (Parser)this.componentManager.lookup(Roles.PARSER);
            SAXConfigurationHandler b = new SAXConfigurationHandler();
            InputSource is = new InputSource(this.configurationFile.openStream());
            p.setContentHandler(b);
            is.setSystemId(this.configurationFile.toExternalForm());
            p.parse(is);
            this.configuration = b.getConfiguration();
        } catch (Exception e) {
            getLogger().error("Could not configure Cocoon environment", e);
            throw new ConfigurationException("Error trying to load configurations",e);
        } finally {
            if (p != null) this.componentManager.release((Component) p);
        }

        Configuration conf = this.configuration;

        getLogger().debug("Root configuration: " + conf.getName());
        if (! "cocoon".equals(conf.getName())) {
            throw new ConfigurationException("Invalid configuration file\n" + conf.toString());
        }
        getLogger().debug("Configuration version: " + conf.getAttribute("version"));
        if (Constants.CONF_VERSION.equals(conf.getAttribute("version")) == false) {
            throw new ConfigurationException("Invalid configuration schema version. Must be '" + Constants.CONF_VERSION + "'.");
        }
        getLogger().debug("Setting up components...");
        this.componentManager.configure(conf);
        getLogger().debug("Setting up the sitemap.");
        // Create the sitemap
        Configuration sconf = conf.getChild("sitemap");
        this.sitemapManager = new Manager();
        this.sitemapManager.setLogger(getLogger());
        this.sitemapManager.contextualize(this.context);
        this.sitemapManager.compose(this.componentManager);
        this.sitemapManager.configure(conf);
        this.sitemapFileName = sconf.getAttribute("file");
        if (this.sitemapFileName == null) {
            getLogger().error("No sitemap file name");
            throw new ConfigurationException("No sitemap file name\n" + conf.toString());
        }
        String value = sconf.getAttribute("check-reload", "yes");
        this.checkSitemapReload = !(value != null && value.equalsIgnoreCase("no") == true);
        value = sconf.getAttribute("reload-method", "asynchron");
        this.reloadSitemapAsynchron = !(value != null && value.equalsIgnoreCase("synchron") == true);
        getLogger().debug("Sitemap location = " + this.sitemapFileName);
        getLogger().debug("Checking sitemap reload = " + this.checkSitemapReload);
        getLogger().debug("Reloading sitemap asynchron = " + this.reloadSitemapAsynchron);
    }

    /** Queries the class to estimate its ergodic period termination. */
    public boolean modifiedSince(long date) {
        boolean answer;
        try {
            answer = date < this.configurationFile.openConnection().getLastModified();
        } catch (IOException ioe) {
            getLogger().warn("Problem checking the date on the Configuration File.", ioe);
            answer = false;
        }
        return answer;
    }

    /** Sets required system properties . */
    protected void setSystemProperties() {
        java.util.Properties props = new java.util.Properties();
        // FIXME We shouldn't have to specify the SAXParser...
        // This is needed by Xalan2, it is used by org.xml.sax.helpers.XMLReaderFactory
        // to locate the SAX2 driver.
        props.put("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
        java.util.Properties systemProps = System.getProperties();
        Enumeration propEnum = props.propertyNames();
        while (propEnum.hasMoreElements()) {
            String prop = (String)propEnum.nextElement();
            if (!systemProps.containsKey(prop))
                systemProps.put(prop, props.getProperty(prop));
        }
        System.setProperties(systemProps);
    }

    public synchronized void dispose() {
        this.disposed = true;

        this.componentManager.dispose();
    }

    /**
     * Process the given <code>Environment</code> to produce the output.
     */
    public boolean process(Environment environment)
    throws Exception {
        if (disposed) throw new IllegalStateException("You cannot process a Disposed Cocoon engine.");
        return this.sitemapManager.invoke(environment, "", this.sitemapFileName,
                 this.checkSitemapReload, this.reloadSitemapAsynchron);
    }

    /**
     * Process the given <code>Environment</code> to assemble
     * a <code>StreamPipeline</code> and an <code>EventPipeline</code>.
     */
    public boolean process(Environment environment, StreamPipeline pipeline, EventPipeline eventPipeline)
    throws Exception {
        if (disposed) throw new IllegalStateException("You cannot process a Disposed Cocoon engine.");
        return this.sitemapManager.invoke(environment, "", this.sitemapFileName,
              this.checkSitemapReload, this.reloadSitemapAsynchron,
              pipeline, eventPipeline);
    }

    /**
     * Process the given <code>Environment</code> to generate the sitemap.
     */
    public void generateSitemap(Environment environment)
    throws Exception {
        ProgramGenerator programGenerator = null;
        URLFactory urlFactory = null;
        try {
            programGenerator = (ProgramGenerator) this.componentManager.lookup(Roles.PROGRAM_GENERATOR);
            urlFactory = (URLFactory) this.componentManager.lookup(Roles.URL_FACTORY);
            File sourceFile = new File(urlFactory.getURL(environment.resolveEntity(null, sitemapFileName).getSystemId()).getFile());
            String markupLanguage = "sitemap";
            String programmingLanguage = "java";

            getLogger().debug("Sitemap regeneration begin:" + sitemapFileName);
            CompiledComponent smap = (CompiledComponent) programGenerator.load(sourceFile, markupLanguage, programmingLanguage, environment);
            getLogger().debug("Sitemap regeneration complete");

            if (smap != null) {
                getLogger().debug("Main: The sitemap has been successfully compiled!");
            } else {
                getLogger().debug("Main: No errors, but the sitemap has not been set.");
            }
        } catch (Exception e) {
            getLogger().error("Main: Error compiling sitemap", e);
            throw e;
        } finally {
            if (programGenerator != null) this.componentManager.release((Component) programGenerator);
            if (urlFactory != null) this.componentManager.release((Component) urlFactory);
        }
    }

    /**
     * Process the given <code>Environment</code> to generate Java code for specified XSP files.
     */
    public void generateXSP(String fileName, Environment environment)
    throws Exception {
        ProgramGenerator programGenerator = null;
        URLFactory urlFactory = null;
        try {
            getLogger().debug("XSP generation begin:" + fileName);

            programGenerator = (ProgramGenerator) this.componentManager.lookup(Roles.PROGRAM_GENERATOR);
            urlFactory = (URLFactory) this.componentManager.lookup(Roles.URL_FACTORY);
            File sourceFile = new File(urlFactory.getURL(environment.resolveEntity(null, fileName).getSystemId()).getFile());
            String markupLanguage = "xsp";
            String programmingLanguage = "java";

            CompiledComponent xsp = (CompiledComponent) programGenerator.load(sourceFile, markupLanguage, programmingLanguage, environment);
            getLogger().debug("XSP generation complete:" + xsp);

            this.componentManager.release((Component) programGenerator);
        } catch (Exception e) {
            getLogger().error("Main: Error compiling XSP", e);
            throw e;
        } finally {
            if (programGenerator != null) this.componentManager.release((Component) programGenerator);
            if (urlFactory != null) this.componentManager.release((Component) urlFactory);
        }
    }
}
