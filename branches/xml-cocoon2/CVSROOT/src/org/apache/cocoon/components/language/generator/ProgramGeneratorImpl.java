/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.cocoon.components.language.generator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.apache.log.Logger;
import org.apache.avalon.Loggable;
import org.apache.avalon.AbstractLoggable;
import org.apache.avalon.Modifiable;
import org.apache.avalon.Component;
import org.apache.avalon.Composer;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentManagerException;
import org.apache.avalon.Context;
import org.apache.avalon.Contextualizable;
import org.apache.avalon.configuration.Configurable;
import org.apache.avalon.configuration.Configuration;
import org.apache.avalon.configuration.ConfigurationException;
import org.apache.avalon.ComponentSelector;
import org.apache.avalon.ThreadSafe;
import org.apache.avalon.configuration.Parameters;
import org.apache.cocoon.Constants;
import org.apache.cocoon.Roles;
import org.apache.cocoon.components.store.Store;
import org.apache.cocoon.components.language.LanguageException;
import org.apache.cocoon.components.language.markup.sitemap.SitemapMarkupLanguage;
import org.apache.cocoon.components.language.markup.MarkupLanguage;
import org.apache.cocoon.components.language.programming.CodeFormatter;
import org.apache.cocoon.components.language.programming.ProgrammingLanguage;
import org.apache.cocoon.util.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

/**
 * The default implementation of <code>ProgramGenerator</code>
 * @author <a href="mailto:ricardo@apache.org">Ricardo Rocha</a>
 * @version CVS $Revision: 1.1.2.43 $ $Date: 2001-03-19 21:20:21 $
 */
public class ProgramGeneratorImpl extends AbstractLoggable implements ProgramGenerator, Contextualizable, Composer, Configurable, ThreadSafe {

    /** The auto-reloading option */
    protected boolean autoReload = false;

    /** The ComponentSelector for CompiledPages */
    protected GeneratorSelector cache;

    /** The repository store */
    protected Store repository;

    /** The component manager */
    protected ComponentManager manager = null;

    /** The markup language component selector */
    protected ComponentSelector markupSelector;

    /** The programming language component selector */
    protected ComponentSelector languageSelector;

    /** The working directory */
    protected File workDir;

    /** The context root */
    protected String rootPath;

    /** The root package */
    protected String rootPackage;

    /** Set the Cache's logger */
    public void setLogger(Logger log) {
        super.setLogger(log);
    }

    /** Contextualize this class */
    public void contextualize(Context context) {
       if (this.workDir == null) {
           this.workDir = (File) context.get(Constants.CONTEXT_WORK_DIR);
           this.rootPath = (String) context.get(Constants.CONTEXT_ROOT_PATH);
       }
    }

    /**
     * Set the global component manager. This metod also sets the
     * <code>ComponentSelector</code> used as language factory for both markup and programming languages.
     * @param manager The global component manager
     */
    public void compose(ComponentManager manager) throws ComponentManagerException {
        if ((this.manager == null) && (manager != null)) {
            this.manager = manager;
            try {
                this.cache = (GeneratorSelector) this.manager.lookup(Roles.SERVERPAGES);
                this.repository = (Store) this.manager.lookup(Roles.REPOSITORY);
                this.markupSelector = (ComponentSelector)this.manager.lookup(Roles.MARKUP_LANGUAGE);
                this.languageSelector = (ComponentSelector)this.manager.lookup(Roles.PROGRAMMING_LANGUAGE);
            } catch (Exception e) {
                getLogger().warn("Could not lookup Component", e);
            }
        }
    }

    /**
     * Set the sitemap-provided configuration. This method sets the persistent code repository and the auto-reload option
     * @param conf The configuration information
     * @exception ConfigurationException Not thrown here
     */
    public void configure(Configuration conf) throws ConfigurationException {
        Parameters params = Parameters.fromConfiguration(conf);
        this.autoReload = params.getParameterAsBoolean("auto-reload", autoReload);
        this.rootPackage = params.getParameter("root-package", "org.apache.cocoon");
    }

