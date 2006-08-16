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
package org.apache.cocoon.portal.event.aspect.impl;

import org.apache.cocoon.portal.PortalService;
import org.apache.cocoon.portal.event.Event;
import org.apache.cocoon.portal.event.layout.LayoutInstanceChangeAttributeEvent;
import org.apache.cocoon.portal.om.Layout;
import org.apache.cocoon.portal.om.LayoutFeatures;
import org.apache.cocoon.portal.om.LayoutInstance;
import org.apache.cocoon.portal.om.LinkLayout;

/**
 *
 * @version $Id$
 */
public class LinkEventAspect extends AbstractContentEventAspect {

    protected String getRequestParameterName() {
        // TODO - make this configurable
        return "link";
    }

    protected int getRequiredValueCount() {
        return 3;
    }

    /**
     * @see org.apache.cocoon.portal.event.aspect.impl.AbstractContentEventAspect#publish(PortalService, org.apache.cocoon.portal.om.Layout, java.lang.String[])
     */
    protected void publish(PortalService service,
                           Layout layout,
                           String[] values) {
        if (layout instanceof LinkLayout) {
            LayoutInstance instance;
            instance = LayoutFeatures.getLayoutInstance(service, layout, false);
            if ( instance == null ) {
                Event e = new LayoutInstanceChangeAttributeEvent(instance, "link-layout-key", values[2], true);
                service.getEventManager().send(e);                    
                e = new LayoutInstanceChangeAttributeEvent(instance, "llink-layout-id", values[3], true);
                service.getEventManager().send(e);                    
            }
        } else {
            this.getLogger().warn(
                "the configured layout: "
                    + layout.getType()
                    + " is not a LinkLayout.");
        }
    }
}
