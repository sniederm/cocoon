/*****************************************************************************
 * Copyright (C) 1999 The Apache Software Foundation.   All rights reserved. *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1,  a copy of wich has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.cocoon.sitemap.Request;

/**
 *
 * @author <a href="mailto:fumagalli@exoffice.com">Pierpaolo Fumagalli</a>, 
 *         Exoffice Technologies, INC.</a>
 * @author Copyright 1999 &copy; <a href="http://www.apache.org">The Apache
 *         Software Foundation</a>. All rights reserved.
 * @version CVS $Revision: 1.1.2.1 $ $Date: 2000-02-11 13:14:47 $
 */
public class CocoonServletRequest implements Request, HttpServletRequest {
    /** Deny public construction */
    private CocoonServletRequest() {}
    
    /** This request <code>HttpServletRequest</code> */
    private HttpServletRequest request=null;
    /** This request source uri */
    private String source=null;
    
    /**
     * Create a new instance of this <code>CocoonServletRequest</code>
     */
    protected CocoonServletRequest(HttpServletRequest req) {
        super();
        this.request=req;
    }

    /**
     * Get the requested URI in the target URI-space.
     */
    public String getPathInfo() {
        return(this.request.getPathInfo());
    }

    /**
     * Get the requested URI in the source URI-space.
     */
    public String getPathTranslated() {
        return(this.source==null?this.request.getPathTranslated():this.source);
    }
    
    /**
     * Set the requested URI in the source URI-space.
     *
     * @exception IllegalStateException If this method was already called.
     * @exception NullPointerException If the specified uri was null.
     */
    public void setPathTranslated(String uri)
    throws IllegalStateException, NullPointerException {
        if(uri==null) throw new NullPointerException("Null Path Translated");
        if(this.source!=null) throw new IllegalStateException("Already called");
        this.source=uri;
    }

    /**
     * Returns the name of the user making this request, or null if not known.
     */
    public String getRemoteUser() {
        return(this.request.getRemoteUser());
    }

    public int getContentLength() {
        return(this.request.getContentLength());
    }

    public String getContentType() {
        return(this.request.getContentType());
    }

    public String getProtocol() {
        return(this.request.getProtocol());
    }

    public String getScheme() {
        return(this.request.getScheme());
    }

    public String getServerName() {
        return(this.request.getServerName());
    }

    public int getServerPort() {
        return(this.request.getServerPort());
    }

    public String getRemoteAddr() {
        return(this.request.getRemoteAddr());
    }

    public String getRemoteHost() {
        return(this.request.getRemoteHost());
    }

    public String getRealPath(String path) {
        return(this.request.getRealPath(path));
    }

    public ServletInputStream getInputStream()
    throws IOException {
        return(this.request.getInputStream());
    }

    public String getParameter(String name) {
        return(this.request.getParameter(name));
    }

    public String[] getParameterValues(String name) {
        return(this.request.getParameterValues(name));
    }

    public Enumeration getParameterNames() {
        return(this.request.getParameterNames());
    }

    public Object getAttribute(String name) {
        return(this.request.getAttribute(name));
    }

    public BufferedReader getReader()
    throws IOException {
        return(this.request.getReader());
    }

    public String getCharacterEncoding() {
        return(this.request.getCharacterEncoding());
    }

    public Cookie[] getCookies() {
        return(this.request.getCookies());
    }

    public String getMethod() {
        return(this.request.getMethod());
    }

    public String getRequestURI() {
        return(this.request.getRequestURI());
    }

    public String getServletPath() {
        return(this.request.getServletPath());
    }

    public String getQueryString() {
        return(this.request.getQueryString());
    }

    public String getAuthType() {
        return(this.request.getAuthType());
    }

    public String getHeader(String name) {
        return(this.request.getHeader(name));
    }

    public int getIntHeader(String name) {
        return(this.request.getIntHeader(name));
    }

    public long getDateHeader(String name) {
        return(this.request.getDateHeader(name));
    }

    public Enumeration getHeaderNames() {
        return(this.request.getHeaderNames());
    }

    public HttpSession getSession(boolean create) {
        return(this.request.getSession(create));
    }
    
    public String getRequestedSessionId() {
        return(this.request.getRequestedSessionId());
    }

    public boolean isRequestedSessionIdValid() {
        return(this.request.isRequestedSessionIdValid());
    }

    public boolean isRequestedSessionIdFromCookie() {
        return(this.request.isRequestedSessionIdFromCookie());
    }

    public boolean isRequestedSessionIdFromUrl() {
        return(this.request.isRequestedSessionIdFromUrl());
    }
}
