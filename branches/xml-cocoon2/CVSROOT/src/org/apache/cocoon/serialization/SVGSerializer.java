/*****************************************************************************
 * Copyright (C) 1999 The Apache Software Foundation.   All rights reserved. *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1,  a copy of wich has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.cocoon.serialization;

import org.apache.cocoon.*;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.components.parser.Parser;
import org.apache.cocoon.xml.*;
import org.apache.cocoon.xml.dom.*;
import org.apache.avalon.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Iterator;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.w3c.dom.*;
import org.w3c.dom.svg.*;
import org.apache.batik.transcoder.*;
import org.apache.cocoon.util.ClassUtils;

/**
 * A Batik based Serializer for generating PNG/JPEG images
 *
 * @author <a href="mailto:dims@yahoo.com">Davanum Srinivas</a>
 * @author <a href="mailto:rossb@apache.org">Ross Burton</a>
 * @version CVS $Revision: 1.1.2.26 $ $Date: 2001-02-22 17:10:45 $
 */
public class SVGSerializer extends SVGBuilder implements Composer, Serializer, Configurable, Poolable {

    /** The <code>ContentHandler</code> receiving SAX events. */
    private ContentHandler contentHandler=null;
    /** The <code>LexicalHandler</code> receiving SAX events. */
    private LexicalHandler lexicalHandler=null;
    /** The component manager instance */
    private ComponentManager manager=null;
    /** The current <code>Environment</code>. */
    private Environment environment=null;
    /** The current <code>Parameters</code>. */
    private Configuration config=null;
    /** The current <code>OutputStream</code>. */
    private OutputStream output=null;
    /** The current <code>mime-type</code>. */
    private String mimetype = null;
    /** The current <code>Transcoder</code>.  */
    Transcoder transcoder = null;

    /**
     * Set the <code>OutputStream</code> where the XML should be serialized.
     */
    public void setOutputStream(OutputStream out) {
        this.output = new BufferedOutputStream(out);
    }

    /**
     * Set the configurations for this serializer.
     */
    public void configure(Configuration conf) throws ConfigurationException {
        this.mimetype = conf.getAttribute("mime-type");
        log.debug("SVGSerializer mime-type:" + mimetype);
        // TODO: take the mime type and create a transcoder from it
        // allow the parameter "transcoder" to override it however
        String transcoderName = null; // TODO: whatever the factory will say it is

        // Iterate through the parameters, looking for a transcoder reference
        for (Iterator i = conf.getChildren("parameter"); i.hasNext(); ) {
            Configuration paramConf = (Configuration)i.next();
            String name = paramConf.getAttribute("name");
            if ("transcoder".equals(name)) {
                transcoderName = paramConf.getAttribute("value");
            }
        }
        // Now try creating this transcoder
        try {
            this.transcoder = (Transcoder)ClassUtils.newInstance(transcoderName);
        } catch (Exception ex) {
            log.error("Cannot load  class " + transcoderName, ex);
            throw new ConfigurationException("Cannot load class " + transcoderName, ex);
        }
        // Now run through the other parameters, using them as hints
        // to the transcoder
        for (Iterator i = conf.getChildren("parameter"); i.hasNext(); ) {
            Configuration paramConf = (Configuration)i.next();
            String name = paramConf.getAttribute("name");
            // Skip over the parameters we've dealt with. Ensure this
            // is kept in sync with the above list!
            if ("transcoder".equals(name)) continue;
            // Now try and get the hints out
            try {
                // Turn it into a key name (assume the current Batik style continues!
                name = ("KEY_" + name).toUpperCase();
                // Use reflection to get a reference to the key object
                TranscodingHints.Key key = (TranscodingHints.Key)
                    (transcoder.getClass().getField(name).get(transcoder));
                Object value;
                String keyType = paramConf.getAttribute("type", "STRING").toUpperCase();
                if ("FLOAT".equals(keyType)) {
                    // Can throw an exception.
                    value = new Float(paramConf.getAttributeAsFloat("value"));
                } else if ("INTEGER".equals(keyType)) {
                    // Can throw an exception.
                    value = new Integer(paramConf.getAttributeAsInt("value"));
                } else if ("BOOLEAN".equals(keyType)) {
                    // Can throw an exception.
                    value = new Boolean(paramConf.getAttributeAsBoolean("value"));
                } else if ("COLOR".equals(keyType)) {
                  // Can throw an exception
                  String stringValue = paramConf.getAttribute("value");
                  if (stringValue.startsWith("#")) {
                    stringValue = stringValue.substring(1);
                  }
                  value = new Color(Integer.parseInt(stringValue, 16));
                } else {
                    // Assume String, and get the value. Allow an empty string.
                    value = paramConf.getValue("");
                }
                // TODO: if (logger.isDebug())
                log.debug("SVG Serializer: adding hint \"" + name + "\" with value \"" + value.toString() + "\"");
                transcoder.addTranscodingHint(key, value);
            } catch (ClassCastException ex) {
                // This is only thrown from the String keyType... line
                throw new ConfigurationException("Specified key (" + name + ") is not a valid Batik Transcoder key.", ex);
            } catch (ConfigurationException ex) {
                throw new ConfigurationException("Name or value not specified.", ex);
            } catch (IllegalAccessException ex) {
                throw new ConfigurationException("Cannot access the key for parameter \"" + name + "\"", ex);
            } catch (NoSuchFieldException ex) {
                throw new ConfigurationException("No field available for parameter \"" + name + "\"", ex);
            }
        }
    }

    /**
     * Set the current <code>ComponentManager</code> instance used by this
     * <code>Composer</code>.
     */
    public void compose(ComponentManager manager) {
        this.manager = manager;
    }

    /**
     * Set the <code>XMLConsumer</code> that will receive XML data.
     * <br>
     * This method will simply call <code>setContentHandler(consumer)</code>
     * and <code>setLexicalHandler(consumer)</code>.
     */
    public void setConsumer(XMLConsumer consumer) {
        this.contentHandler=consumer;
        this.lexicalHandler=consumer;
    }

    /**
     * Set the <code>ContentHandler</code> that will receive XML data.
     * <br>
     * Subclasses may retrieve this <code>ContentHandler</code> instance
     * accessing the protected <code>super.contentHandler</code> field.
     */
    public void setContentHandler(ContentHandler content) {
        this.contentHandler=content;
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
        this.lexicalHandler=lexical;
    }

    /**
     * Receive notification of a successfully completed DOM tree generation.
     */
    public void notify(Document doc) throws SAXException {
        try {
            TranscoderInput transInput = new TranscoderInput(doc);
            TranscoderOutput transOutput = new TranscoderOutput(this.output);
            transcoder.transcode(transInput, transOutput);
            this.output.flush();
        } catch (Exception ex) {
            log.error("SVGSerializer: Exception writing image", ex);
            throw new SAXException("Exception writing image ", ex);
        }
    }

    /**
     * Return the MIME type.
     */
    public String getMimeType() {
        return mimetype;
    }
}
