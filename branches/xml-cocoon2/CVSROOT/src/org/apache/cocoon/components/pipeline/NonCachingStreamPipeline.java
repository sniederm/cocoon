/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.components.pipeline;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentManagerException;
import org.apache.avalon.ComponentSelector;
import org.apache.avalon.Composer;
import org.apache.avalon.configuration.Parameters;
import org.apache.avalon.AbstractLoggable;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.reading.Reader;
import org.apache.cocoon.serialization.Serializer;
import org.apache.cocoon.sitemap.SitemapComponentSelector;
import org.apache.cocoon.xml.XMLProducer;
import org.apache.cocoon.xml.XMLConsumer;
import org.apache.cocoon.Roles;

import org.xml.sax.EntityResolver;

/** A <CODE>ResourcePipeline</CODE> either
 * <UL>
 *  <LI>collects a <CODE>Reader</CODE> and let it process</LI>
 *  <LI>or connects a <CODE>EventPipeline</CODE> with a
 *  <CODE>Serializer</CODE> and let them produce the requested
 * resource
 * </UL>
 * @author <a href="mailto:Giacomo.Pati@pwr.ch">Giacomo Pati</a>
 * @version CVS $Revision: 1.1.2.1 $ $Date: 2001-04-04 15:42:43 $
 */
public class NonCachingStreamPipeline extends AbstractLoggable implements StreamPipeline {
    private EventPipeline eventPipeline;
    private Reader reader;
    private Parameters readerParam;
    private String readerSource;
    private String readerMimeType;
    private String sitemapReaderMimeType;
    private Serializer serializer;
    private Parameters serializerParam;
    private String serializerSource;
    private String serializerMimeType;
    private String sitemapSerializerMimeType;

    /** the component manager */
    private ComponentManager manager;

    /**
     * Pass the <code>ComponentManager</code> to the <code>composer</code>.
     * The <code>Composer</code> implementation should use the specified
     * <code>ComponentManager</code> to acquire the components it needs for
     * execution.
     *
     * @param manager The <code>ComponentManager</code> which this
     *               <code>Composer</code> uses.
     * @throws ComponentManagerException  */
    public void compose (ComponentManager manager) throws ComponentManagerException {
        this.manager = manager;
    }

    public void setEventPipeline (EventPipeline eventPipeline) throws Exception {
        if (this.eventPipeline != null) {
            throw new ProcessingException ("EventPipeline already set. You can only select one EventPipeline ");
        }
        this.eventPipeline = eventPipeline;
    }

    public void setGenerator (String role, String source, Parameters param, Exception e) throws Exception {
        this.eventPipeline.setGenerator (role, source, param);
    }

    public void setGenerator (String role, String source, Parameters param) throws Exception {
        this.eventPipeline.setGenerator (role, source, param);
    }

    public EventPipeline getEventPipeline () {
        return this.eventPipeline;
    }

    public void setReader (String role, String source, Parameters param)
    throws Exception {
        this.setReader (role, source, param, null);
    }

    public void setReader (String role, String source, Parameters param, String mimeType)
    throws Exception {
        if (this.reader != null) {
            throw new ProcessingException ("Reader already set. You can only select one Reader (" + role + ")");
        }
        SitemapComponentSelector selector = (SitemapComponentSelector) this.manager.lookup(Roles.READERS);
        this.reader = (Reader)selector.select(role);
        this.readerSource = source;
        this.readerParam = param;
        this.readerMimeType = mimeType;
        this.sitemapReaderMimeType = selector.getMimeTypeForRole(role);
    }

    public void setSerializer (String role, String source, Parameters param)
    throws Exception {
        this.setSerializer (role, source, param, null);
    }

