/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.transformation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.StringCharacterIterator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Iterator;
import java.util.Map;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Loggable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.Constants;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.Roles;
import org.apache.cocoon.caching.CacheValidity;
import org.apache.cocoon.caching.Cacheable;
import org.apache.cocoon.caching.CompositeCacheValidity;
import org.apache.cocoon.caching.ParametersCacheValidity;
import org.apache.cocoon.caching.TimeStampCacheValidity;
import org.apache.cocoon.components.browser.Browser;
import org.apache.cocoon.components.store.Store;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.cocoon.util.TraxErrorHandler;
import org.apache.cocoon.xml.ContentHandlerWrapper;
import org.apache.cocoon.xml.XMLConsumer;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.excalibur.pool.Recyclable;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * This Transformer is used to transform this incomming SAX stream using
 * a XSLT stylesheet. Use the following sitemap declarations to define, configure
 * and parameterize it:
 *
 * <b>In the map:sitemap/map:components/map:transformers:</b><br>
 *
 * &lt;map:transformer name="xslt" src="org.apache.cocoon.transformation.TraxTransformer"&gt;<br>
 *   &lt;use-store map:value="true"/&gt;
 *   &lt;use-request-parameters map:value="false"/&gt;
 *   &lt;use-browser-capabilities-db map:value="false"/&gt;
 * &lt;/map:transformer&gt;
 *
 * The &lt;use-store&gt; configuration forces the transformer to put the
 * <code>Templates</code> generated from the XSLT stylesheet into the
 * <code>Store</code>. This property is true by default.
 *
 * The &lt;use-request-parameter&gt; configuration forces the transformer to make all
 * request parameters available in the XSLT stylesheet. Note that this might have issues
 * concerning cachability of the generated output of this transformer.
 * This property is false by default.
 *
 * The &lt;use-browser-capabilities-db&gt; configuration forces the transformer to make all
 * properties from the browser capability database available in the XSLT stylesheetas.
 * Note that this might have issues concerning cachability of the generated output of this
 * transformer.
 * This property is false by default.
 *
 * <b>In a map:sitemap/map:pipelines/map:pipeline:</b><br>
 *
 * &lt;map:transform type="xslt" src="stylesheets/yours.xsl"&gt;<br>
 *   &lt;parameter name="myparam" value="myvalue"/&gt;
 * &lt;/map:transform&gt;
 *
 * All <parameter> declarations will be made available in the XSLT stylesheet as
 * xsl:variables.
 *
 * @author <a href="mailto:fumagalli@exoffice.com">Pierpaolo Fumagalli</a>
 *         (Apache Software Foundation, Exoffice Technologies)
 * @author <a href="mailto:dims@yahoo.com">Davanum Srinivas</a>
 * @author <a href="mailto:cziegeler@apache.org">Carsten Ziegeler</a>
 * @author <a href="mailto:giacomo@apache.org">Giacomo Pati</a>
 * @version CVS $Id: TraxTransformer.java,v 1.1.2.31 2001-05-09 12:09:16 cziegeler Exp $
 */
