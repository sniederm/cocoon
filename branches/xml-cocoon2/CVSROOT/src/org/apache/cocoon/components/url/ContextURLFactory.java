/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.components.url;

import java.net.URL;
import java.net.MalformedURLException;

import javax.servlet.ServletContext;

import org.apache.avalon.Context;
import org.apache.avalon.Contextualizable;
import org.apache.avalon.Loggable;

import org.apache.log.Logger;

/**
 * @author <a href="mailto:giacomo@apache.org">Giacomo Pati</a>
 * @version $Id: ContextURLFactory.java,v 1.1.2.1 2001-02-12 13:33:15 giacomo Exp $
 */
public class ContextURLFactory implements URLFactory, Loggable, Contextualizable {

    /**
     * The logger
     */
    protected Logger log;

    /**
     * The context
     */
    protected Context context;

    /**
     * Create a URL from a location. This method supports the
     * <i>context://</i> pseudo-protocol for loading
     * resources accessible from the context root path
     *
     * @param location The location
     * @return The URL pointed to by the location
     * @exception MalformedURLException If the location is malformed
     */
    public URL getURL(String location) throws MalformedURLException {
        ServletContext servletContext = (ServletContext)context.get("servlet-context");
        if (servletContext == null) {
            log.warn("no servlet-context in application context (making an absolute URL)");
            return new URL(location);
        }
        URL u = ((ServletContext)context.get("servlet-context")).getResource(location);
        if (u != null)
            return u;
        else {
            log.error(location + " could not be found. (possible context problem)");
            throw new RuntimeException(location + " could not be found. (possible context problem)");
        }
    }

    public URL getURL(URL base, String location) throws MalformedURLException {
        return getURL(base.toExternalForm() + location);
    }

    /**
     * Get the context
     */
    public void contextualize(Context context) {
        if (this.context == null) {
            this.context = context;
        }
    }

    /**
     * Get the logger
     */
    public void setLogger(Logger logger) {
        if (this.log == null) {
            this.log = logger;
        }
    }
}