    public void setSerializer (String role, String source, Parameters param, String mimeType)
    throws Exception {
        if (this.serializer != null) {
            throw new ProcessingException ("Serializer already set. You can only select one Serializer (" + role + ")");
        }
        SitemapComponentSelector selector = (SitemapComponentSelector) this.manager.lookup(Roles.SERIALIZERS);
        this.serializer = (Serializer)selector.select(role);
        this.serializerSource = source;
        this.serializerParam = param;
        this.serializerMimeType = mimeType;
        this.sitemapSerializerMimeType = selector.getMimeTypeForRole(role);
    }

    public void addTransformer (String role, String source, Parameters param) throws Exception {
        this.eventPipeline.addTransformer (role, source, param);
    }

    public boolean process(Environment environment)
    throws ProcessingException {
        if ( this.reader != null ) {
            return processReader(environment);
        } else {
            if ( !checkPipeline() ) {
                throw new ProcessingException("Attempted to process incomplete pipeline.");
            }

            setupPipeline(environment);
            connectPipeline();

            // execute the pipeline:
            try {
                this.eventPipeline.process(environment);
            } catch ( Exception e ) {
                throw new ProcessingException(
                    "Failed to execute pipeline.",
                    e
                );
            }

            return true;
        }
    }

    /** Process the pipeline using a reader.
     * @throws ProcessingException if
     */
    private boolean processReader(Environment environment)
    throws ProcessingException {
        String mimeType;
        try {
            this.reader.setup((EntityResolver) environment,environment.getObjectModel(),readerSource,readerParam);
            mimeType = this.reader.getMimeType();
            if ( mimeType != null ) {
                environment.setContentType(mimeType);
            } else if ( readerMimeType != null ) {
                environment.setContentType(this.readerMimeType);
            } else {
                environment.setContentType(this.sitemapReaderMimeType);
            }
            this.reader.setOutputStream(environment.getOutputStream());
            this.reader.generate();
        } catch ( Exception e ) {
            throw new ProcessingException("Error reading resource",e);
        }
        return true;
    }

    /** Sanity check the non-reader pipeline.
     * @return true if the pipeline is 'sane', false otherwise.
     */
    private boolean checkPipeline() {
        if ( this.eventPipeline == null ) {
            return false;
        }

        if ( this.serializer == null ) {
            return false;
        }

        return true;
    }

    /** Setup pipeline components.
     */
    private void setupPipeline(Environment environment)
    throws ProcessingException {
        try {
            this.serializer.setOutputStream(environment.getOutputStream());
            String mimeType = this.serializer.getMimeType();
            if (mimeType != null) {
                // we have a mimeType freom the component itself
                environment.setContentType (mimeType);
            } else if (serializerMimeType != null) {
                // there was a mimeType specified in the sitemap pipeline
                environment.setContentType (serializerMimeType);
            } else {
                // use the mimeType specified in the sitemap component declaration
                environment.setContentType (this.sitemapSerializerMimeType);
            }
        } catch (IOException e) {
            throw new ProcessingException(
                "Could not setup resource pipeline.",
                e
            );
        }


    }

    /** Connect the pipeline.
     */
    private void connectPipeline() throws ProcessingException {
        XMLProducer prev = (XMLProducer) this.eventPipeline;
        XMLConsumer next;

        // connect serializer.
        prev.setConsumer(this.serializer);
    }

    public void recycle() {
        getLogger().debug("Recycling of NonCachingStreamPipeline");

        try {
            // release reader.
            if ( this.reader != null ) {
                ((ComponentSelector) this.manager.lookup(Roles.READERS))
                    .release(this.reader);
            }
            this.reader = null;

            // release eventPipeline
            this.eventPipeline = null;

            // release serializer
            if ( this.serializer != null ) {
                ((ComponentSelector) this.manager.lookup(Roles.SERIALIZERS))
                    .release(this.serializer);
            }
            this.serializer = null;
        } catch ( Exception e ) {
            getLogger().warn(
                "Failed to release components from NonCachingStreamPipeline.",
                e
            );
        } finally {
            this.reader = null;
            this.eventPipeline = null;
            this.serializer = null;
        }
    }
}