public class TraxTransformer extends ContentHandlerWrapper
implements Transformer, Composable, Recyclable, Configurable, Cacheable, Disposable {
    private static String FILE = "file:/";

    /** The store service instance */
    private Store store = null;

    /** The Browser service instance */
    private Browser browser = null;

    /** The trax TransformerFactory */
    private SAXTransformerFactory tfactory = null;

    /** The trax TransformerHandler */
    private TransformerHandler transformerHandler = null;

    /** Is the store turned on? (default is on) */
    private boolean useStore = true;

    /** Should we make the request parameters available in the stylesheet? (default is off) */
    private boolean useParameters = false;

    /** Should we make the browser capability properties available in the stylesheet? (default is off) */
    private boolean useBrowserCap = false;

    private ComponentManager manager;

    private long xslFileLastModified;

    /** The InputSource */
    private InputSource inputSource;
    private String systemID;
    private String xsluri;
    private Parameters par;
    private Map objectModel;
    private EntityResolver resolver;

    TransformerHandler getTransformerHandler(EntityResolver resolver)
    throws SAXException, ProcessingException, IOException, TransformerConfigurationException {
        Templates templates = getTemplates();
        if(templates == null) {
            getLogger().debug("Creating new Templates in " + this + " for" + systemID + ":" + xsluri);
            templates = getTransformerFactory().newTemplates(new SAXSource(this.inputSource));
            putTemplates (templates);
        } else {
            getLogger().debug("Reusing Templates in " + this + " for" + systemID + ":" + xsluri);
        }

        TransformerHandler handler = getTransformerFactory().newTransformerHandler(templates);
        if(handler == null) {
            /* If there is a problem in getting the handler, try using a brand new Templates object */
            getLogger().debug("Re-creating new Templates in " + this + " for" + systemID + ":" + xsluri);
            templates = getTransformerFactory().newTemplates(new SAXSource(resolver.resolveEntity(null, this.xsluri)));
            putTemplates (templates);
            handler = getTransformerFactory().newTransformerHandler(templates);
        }

        handler.getTransformer().setErrorListener(new TraxErrorHandler(getLogger()));
        return handler;
    }

    private Templates getTemplates ()
    throws IOException {
        Templates templates = null;
        if (this.useStore == false)
            return null;

        // only stylesheets with a last modification date are stored
        if (this.xslFileLastModified != 0) {

            // Stored is an array of the template and the caching time
            if (store.containsKey(xsluri) == true) {
                Object[] templateAndTime = (Object[])store.get(xsluri);
                if(templateAndTime != null && templateAndTime[1] != null) {
                    long storedTime = ((Long)templateAndTime[1]).longValue();
                    if (storedTime < this.xslFileLastModified) {
                        store.remove(xsluri);
                    } else {
                        templates = (Templates)templateAndTime[0];
                    }
                }
            }
        } else {
            // remove an old template if it exists
            if (store.containsKey(xsluri) == true) {
               store.remove(xsluri);
            }
        }
        return templates;
    }

    private void putTemplates (Templates templates)
    throws IOException {
        if (this.useStore == false)
            return;

        // only stylesheets with a last modification date are stored
        if (this.xslFileLastModified != 0) {

            // Stored is an array of the template and the current time
            Object[] templateAndTime = new Object[2];
            templateAndTime[0] = templates;
            templateAndTime[1] = new Long(this.xslFileLastModified);
            store.hold(this.xsluri, templateAndTime);
        }
    }

    /**
     * Helper for TransformerFactory.
     */
    private synchronized SAXTransformerFactory getTransformerFactory() {
        if(tfactory == null)  {
            tfactory = (SAXTransformerFactory) TransformerFactory.newInstance();
            tfactory.setErrorListener(new TraxErrorHandler(getLogger()));
        }
        return tfactory;
    }

    /**
     * Configure this transformer.
     */
    public void configure(Configuration conf)
    throws ConfigurationException {
        if (conf != null) {
            Configuration child = conf.getChild("use-store");
            this.useStore = child.getValueAsBoolean(true);
            getLogger().debug("Use store is " + this.useStore + " for " + this);
            child = conf.getChild("use-request-parameters");
            this.useParameters = child.getValueAsBoolean(false);
            getLogger().debug("Use parameters is " + this.useParameters + " for " + this);
            child = conf.getChild("use-browser-capabilities-db");
            this.useBrowserCap = child.getValueAsBoolean(false);
            getLogger().debug("Use browser capabilities is " + this.useBrowserCap + " for " + this);
        }
    }

    /**
     * Set the current <code>ComponentManager</code> instance used by this
     * <code>Composable</code>.
     */
    public void compose(ComponentManager manager) {
        try {
            this.manager = manager;
            getLogger().debug("Looking up " + Roles.STORE);
            this.store = (Store) manager.lookup(Roles.STORE);
            getLogger().debug("Looking up " + Roles.BROWSER);
            this.browser = (Browser) manager.lookup(Roles.BROWSER);
        } catch (Exception e) {
            getLogger().error("Could not find component", e);
        }
    }

    /**
     * Set the <code>EntityResolver</code>, the <code>Map</code> with
     * the object model, the source and sitemap
     * <code>Parameters</code> used to process the request.
     */
    public void setup(EntityResolver resolver, Map objectModel, String src, Parameters par)
    throws SAXException, ProcessingException, IOException {

        // Check the stylesheet uri
        this.xsluri = src;
        if (this.xsluri == null) {
            throw new ProcessingException("Stylesheet URI can't be null");
        }
        this.par = par;
        this.objectModel = objectModel;
        this.inputSource = resolver.resolveEntity(null, this.xsluri);
        this.systemID = inputSource.getSystemId();
        this.resolver = resolver;

        this.xslFileLastModified = 0;
        if (this.systemID.startsWith(FILE) == true) {
            File xslFile = new File(this.systemID.substring(FILE.length()));
            this.xslFileLastModified = xslFile.lastModified();
        }
        getLogger().debug("Using stylesheet: '"+this.xsluri+"' in " + this + ", last modified: " + this.xslFileLastModified);
    }

    /**
     * Generate the unique key.
     * This key must be unique inside the space of this component.
     *
     * @return The generated key hashes the src
     */
    public long generateKey() {
        if (this.xslFileLastModified != 0) {
            return HashUtil.hash(this.xsluri);
        } else {
            return 0;
        }
    }

    /**
     * Generate the validity object.
     *
     * @return The generated validity object or <code>null</code> if the
     *         component is currently not cacheable.
     */
    public CacheValidity generateValidity() {
        if (this.xslFileLastModified != 0) {
            HashMap map = getLogicSheetParameters();
            if (map == null) {
                return new TimeStampCacheValidity(this.xslFileLastModified);
            } else {
                return new CompositeCacheValidity(
                                    new ParametersCacheValidity(map),
                                    new TimeStampCacheValidity(this.xslFileLastModified)
                                    );
            }
        }
        return null;
    }

    /**
     * Set the <code>XMLConsumer</code> that will receive XML data.
     * <br>
     * This method will simply call <code>setContentHandler(consumer)</code>
     * and <code>setLexicalHandler(consumer)</code>.
     */
    public void setConsumer(XMLConsumer consumer) {

        /** Get a Transformer Handler */
        try {
            transformerHandler = getTransformerHandler(resolver);
        } catch (TransformerConfigurationException e){
            getLogger().error("Problem in getTransformer:", e);
            throw new RuntimeException("Problem in getTransformer:" + e.getMessage());
        } catch (SAXException e){
            getLogger().error("Problem in getTransformer:", e);
            throw new RuntimeException("Problem in getTransformer:" + e.getMessage());
        } catch (IOException e){
            getLogger().error("Problem in getTransformer:", e);
            throw new RuntimeException("Problem in getTransformer:" + e.getMessage());
        } catch (ProcessingException e){
            getLogger().error("Problem in getTransformer:", e);
            throw new RuntimeException("Problem in getTransformer:" + e.getMessage());
        }

        HashMap map = getLogicSheetParameters();
        if (map != null) {
            Iterator iterator = map.keySet().iterator();
            while(iterator.hasNext()) {
                String name = (String)iterator.next();
                transformerHandler.getTransformer().setParameter(name,map.get(name));
            }
        }

        super.setContentHandler(transformerHandler);
        if(transformerHandler instanceof Loggable) {
            ((Loggable)transformerHandler).setLogger(getLogger());
        }
        if(transformerHandler instanceof org.xml.sax.ext.LexicalHandler)
            this.setLexicalHandler((org.xml.sax.ext.LexicalHandler)transformerHandler);

        this.setContentHandler(consumer);
    }

    private HashMap getLogicSheetParameters() {
        HashMap map = null;
        if (par != null) {
            Iterator params = par.getParameterNames();
            while (params.hasNext()) {
                String name = (String) params.next();
                if (isValidXSLTParameterName(name)) {
                    String value = par.getParameter(name,null);
                    if (value != null) {
                        if (map == null) {
                            map = new HashMap();
                        }
                        map.put(name,value);
                    }
                }
            }
        }

        if (this.useParameters) {
            /** The Request object */
            Request request = (Request) objectModel.get(Constants.REQUEST_OBJECT);

            if (request != null) {
                Enumeration parameters = request.getParameterNames();
                if ( parameters != null ) {
                    while (parameters.hasMoreElements()) {
                        String name = (String) parameters.nextElement();
                        if (isValidXSLTParameterName(name)) {
                            String value = request.getParameter(name);
                            if (map == null) {
                                map = new HashMap();
                            }
                            map.put(name,value);
                        }
                    }
                }
            }
        }

        if (this.useBrowserCap) try {
            Request request = (Request) objectModel.get(Constants.REQUEST_OBJECT);
            if (map == null) {
                map = new HashMap();
            }
            /* Get the accept header; it's needed to get the browser type. */
            String accept = request.getParameter("accept");
            if (accept == null)
                accept = request.getHeader("accept");

            /* Get the user agent; it's needed to get the browser type. */
            String agent = request.getParameter("user-Agent");
            if (agent == null)
                agent = request.getHeader("user-Agent");

            /* add the accept param */
            map.put("accept", accept);

            /* add the user agent param */
            map.put("user-agent", java.net.URLEncoder.encode(agent));

            /* add the map param */
            HashMap agmap = browser.getBrowser(agent, accept);
            map.put("browser", agmap);

            /* add the media param */
            String browserMedia = browser.getMedia(agmap);
            map.put("browser-media", browserMedia);

            /* add the uaCapabilities param */
            org.w3c.dom.Document uaCapabilities = browser.getUACapabilities(agmap);
            map.put("ua-capabilities", uaCapabilities);
        } catch (Exception e) {
            getLogger().error("Error setting Browser info", e);
        }

        return map;
    }

    /**
     * Set the <code>ContentHandler</code> that will receive XML data.
     * <br>
     * Subclasses may retrieve this <code>ContentHandler</code> instance
     * accessing the protected <code>super.contentHandler</code> field.
     */
    public void setContentHandler(ContentHandler content) {
        transformerHandler.setResult(new SAXResult(content));
    }

    /**
     * Set the <code>LexicalHandler</code> that will receive XML data.
     * <br>
     * Subclasses may retrieve this <code>LexicalHandler</code> instance
     * accessing the protected <code>super.lexicalHandler</code> field.
     *
     * @exception IllegalStateException If the <code>LexicalHandler</code> or
     *                                  the <code>XMLConsumer</code> were
     *                                  already set.
     */
    public void setLexicalHandler(LexicalHandler lexical) {
    }

    // FIXME (SM): this method may be a hotspot for requests with many
    //             parameters we should try to optimize it further
    static boolean isValidXSLTParameterName(String name) {
        StringCharacterIterator iter = new StringCharacterIterator(name);
        char c = iter.first();
        if (!(Character.isLetter(c) || c == '_')) {
            return false;
        } else {
            c = iter.next();
        }
        while (c != iter.DONE) {
            if (!(Character.isLetterOrDigit(c) ||
                c == '-' ||
                c == '_' ||
                c == '.')) {
                return false;
            } else {
                c = iter.next();
            }
        }

        return true;
    }

    public void dispose()
    {
        if(this.store != null)
            this.manager.release((Component)this.store);
        if(this.browser != null)
            this.manager.release((Component)this.browser);
    }

    public void recycle()
    {
        //FIXME: Patch for Xalan2J, to stop transform threads if
        //       there is a failure in the pipeline.
        try {
            Class clazz =
                Class.forName("org.apache.xalan.stree.SourceTreeHandler");
            Class  paramTypes[] =
                    new Class[]{ Exception.class };
            Object params[] =
                    new Object[] { new SAXException("Dummy Exception") };
            if(clazz.isInstance(transformerHandler)) {
                Method method = clazz.getMethod("setExceptionThrown",paramTypes);
                method.invoke(transformerHandler,params);
            }
        } catch (Exception e){
            getLogger().debug("Exception in recycle:", e);
        }
        this.transformerHandler = null;
        this.objectModel = null;
        this.inputSource = null;
        this.par = null;
        this.systemID = null;
        this.xsluri = null;
        this.resolver = null;
        super.recycle();
    }
}
