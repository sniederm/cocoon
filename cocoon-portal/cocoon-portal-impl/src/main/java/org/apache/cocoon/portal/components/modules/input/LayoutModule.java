/*
 * Copyright 1999-2005 The Apache Software Foundation.
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
package org.apache.cocoon.portal.components.modules.input;

import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.jxpath.JXPathContext;

/**
 * This input module gives access to information stored in a layout object
 * by using JXPathExpressions.
 * The syntax to use is LAYOUT_ID/PATH or LAYOUT_KEY:LAYOUT_ID/PATH
 *
 * @version $Id$
 */
public class LayoutModule 
    extends AbstractModule {

    /**
     * @see org.apache.cocoon.components.modules.input.InputModule#getAttribute(java.lang.String, org.apache.avalon.framework.configuration.Configuration, java.util.Map)
     */
    public Object getAttribute(String name, Configuration modeConf, Map objectModel) 
    throws ConfigurationException {
        int pos = name.indexOf('/');
        String path;
        if ( pos == -1 ) {
            path = null;
        } else {
            path = name.substring(pos + 1);
            name = name.substring(0, pos);
        }
        // is the layout key specified?
        pos = name.indexOf(':');
        String layoutKey = null;
        String layoutId = name;
        if ( pos != -1 ) {
            layoutKey = name.substring(0, pos);
            layoutId = name.substring(pos + 1);
        }

        // get the layout
        final Object layout = portalService.getProfileManager().getPortalLayout(layoutKey, layoutId);
        Object value = layout;
        if ( layout != null && path != null ) {
            final JXPathContext jxpathContext = JXPathContext.newContext(layout);
            value = jxpathContext.getValue(path);
        }
        return value;            
    }
}
