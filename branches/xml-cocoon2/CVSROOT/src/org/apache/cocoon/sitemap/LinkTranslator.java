/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.cocoon.sitemap;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;

import org.apache.avalon.utils.Parameters;

import org.apache.cocoon.Cocoon;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.xml.xlink.ExtendedXLinkPipe;
import org.apache.cocoon.transformation.Transformer;
import org.apache.cocoon.util.NetUtils;

/**
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @version CVS $Revision: 1.1.2.10 $ $Date: 2000-10-06 21:25:31 $
 */

public class LinkTranslator extends ExtendedXLinkPipe implements Transformer {

    private Map links;

    /**
     * Set the <code>EntityResolver</code>, objectModel <code>Map</code>,
     * the source and sitemap <code>Parameters</code> used to process the request.
     */
    public void setup(EntityResolver resolver, Map objectModel, String src, Parameters par)
    throws ProcessingException, SAXException, IOException {
        this.links = (Map) objectModel.get(Cocoon.LINK_OBJECT);
    }

    public void simpleLink(String href, String role, String arcrole, String title, String show, String actuate, String uri, String name, String raw, Attributes attr)
    throws SAXException {
        String newHref = (String) this.links.get(href);
        super.simpleLink((newHref != null) ? newHref : href, role, arcrole, title, show, actuate, uri, name, raw, attr);
    }

    public void startLocator(String href, String role, String title, String label, String uri, String name, String raw, Attributes attr)
    throws SAXException {
        String newHref = (String) this.links.get(href);
        super.startLocator((newHref != null) ? newHref : href, role, title, label, uri, name, raw, attr);
    }
}
