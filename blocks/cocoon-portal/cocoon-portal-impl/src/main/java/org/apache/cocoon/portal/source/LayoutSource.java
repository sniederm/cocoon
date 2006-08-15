/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.cocoon.portal.source;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.CascadingIOException;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.portal.LayoutException;
import org.apache.cocoon.portal.PortalService;
import org.apache.cocoon.portal.layout.renderer.Renderer;
import org.apache.cocoon.portal.om.Layout;
import org.apache.cocoon.serialization.Serializer;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceNotFoundException;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.xml.sax.XMLizable;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * This is the source implementation of the layout source.
 *
 * @version $Id$
 */
public class LayoutSource 
    implements Source, XMLizable {

    protected final ServiceManager manager;

    protected final Context context;

    protected final String uri;
    protected final Layout layout;
    protected final PortalService portalService;

    /** The used protocol */
    protected final String scheme;

    public LayoutSource(String uri,
                        String protocol,
                        Layout layout,
                        PortalService service,
                        ServiceManager manager,
                        Context context) {
        this.uri = uri;
        this.scheme = protocol;
        this.layout = layout;
        this.portalService = service;
        this.manager = manager;
        this.context = context;
    }

	/**
	 * @see org.apache.excalibur.source.Source#getInputStream()
	 */
	public InputStream getInputStream() throws IOException, SourceNotFoundException {
        try {
            ServiceManager sitemapManager = (ServiceManager) this.context.get(ContextHelper.CONTEXT_SITEMAP_SERVICE_MANAGER);
            Serializer serializer = null;
            try {
                serializer = (Serializer) sitemapManager.lookup(Serializer.ROLE+ "/xml");
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                serializer.setOutputStream(os);
                this.toSAX(serializer);
                return new ByteArrayInputStream(os.toByteArray());
            } catch (SAXException se) {
                throw new CascadingIOException("Unable to stream content.", se);
            } catch (ServiceException ce) {
                throw new CascadingIOException("Unable to get components for serializing.", ce);
            } finally {
                sitemapManager.release(serializer);
            }
        } catch (ContextException ce) {
            throw new CascadingIOException("Unable to get sitemap service manager.", ce);
        }
	}

	/**
	 * @see org.apache.excalibur.source.Source#getURI()
	 */
	public String getURI() {
		return this.uri;
	}

	/**
	 * @see org.apache.excalibur.source.Source#getValidity()
	 */
	public SourceValidity getValidity() {
		return null;
	}

	/**
	 * @see org.apache.excalibur.source.Source#refresh()
	 */
	public void refresh() {
        // nothing to do 
	}

	/**
	 * @see org.apache.excalibur.source.Source#getMimeType()
	 */
	public String getMimeType() {
		return null;
	}

	/**
	 * @see org.apache.excalibur.source.Source#getContentLength()
	 */
	public long getContentLength() {
		return -1;
	}

	/**
	 * @see org.apache.excalibur.source.Source#getLastModified()
	 */
	public long getLastModified() {
		return 0;
	}

	/**
	 * @see org.apache.excalibur.xml.sax.XMLizable#toSAX(ContentHandler)
	 */
	public void toSAX(ContentHandler handler) 
    throws SAXException {
        Renderer portalLayoutRenderer = this.portalService.getRenderer( this.portalService.getLayoutFactory().getRendererName(this.layout));

        handler.startDocument();
        try {
            portalLayoutRenderer.toSAX(this.layout, this.portalService, handler);
        } catch (LayoutException e) {
            throw new SAXException(e);
        }
        handler.endDocument();
	}

    /**
     * @see org.apache.excalibur.source.Source#exists()
     */
    public boolean exists() {
        return true;
    }

    /**
     * @see org.apache.excalibur.source.Source#getScheme()
     */
    public String getScheme() {
        return this.scheme;
    }
}
