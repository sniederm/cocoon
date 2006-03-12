/*
 * Copyright 2005 The Apache Software Foundation
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.blocks.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * Creates a HttpServletResponse object that is usable for internal block calls.
 * 
 * @version $Id:$
 */
public class BlockCallHttpServletResponse implements HttpServletResponse {

    private OutputStream outputStream;
    private ServletOutputStream servletStream;
    private OutputStreamWriter writer;
    private boolean committed;
    private Locale locale;

    public BlockCallHttpServletResponse() {
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#addCookie(javax.servlet.http.Cookie)
     */
    public void addCookie(Cookie cookie) {
        // Ignore
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponseWrapper#addDateHeader(java.lang.String, long)
     */
    public void addDateHeader(String name, long date) {
        // Ignore
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponseWrapper#addHeader(java.lang.String, java.lang.String)
     */
    public void addHeader(String name, String value) {
        // Ignore
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponseWrapper#addIntHeader(java.lang.String, int)
     */
    public void addIntHeader(String name, int value) {
        // Ignore
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#containsHeader(java.lang.String)
     */
    public boolean containsHeader(String name) {
        return false;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#encodeRedirectUrl(java.lang.String)
     */
    public String encodeRedirectUrl(String url) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#encodeRedirectURL(java.lang.String)
     */
    public String encodeRedirectURL(String url) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#encodeUrl(java.lang.String)
     */
    public String encodeUrl(String url) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#encodeURL(java.lang.String)
     */
    public String encodeURL(String url) {
        // TODO Auto-generated method stub
        return null;
    }
    /* (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#flushBuffer()
     */
    public void flushBuffer() throws IOException {
        this.committed = true;
    }
    /* (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#getBufferSize()
     */
    public int getBufferSize() {
        return 0;
    }
    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#getCharacterEncoding()
     */
    public String getCharacterEncoding() {
        // TODO Let it depend on the actual response body
        return "ISO-8859-1";
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#getLocale()
     */
    public Locale getLocale() {
        return this.locale;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#getOutputStream()
     */
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.writer != null)
            throw new IllegalStateException( "Tried to create output stream; writer already exists" );

        return new ServletOutputStream() {

            /* (non-Javadoc)
             * @see java.io.OutputStream#write(int)
             */
            public void write(int b) throws IOException {
                BlockCallHttpServletResponse.this.outputStream.write(b);
            }
        };
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#getWriter()
     */
    public PrintWriter getWriter() throws IOException {
        if (this.servletStream != null)
            throw new IllegalStateException( "Tried to create writer; output stream already exists" );

        return new PrintWriter(new OutputStreamWriter(this.outputStream, this.getCharacterEncoding()));
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#isCommitted()
     */
    public boolean isCommitted() {
        return this.committed;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#reset()
     */
    public void reset() {
        this.resetBuffer();
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#resetBuffer()
     */
    public void resetBuffer() {
        if (this.committed)
            throw new IllegalStateException( "May not resetBuffer after response is committed" );
        this.outputStream = null;
        this.servletStream = null;
        this.writer = null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#sendError(int)
     */
    public void sendError(int sc) throws IOException {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#sendError(int, java.lang.String)
     */
    public void sendError(int sc, String msg) throws IOException {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#sendRedirect(java.lang.String)
     */
    public void sendRedirect(String location) throws IOException {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#setBufferSize(int)
     */
    public void setBufferSize(int size) {
        // TODO Implement buffering, for the moment ignore.
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#setContentLength(int)
     */
    public void setContentLength(int len) {
        // Ignore
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#setContentType(java.lang.String)
     */
    public void setContentType(String type) {
        // Ignore
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponseWrapper#setDateHeader(java.lang.String, long)
     */
    public void setDateHeader(String name, long date) {
        // Ignore
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponseWrapper#setHeader(java.lang.String, java.lang.String)
     */
    public void setHeader(String name, String value) {
        // Ignore
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponseWrapper#setIntHeader(java.lang.String, int)
     */
    public void setIntHeader(String name, int value) {
        // Ignore
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#setLocale(java.util.Locale)
     */
    public void setLocale(Locale loc) {
        this.locale = loc;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;        }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponseWrapper#setStatus(int)
     */
    public void setStatus(int sc) {
        // Ignore
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponseWrapper#setStatus(int, java.lang.String)
     */
    public void setStatus(int sc, String sm) {
        // Ignore
    }
}