    /**
     * Load a program built from an XML document written in a <code>MarkupLanguage</code>
     * @param file The input document's <code>File</code>
     * @param markupLanguage The <code>MarkupLanguage</code> in which the input document is written
     * @param programmingLanguage The <code>ProgrammingLanguage</code> in which the program must be written
     * @return The loaded program instance
     * @exception Exception If an error occurs during generation or loading
     */
    public CompiledComponent load(File file,
                                  String markupLanguageName,
                                  String programmingLanguageName,
                                  EntityResolver resolver)
    throws Exception {

        // Create filesystem store
        // Set filenames
        String filename = IOUtils.getFullFilename(file);
        StringBuffer contextFilename = new StringBuffer(this.rootPackage.replace('.', File.separatorChar));
        contextFilename.append(File.separator);
        contextFilename.append(IOUtils.getContextFilePath(this.rootPath, filename));
        String normalizedName = IOUtils.normalizedFilename(contextFilename.toString());
        // Ensure no 2 requests for the same file overlap
        Class program = null;
        CompiledComponent programInstance = null;

        // Attempt to load program object from cache
        try {
            programInstance = (CompiledComponent) select(normalizedName);
        } catch (Exception e) {
            getLogger().debug("The instance was not accessible, creating it now.");
        }

        if (programInstance == null) {
            MarkupLanguage markupLanguage = null;
            ProgrammingLanguage programmingLanguage = null;
            try {
                // Get markup and programming languages
                markupLanguage = (MarkupLanguage)this.markupSelector.select(markupLanguageName);
                programmingLanguage = (ProgrammingLanguage)this.languageSelector.select(programmingLanguageName);
                programmingLanguage.setLanguageName(programmingLanguageName);
                program = generateResource(file, normalizedName, markupLanguage, programmingLanguage, resolver);
            } catch (LanguageException le) {
                getLogger().debug("Language Exception", le);
            } finally {
                this.markupSelector.release((Component) markupLanguage);
                this.languageSelector.release((Component) programmingLanguage);
            }

            try {
                programInstance = (CompiledComponent) select(normalizedName);
            } catch (Exception cme) {
                getLogger().debug("Can't load ServerPage", cme);
            }
        }

        if (this.autoReload == false) {
            return programInstance;
        }

        /*
         * FIXME: It's the program (not the instance) that must
         * be queried for changes!!!
         */

        if (programInstance != null && programInstance.modifiedSince(file.lastModified())) {
            // Release the component.
            release(programInstance);

            // Unload program
            ProgrammingLanguage programmingLanguage = (ProgrammingLanguage)this.languageSelector.select(programmingLanguageName);
            programmingLanguage.setLanguageName(programmingLanguageName);
            programmingLanguage.unload(program, normalizedName, this.workDir);

            // Invalidate previous program/instance pair
            program = null;
            programInstance = null;
        }

        if (programInstance == null) {
            if (program == null) {
                MarkupLanguage markupLanguage = null;
                ProgrammingLanguage programmingLanguage = null;
                try {
                    // Get markup and programming languages
                    markupLanguage = (MarkupLanguage)this.markupSelector.select(markupLanguageName);
                    programmingLanguage = (ProgrammingLanguage)this.languageSelector.select(programmingLanguageName);
                    programmingLanguage.setLanguageName(programmingLanguageName);
                    program = generateResource(file, normalizedName, markupLanguage, programmingLanguage, resolver);
                } catch (LanguageException le) {
                    getLogger().debug("Language Exception", le);
                } finally {
                    this.markupSelector.release((Component) markupLanguage);
                    this.languageSelector.release((Component) programmingLanguage);
                }
            }
            // Instantiate
            programInstance = (CompiledComponent) select(normalizedName);
        }

        return programInstance;
    }

    private Class generateResource(File file,
                                  String normalizedName,
                                  MarkupLanguage markupLanguage,
                                  ProgrammingLanguage programmingLanguage,
                                  EntityResolver resolver)
    throws Exception {
        // Generate code
        String code = markupLanguage.generateCode(
            new InputSource(
            new FileReader(file)), normalizedName, programmingLanguage, resolver);
        String encoding = markupLanguage.getEncoding();
        // Format source code if applicable
        CodeFormatter codeFormatter = programmingLanguage.getCodeFormatter();
        if (codeFormatter != null) {
            code = codeFormatter.format(code, encoding);
        }
        // Store generated code
        String sourceFilename = normalizedName + "." + programmingLanguage.getSourceExtension();
        repository.store(sourceFilename, code);
        // [Compile]/Load generated program
        Class program = programmingLanguage.load(normalizedName, this.workDir, markupLanguage.getEncoding());
        // Store generated program in cache
        this.cache.addGenerator(normalizedName, program);

        if (markupLanguage.getClass().getName().equals(SitemapMarkupLanguage.class.getName())) {
            try {
                select("sitemap");
            } catch (Exception e) {
                // If the root sitemap has not been compiled, add an alias here.
                this.cache.addGenerator("sitemap", program);
            }
        }

        return program;
    }

    public CompiledComponent select(String componentName)
        throws Exception {
        CompiledComponent component = (CompiledComponent)this.cache.select(componentName);
        return component;
    }

    public void release(CompiledComponent component) {
        this.cache.release((Component) component);
    }
}
