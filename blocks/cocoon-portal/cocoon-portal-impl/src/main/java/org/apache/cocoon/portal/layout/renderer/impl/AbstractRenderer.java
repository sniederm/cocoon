/*
 * Copyright 1999-2002,2004-2005 The Apache Software Foundation.
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
package org.apache.cocoon.portal.layout.renderer.impl;

import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.portal.PortalService;
import org.apache.cocoon.portal.layout.Layout;
import org.apache.cocoon.portal.layout.renderer.Renderer;
import org.apache.cocoon.xml.IncludeXMLConsumer;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.xml.xslt.XSLTProcessor;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * Base class for all renderers.
 *
 * @version $Id$
 */
public abstract class AbstractRenderer
    extends AbstractLogEnabled
    implements Renderer, Serviceable, Disposable, ThreadSafe {

    protected ServiceManager manager;

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager aManager) throws ServiceException {
        this.manager = aManager;
    }

    public String getStylesheetURI(Layout layout) {
        return null;
    }

    public boolean useStylesheet() {
        return false;
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
        this.manager = null;
    }

    /**
     * Stream out raw layout 
     */
    public void toSAX(Layout layout, PortalService service, ContentHandler handler) throws SAXException {
        if (this.useStylesheet()) {
            XSLTProcessor processor = null;
            Source stylesheet = null;
            SourceResolver resolver = null;
            try {
                resolver = (SourceResolver) this.manager.lookup(SourceResolver.ROLE);
                stylesheet = resolver.resolveURI(this.getStylesheetURI(layout));
                processor = (XSLTProcessor) this.manager.lookup(XSLTProcessor.ROLE);
                TransformerHandler transformer = processor.getTransformerHandler(stylesheet);
                SAXResult result = new SAXResult(new IncludeXMLConsumer((handler)));
                if (handler instanceof LexicalHandler) {
                    result.setLexicalHandler((LexicalHandler) handler);
                }
                transformer.setResult(result);
                transformer.startDocument();
                this.process(layout, service, transformer);
                transformer.endDocument();
            } catch (Exception ce) {
                throw new SAXException("Unable to lookup component.", ce);
            } finally {
                if (null != resolver) {
                    resolver.release(stylesheet);
                    this.manager.release(resolver);
                }
                this.manager.release(processor);
            }
        } else {
            this.process(layout, service, handler);
        }
    }

    /**
     * Process a Layout
     */
    protected void processLayout(Layout layout, PortalService service, ContentHandler handler) throws SAXException {
        final String rendererName = service.getLayoutFactory().getRendererName(layout);
        Renderer renderer = null;
        renderer = service.getRenderer(rendererName);
        renderer.toSAX(layout, service, handler);
    }

    protected abstract void process(Layout layout, PortalService service, ContentHandler handler) throws SAXException;
}
