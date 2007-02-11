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
package org.apache.cocoon.portal.layout.renderer.aspect.impl;

import org.apache.cocoon.portal.PortalService;
import org.apache.cocoon.portal.coplet.CopletInstanceData;
import org.apache.cocoon.portal.coplet.status.SizingStatus;
import org.apache.cocoon.portal.event.impl.ChangeCopletInstanceAspectDataEvent;
import org.apache.cocoon.portal.layout.Layout;
import org.apache.cocoon.portal.layout.impl.CopletLayout;
import org.apache.cocoon.portal.layout.renderer.aspect.RendererAspectContext;
import org.apache.cocoon.xml.XMLUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * This renderer aspect tests, if a coplet is sizable and/or maxpageable.
 * 
 * TODO: make the names of the aspects to test configurable
 *
 * @author <a href="mailto:cziegeler@s-und-n.de">Carsten Ziegeler</a>
 * @author <a href="mailto:volker.schmitt@basf-it-services.com">Volker Schmitt</a>
 * 
 * @version CVS $Id: SizingAspect.java,v 1.6 2003/09/02 08:55:26 cziegeler Exp $
 */
public class SizingAspect extends AbstractAspect {

	/* (non-Javadoc)
	 * @see org.apache.cocoon.portal.layout.renderer.RendererAspect#toSAX(org.apache.cocoon.portal.layout.renderer.RendererAspectContext, org.apache.cocoon.portal.layout.Layout, org.apache.cocoon.portal.PortalService, org.xml.sax.ContentHandler)
	 */
	public void toSAX(RendererAspectContext context,
                		Layout layout,
                		PortalService service,
                		ContentHandler handler)
	throws SAXException {
        
        CopletInstanceData cid = ((CopletLayout)layout).getCopletInstanceData();

        boolean showContent = true;
        
        boolean sizable = ((Boolean)cid.getCopletData().getAspectData("sizable")).booleanValue();
        Integer size = null;
        
        if ( sizable ) {
            size = (Integer)cid.getAspectData("size");
            if ( size == null ) {
                size = SizingStatus.STATUS_MAXIMIZED;
            }

            ChangeCopletInstanceAspectDataEvent event;    

            if ( size.equals(SizingStatus.STATUS_MAXIMIZED) ) {
                event = new ChangeCopletInstanceAspectDataEvent(cid, "size", SizingStatus.STATUS_MINIMIZED);
                XMLUtils.createElement(handler, "minimize-uri", service.getComponentManager().getLinkService().getLinkURI(event));
            }

            if ( size.equals(SizingStatus.STATUS_MINIMIZED)) {
                event = new ChangeCopletInstanceAspectDataEvent(cid, "size", SizingStatus.STATUS_MAXIMIZED);
                XMLUtils.createElement(handler, "maximize-uri", service.getComponentManager().getLinkService().getLinkURI(event));
            }
            
            if (size == SizingStatus.STATUS_MINIMIZED) {
                showContent = false;
            }
        } 
/*        boolean maxPageable = ((Boolean)cid.getCopletData().getAspectData("maxpageable")).booleanValue();
        if ( maxPageable ) {
            if ( size == null ) {
                size = (Integer)cid.getAspectData("size");
                if ( size == null ) {
                    size = SizingStatus.STATUS_MAXIMIZED;
                }
            }
            ChangeCopletInstanceAspectDataEvent event;    

            if ( size == SizingStatus.STATUS_MAXIMIZED) {
                event = new ChangeCopletInstanceAspectDataEvent(cid, "size", SizingStatus.STATUS_MAXPAGED);
                XMLUtils.createElement(handler, "maxpage-uri", service.getComponentManager().getLinkService().getLinkURI(event));
            }
            if ( size == SizingStatus.STATUS_MAXPAGED) {
                event = new ChangeCopletInstanceAspectDataEvent(cid, "size", SizingStatus.STATUS_MAXIMIZED);
                XMLUtils.createElement(handler, "minpage-uri", service.getComponentManager().getLinkService().getLinkURI(event));
            }
        }
*/               
        if ( showContent ) {
            context.invokeNext(layout, service, handler);
        }
	}

}
