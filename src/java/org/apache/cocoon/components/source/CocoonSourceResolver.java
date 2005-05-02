/*
 * Copyright 1999-2004 The Apache Software Foundation.
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
package org.apache.cocoon.components.source;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.avalon.framework.CascadingRuntimeException;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.Processor;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.internal.EnvironmentHelper;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceException;
import org.apache.excalibur.source.SourceFactory;
import org.apache.excalibur.source.URIAbsolutizer;

/**
 * This is the default implementation of the {@link SourceResolver} for
 * Cocoon. The implementation is based on the original source resolver implementation
 * from the Excalibur project.
 * @since 2.2
 * 
 * @author <a href="mailto:cziegeler@s-und-n.de">Carsten Ziegeler</a>
 * @version CVS $Id$
*/
public class CocoonSourceResolver 
extends AbstractLogEnabled
implements SourceResolver, Contextualizable, Serviceable, Disposable {

    /** A (optional) custom source resolver */
    protected org.apache.excalibur.source.SourceResolver customResolver;

    /** The service manager */
    protected ServiceManager manager;

    /** The base URL */
    protected URL baseURL;

    /** The component context */
    protected Context context;

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(org.apache.avalon.framework.context.Context)
     */
    public void contextualize( Context context )
    throws ContextException {
        this.context = context;
        try {
            if( context.get( "context-root" ) instanceof URL ) {
                this.baseURL = (URL)context.get( "context-root" );
            } else {
                this.baseURL = ( (File)context.get( "context-root" ) ).toURL();
            }
        } catch( ContextException ce ) {
            // set the base URL to the current directory
            try {
                this.baseURL = new File( System.getProperty( "user.dir" ) ).toURL();
                if( this.getLogger().isDebugEnabled() ) {
                    this.getLogger().debug( "SourceResolver: Using base URL: " + this.baseURL );
                }
            } catch( MalformedURLException mue ) {
                throw new ContextException( "Malformed URL for user.dir, and no container.rootDir exists", mue );
            }
        } catch( MalformedURLException mue ) {
            throw new ContextException( "Malformed URL for container.rootDir", mue );
        }
    }

    /**
     * @see org.apache.excalibur.source.SourceResolver#resolveURI(java.lang.String, java.lang.String, java.util.Map)
     */
    public Source resolveURI(String location, String baseURI, Map parameters)
    throws MalformedURLException, IOException, SourceException {
        if ( baseURI == null ) {
            final Processor processor = EnvironmentHelper.getCurrentProcessor();
            if ( processor != null ) {
                baseURI = processor.getContext();
            }
        }
        if ( this.customResolver != null ) {
            return this.customResolver.resolveURI(location, baseURI, parameters);
        } else {
            if( this.getLogger().isDebugEnabled() ) {
                this.getLogger().debug( "Resolving '" + location + "' with base '" + baseURI + "' in context '" + this.baseURL + "'" );
            }
            if( location == null ) {
                throw new MalformedURLException( "Invalid System ID" );
            }
            if( null != baseURI && org.apache.excalibur.source.SourceUtil.indexOfSchemeColon(baseURI) == -1 ) {
                throw new MalformedURLException( "BaseURI is not valid, it must contain a protocol: " + baseURI );
            }

            if( baseURI == null ) {
                baseURI = this.baseURL.toExternalForm();
            }

            String systemID = location;
            // special handling for windows file paths
            if( location.length() > 1 && location.charAt( 1 ) == ':' ) {
                systemID = "file:/" + location;
            } else if( location.length() > 2 && location.charAt(0) == '/' && location.charAt(2) == ':' ) {
                systemID = "file:" + location;
            }

            // determine protocol (scheme): first try to get the one of the systemID, if that fails, take the one of the baseURI
            String protocol;
            int protocolPos = org.apache.excalibur.source.SourceUtil.indexOfSchemeColon(systemID);
            if( protocolPos != -1 ) {
                protocol = systemID.substring( 0, protocolPos );
            } else {
                protocolPos = org.apache.excalibur.source.SourceUtil.indexOfSchemeColon(baseURI);
                if( protocolPos != -1 ) {
                    protocol = baseURI.substring( 0, protocolPos );
                } else {
                    protocol = "*";
                }
            }

            final ServiceManager m = this.getServiceManager();

            Source source = null;
            // search for a SourceFactory implementing the protocol
            SourceFactory factory = null;
            try {
                factory = this.getSourceFactory( m, protocol );
                systemID = this.absolutize( factory, baseURI, systemID );
                if( getLogger().isDebugEnabled() ) {
                    getLogger().debug( "Resolved to systemID : " + systemID );
                }
                source = factory.getSource( systemID, parameters );
            } catch( final ServiceException ce ) {
                // no selector available, use fallback
            } finally {
                m.release( factory );
            }

            if( null == source ) {
                try {
                    factory = this.getSourceFactory( m, "*");
                    systemID = this.absolutize( factory, baseURI, systemID );
                    if( getLogger().isDebugEnabled() ) {
                        getLogger().debug( "Resolved to systemID : " + systemID );
                    }
                    source = factory.getSource( systemID, parameters );
                } catch (ServiceException se ) {
                    throw new SourceException( "Unable to select source factory for " + systemID, se );
                } finally {
                    m.release(factory);
                }
            }

            return source;
        }
    }

    /**
     * @see org.apache.excalibur.source.SourceResolver#resolveURI(java.lang.String)
     */
    public Source resolveURI(String location)
    throws MalformedURLException, IOException, SourceException {
        return this.resolveURI(location, null, null);
    }

    /** 
     * Obtain a reference to the SourceResolver with "/Cocoon" hint
     * 
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager manager) throws ServiceException {
        this.manager = manager;
        if ( this.manager.hasService(org.apache.excalibur.source.SourceResolver.ROLE+"/Cocoon")) {
            this.customResolver = (org.apache.excalibur.source.SourceResolver)
                     this.manager.lookup(org.apache.excalibur.source.SourceResolver.ROLE+"/Cocoon");
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
        if ( this.manager != null ) {
            this.manager.release( this.customResolver );
            this.customResolver = null;
            this.manager = null;
        }
    }

    /**
     * Get the service manager.
     */
    protected ServiceManager getServiceManager() {
        ServiceManager m = EnvironmentHelper.getSitemapServiceManager();
        if ( m == null ) {
            m = this.manager;
        }
        return m;
    }

    /**
     * Get the SourceFactory
     */
    protected SourceFactory getSourceFactory(ServiceManager m, String scheme) 
    throws ServiceException {
        return (SourceFactory)m.lookup(SourceFactory.ROLE + '/' + scheme);
    }

    /**
     * @see org.apache.excalibur.source.SourceResolver#release(org.apache.excalibur.source.Source)
     */
    public void release(Source source) {
        if( source == null ) return;

        if ( this.customResolver != null ) {
            this.customResolver.release( source );
        } else {
            final ServiceManager m = this.getServiceManager();
            
            // search for a SourceFactory implementing the protocol
            final String scheme = source.getScheme();
            SourceFactory factory = null;

            try {
                factory = this.getSourceFactory(m, scheme);
                factory.release(source);
            } catch (ServiceException se ) {
                try {
                    factory = this.getSourceFactory(m, "*");
                    factory.release(source);
                } catch (ServiceException sse ) {
                    throw new CascadingRuntimeException( "Unable to select source factory for " + source.getURI(), se );
                }
            } finally {
                m.release( factory );
            }
        }
    }

    /**
     * Makes an absolute URI based on a baseURI and a relative URI.
     */
    protected String absolutize( SourceFactory factory, String baseURI, String systemID )  {
        if( factory instanceof URIAbsolutizer ) {
            systemID = ((URIAbsolutizer)factory).absolutize(baseURI, systemID);
        } else {
            systemID = org.apache.excalibur.source.SourceUtil.absolutize(baseURI, systemID);
        }
        return systemID;
    }

}
