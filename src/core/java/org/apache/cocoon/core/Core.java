/*
 * Copyright 2005 The Apache Software Foundation
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.avalon.framework.CascadingRuntimeException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.cocoon.Constants;
import org.apache.cocoon.configuration.Settings;

/**
 * This is the core Cocoon component.
 * It can be looked up to get access to various information about the
 * current installation.
 *
 * The core of Cocoon is a singleton object that is created on startup.
 *
 * @version SVN $Id$
 * @since 2.2
 */
public class Core
    implements Contextualizable {

    /** Application <code>Context</code> Key for the settings. Please don't
     * use this constant to lookup the settings object. Lookup the core
     * component and use {@link #getSettings()} instead. */
    public static final String CONTEXT_SETTINGS = "settings";

    /**
     * The cleanup threads that are invoked after the processing of a
     * request is finished.
     */
    private static final ThreadLocal cleanup = new ThreadLocal();

    /** The component context. */
    private Context context;

    private final Settings settings;
    
    public Core() {
        this.settings = null;
    }

    public Core(Settings s) {
        this.settings = s;
    }

    /* (non-Javadoc)
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(org.apache.avalon.framework.context.Context)
     */
    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public static void addCleanupTask(CleanupTask task) {
        List l = (List)cleanup.get();
        if ( l == null ) {
            l = new ArrayList();
            cleanup.set(l);
        }
        l.add(task);
    }

    public static void cleanup() {
        List l = (List)cleanup.get();
        if ( l != null ) {
            final Iterator i = l.iterator();
            while ( i.hasNext() ) {
                final CleanupTask t = (CleanupTask)i.next();
                t.invoke();
            }
            l.clear();
        }
    }

    public static interface CleanupTask {

        void invoke();
    }

    /**
     * Return the settings.
     */
    public Settings getSettings() {
        return getSettings(this.context);
        // return this.settings;
    }

    /**
     * Return the component context.
     * This method allows access to the component context for other components
     * that are not created by an Avalon based container.
     * FIXME - will be removed before the release
     */
    public Context getContext() {
        return this.context;
    }

    /**
     * Return the environment context object.
     * @return The environment context.
     */
    public org.apache.cocoon.environment.Context getEnvironmentContext() {
        try {
            return (org.apache.cocoon.environment.Context)this.context.get(Constants.CONTEXT_ENVIRONMENT_CONTEXT);
        } catch (ContextException ce) {
            throw new CascadingRuntimeException("Unable to get the environment object from the context.", ce);
        }
    }
    
    public File getWorkDirectory() {
        try {
            return (File)this.context.get(Constants.CONTEXT_WORK_DIR);
        } catch (ContextException ce) {
            throw new CascadingRuntimeException("Unable to get the working directory from the context.", ce);
        }        
    }

    public File getUploadDirectory() {
        try {
            return (File)this.context.get(Constants.CONTEXT_UPLOAD_DIR);
        } catch (ContextException ce) {
            throw new CascadingRuntimeException("Unable to get the upload directory from the context.", ce);
        }        
    }

    public File getCacheDirectory() {
        try {
            return (File)this.context.get(Constants.CONTEXT_CACHE_DIR);
        } catch (ContextException ce) {
            throw new CascadingRuntimeException("Unable to get the cache directory from the context.", ce);
        }        
    }

    /**
     * Return the current settings.
     * Please don't use this method directly, look up the Core component
     * and use {@link #getSettings()} instead.
     * @param context The component context.
     * @return The settings.
     * FIXME - will be removed before the release
     */
    public static final Settings getSettings(Context context) {
        // the settings object is always present
        try {
            return (Settings)context.get(CONTEXT_SETTINGS);
        } catch (ContextException ce) {
            throw new CascadingRuntimeException("Unable to get the settings object from the context.", ce);
        }
    }

    /**
     * Get the settings for Cocoon
     * @param env This provides access to various parts of the used environment.
     */
    public static Settings createSettings(BootstrapEnvironment env) {
        // create an empty settings objects
        final Settings s = new Settings();

        String additionalPropertyFile = System.getProperty(Settings.PROPERTY_USER_SETTINGS);
        
        // read cocoon-settings.properties - if available
        InputStream propsIS = env.getInputStream("cocoon-settings.properties");
        if ( propsIS != null ) {
            env.log("Reading settings from 'cocoon-settings.properties'");
            final Properties p = new Properties();
            try {
                p.load(propsIS);
                propsIS.close();
                s.fill(p);
                additionalPropertyFile = p.getProperty(Settings.PROPERTY_USER_SETTINGS, additionalPropertyFile);
            } catch (IOException ignore) {
                env.log("Unable to read 'cocoon-settings.properties'.", ignore);
                env.log("Continuing initialization.");
            }
        }
        // fill from the environment configuration, like web.xml etc.
        env.configure(s);
        
        // read additional properties file
        if ( additionalPropertyFile != null ) {
            env.log("Reading user settings from '" + additionalPropertyFile + "'");
            final Properties p = new Properties();
            try {
                FileInputStream fis = new FileInputStream(additionalPropertyFile);
                p.load(fis);
                fis.close();
            } catch (IOException ignore) {
                env.log("Unable to read '" + additionalPropertyFile + "'.", ignore);
                env.log("Continuing initialization.");
            }
        }
        // now overwrite with system properties
        s.fill(System.getProperties());

        return s;        
    }

    public static interface BootstrapEnvironment {

        void log(String message);
        void log(String message, Throwable error);
        
        InputStream getInputStream(String path);
        
        void configure(Settings settings);
        
        ClassLoader getInitClassLoader();

        org.apache.cocoon.environment.Context getEnvironmentContext();
        
        String getContextPath();
    }
    
}
