/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.cocoon.sitemap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.ext.LexicalHandler;

import org.apache.avalon.Component;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentManagerException;
import org.apache.avalon.Composer;
import org.apache.avalon.configuration.Parameters;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.Roles;
import org.apache.cocoon.caching.Cacheable;
import org.apache.cocoon.caching.CacheValidity;
import org.apache.cocoon.caching.TimeStampCacheValidity;
import org.apache.cocoon.components.pipeline.EventPipeline;
import org.apache.cocoon.components.pipeline.StreamPipeline;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.generation.Generator;
import org.apache.cocoon.sitemap.Sitemap;
import org.apache.cocoon.xml.ContentHandlerWrapper;
import org.apache.cocoon.xml.XMLConsumer;
import org.apache.cocoon.xml.XMLProducer;

/**
 * @author <a href="mailto:giacomo@apache.org">Giacomo Pati</a>
 * @version CVS $Id: ContentAggregator.java,v 1.1.2.1 2001-04-19 11:30:31 giacomo Exp $
 */

public class ContentAggregator extends ContentHandlerWrapper 
        implements Generator, Cacheable, Composer {
    /** the current sitemap */
    protected Sitemap sitemap;
    
    /** the root sitemap */
    protected Sitemap rootSitemap;
    
    /** the root element of the aggregated content */
    protected String rootElement;
    
    /** the namespace of the root element */
    protected String rootElementNS;
    
    /** the elements of the parts */
    protected ArrayList partElements = new ArrayList();
    
    /** the namespaces of the parts */
    protected ArrayList partNSs = new ArrayList();
    
    /** the URIs of the parts */
    protected ArrayList partURIs = new ArrayList();

    /** The current <code>Environment</code>. */
    protected Environment environment;

    /** The current <code>EntityResolver</code>. */
    protected EntityResolver resolver;

    /** The current <code>Map</code> objectModel. */
    protected Map objectModel;

    /** The current <code>Parameters</code>. */
    protected Parameters parameters;

    /** The source URI associated with the request or <b>null</b>. */
    protected String source;

    /** The <code>XMLConsumer</code> receiving SAX events. */
    protected XMLConsumer xmlConsumer;

    /** The <code>ContentHandler</code> receiving SAX events. */
    protected ContentHandler contentHandler;

    /** The <code>LexicalHandler</code> receiving SAX events. */
    protected LexicalHandler lexicalHandler;

    /** The <code>ComponentManager</code> */
    protected ComponentManager manager;
    
    private final AttributesImpl attrs = new AttributesImpl();
    private ArrayList partEventPipelines = new ArrayList();

    /**
     * Pass the <code>ComponentManager</code> to the <code>composer</code>.
     * The <code>Composer</code> implementation should use the specified
     * <code>ComponentManager</code> to acquire the components it needs for
     * execution.
     *
     * @param manager The <code>ComponentManager</code> which this
     *                <code>Composer</code> uses.
     */
    public void compose(ComponentManager manager) throws ComponentManagerException {
        if (this.manager == null) {
            this.manager = manager;
        }
    }
    
    /**
     * generates the content
     */
    public void generate() throws IOException, SAXException, ProcessingException {
        getLogger().debug("ContentAggregator: generating aggregated content"); 
        collectParts();
        this.documentHandler.startDocument();
        this.documentHandler.startElement(this.rootElementNS, this.rootElement, this.rootElement, attrs);
        try {
            for (int i = 0; i < this.partEventPipelines.size(); i++) {
                this.documentHandler.startElement((String)this.partNSs.get(i), (String)this.partElements.get(i), 
                        (String)this.partElements.get(i), attrs);
                EventPipeline ep = (EventPipeline)this.partEventPipelines.get(i);
                ((XMLProducer)ep).setConsumer(this);
                try {
                    this.environment.pushURI((String)this.partURIs.get(i));
                    ep.process(this.environment);
                } catch (Exception e) {
                    getLogger().error("ContentAggregator: cannot process event pipeline for URI " + this.partURIs.get(i), e);
                    throw new ProcessingException ("ContentAggregator: cannot process event pipeline for URI " + this.partURIs.get(i), e);
                } finally {
                    this.manager.release(ep);
                    this.environment.popURI();
                    this.documentHandler.endElement((String)this.partNSs.get(i), (String)this.partElements.get(i), 
                            (String)this.partElements.get(i));
                }
            }
        } finally {
            this.documentHandler.endElement(this.rootElementNS, this.rootElement, this.rootElement);
            this.documentHandler.endDocument();
        }
        getLogger().debug("ContentAggregator: finished aggregating content"); 

    }

    private void collectParts() throws ProcessingException {
        if (this.partEventPipelines.size() == 0) {
            EventPipeline eventPipeline = null;
            StreamPipeline pipeline = null;
            for (int i = 0; i < this.partElements.size(); i++) {
                getLogger().debug("ContentAggregator: collecting internal resource " 
                        + (String)this.partURIs.get(i));
                try {
                    eventPipeline = (EventPipeline)this.manager.lookup(Roles.EVENT_PIPELINE);
                    this.partEventPipelines.add(eventPipeline);
                    pipeline = (StreamPipeline)this.manager.lookup(Roles.STREAM_PIPELINE);
                } catch (ComponentManagerException cme) {
                    getLogger().error("ContentAggregator: could not lookup pipeline components", cme);
                    throw new ProcessingException ("could not lookup pipeline components", cme);
                }
                try {
                    pipeline.setEventPipeline(eventPipeline);
                } catch (Exception cme) {
                    getLogger().error("ContentAggregator: could not set event pipeline on stream pipeline", cme);
                    throw new ProcessingException ("could not set event pipeline on stream pipeline", cme);
                }
                try {
                    this.environment.pushURI((String)this.partURIs.get(i));
                    this.sitemap.process(this.environment, pipeline, eventPipeline);
                } catch (Exception cme) {
                    getLogger().error("ContentAggregator: could not process pipeline", cme);
                    throw new ProcessingException ("could not process pipeline", cme);
                } finally {
                    this.manager.release(pipeline);
                    this.environment.popURI();
                }
            }
        }
    }
    
    /**
     * Generate the unique key.
     * This key must be unique inside the space of this component.
     *
     * @return The generated key hashes the src
     */
    public long generateKey() {
        try {
            collectParts();
        } catch (Exception e) {
            getLogger().error ("cannot collect pipeline parts", e);
        }
        //if (this.systemID.startsWith("file:") == true) {
        //    return HashUtil.hash(super.source);
        //}
        return 0;
    }

    /**
     * Generate the validity object.
     *
     * @return The generated validity object or <code>null</code> if the
     *         component is currently not cacheable.
     */
    public CacheValidity generateValidity() {
        //if (this.systemID.startsWith("file:") == true) {
        //    File xmlFile = new File(this.systemID.substring("file:".length()));
        //    return new TimeStampCacheValidity(xmlFile.lastModified());
        //}
        return null;
    }
    
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
    
    public void setSitemap(Sitemap sitemap) {
        this.sitemap = sitemap;
    }
    
    public void setRootSitemap(Sitemap sitemap) {
        this.rootSitemap = sitemap;
    }
    
    public void setRootElement(String element, String namespace) {
        this.rootElement = element;
        if (namespace == null) {
            this.rootElementNS = "";
        } else {
            this.rootElementNS = namespace;
        }
        getLogger().debug("ContentAggregator: root element='" + element + "' ns='" + namespace + "'");
    }
    
    public void addPart(String uri, String element, String namespace) {
        if (namespace == null) {
            this.partNSs.add("");
        } else {
            this.partNSs.add(namespace);
        }
        this.partURIs.add(uri);
        this.partElements.add(element);
        getLogger().debug("ContentAggregator: part uri='" + uri + "' element='" + element + "' ns='" + namespace + "'");
    }

    /**
     * Set the <code>XMLConsumer</code> that will receive XML data.
     * <br>
     * This method will simply call <code>setContentHandler(consumer)</code>
     * and <code>setLexicalHandler(consumer)</code>.
     */
    public void setConsumer(XMLConsumer consumer) {
        this.setContentHandler(consumer);
        this.xmlConsumer = consumer;
        this.contentHandler = consumer;
        this.lexicalHandler = consumer;
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
    public void setLexicalHandler(LexicalHandler handler) {
        this.lexicalHandler = handler;
    }

    /**
     * Recycle the producer by removing references
     */
    public void recycle () {
        super.recycle();
        this.sitemap = null;
        this.rootSitemap = null;
        this.resolver = null;
        this.objectModel = null;
        this.source = null;
        this.parameters = null;
        this.rootElement = null;
        this.rootElementNS = null;
        this.partURIs.clear();
        this.partElements.clear();
        this.partNSs.clear();
        this.environment = null;
        this.partEventPipelines.clear();
        this.xmlConsumer = null;
        this.contentHandler = null;
        this.lexicalHandler = null;
    }

    /**
     * Set the <code>EntityResolver</code>, object model <code>Map</code>,
     * the source and sitemap <code>Parameters</code> used to process the request.
     */
    public void setup(EntityResolver resolver, Map objectModel, String src, Parameters par)
        throws ProcessingException, SAXException, IOException {
        this.resolver=resolver;
        this.objectModel=objectModel;
        this.source=src;
        this.parameters=par;
    }

    /**
     * Ignore start and end document events
     */
    public void startDocument () throws SAXException {
    }
    
    public void endDocument () throws SAXException {
    }
}
