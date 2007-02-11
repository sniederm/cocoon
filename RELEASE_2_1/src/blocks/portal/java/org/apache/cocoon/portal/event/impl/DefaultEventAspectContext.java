/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Apache Cocoon" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 Stefano Mazzocchi  <stefano@apache.org>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.cocoon.portal.event.impl;

import java.util.Iterator;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.portal.PortalService;
import org.apache.cocoon.portal.event.EventConverter;
import org.apache.cocoon.portal.event.Publisher;
import org.apache.cocoon.portal.event.aspect.EventAspect;
import org.apache.cocoon.portal.event.aspect.EventAspectContext;

/**
 *
 * @author <a href="mailto:cziegeler@s-und-n.de">Carsten Ziegeler</a>
 * @author <a href="mailto:volker.schmitt@basf-it-services.com">Volker Schmitt</a>
 * 
 * @version CVS $Id: DefaultEventAspectContext.java,v 1.1 2003/05/22 15:19:43 cziegeler Exp $
 */
public final class DefaultEventAspectContext 
    implements EventAspectContext {

    private EventAspectChain chain;
    
    private Iterator iterator;
    private Iterator configIterator;
    private Parameters config;
    
    private Publisher publisher;
    private Map objectModel;
    private EventConverter converter;

    public DefaultEventAspectContext(EventAspectChain chain) {
        this.chain = chain;
        this.iterator = chain.getIterator();
        this.configIterator = chain.getConfigIterator();
    }
    
	/* (non-Javadoc)
	 * @see org.apache.cocoon.portal.layout.renderer.RendererAspectContext#invokeNext(org.apache.cocoon.portal.layout.Layout, org.apache.cocoon.portal.PortalService, org.xml.sax.ContentHandler)
	 */
	public void invokeNext(PortalService service) {
		if (iterator.hasNext()) {
            this.config = (Parameters) this.configIterator.next();
            final EventAspect aspect = (EventAspect) iterator.next();
            aspect.process( this, service );
		}

	}

	/* (non-Javadoc)
	 * @see org.apache.cocoon.portal.layout.renderer.RendererAspectContext#getConfiguration()
	 */
	public Parameters getAspectParameters() {
		return this.config;
	}

    /**
     * Get the encoder
     */
    public EventConverter getEventConverter(){
        return this.converter;
    }
    
    /**
     * Get the publisher
     */
    public Publisher getEventPublisher(){
        return this.publisher;
    }
    
    /**
     * Get the object model
     */
    public Map getObjectModel() {
        return this.objectModel;
    }

	/**
	 * @param converter
	 */
	public void setEventConverter(EventConverter converter) {
		this.converter = converter;
	}

	/**
	 * @param map
	 */
	public void setObjectModel(Map map) {
		objectModel = map;
	}

	/**
	 * @param publisher
	 */
	public void setEventPublisher(Publisher publisher) {
		this.publisher = publisher;
	}

}
