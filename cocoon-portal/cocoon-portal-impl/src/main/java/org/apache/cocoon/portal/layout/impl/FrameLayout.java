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
package org.apache.cocoon.portal.layout.impl;

import org.apache.cocoon.portal.layout.Layout;
import org.apache.cocoon.portal.layout.LayoutFactory;

/**
 * A frame layout holds a source URI. The URI can be changed dynamically through
 * events. The URI may contain any URI that can be resolved by the Cocoon 
 * {@link org.apache.cocoon.environment.SourceResolver}.
 *
 * @version $Id$
 */
public class FrameLayout extends Layout {

    protected String source;

    /**
     * Create a new frame layout object.
     * Never create a layout object directly. Use the
     * {@link LayoutFactory} instead.
     * @param id The unique identifier of the layout object or null.
     * @param name The name of the layout.
     */
    public FrameLayout(String id, String name) {
        super(id, name);
    }

    /**
     * @return String
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the source.
     * @param source The source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @see java.lang.Object#clone()
     */
    protected Object clone() throws CloneNotSupportedException {
        FrameLayout clone = (FrameLayout)super.clone();

        clone.source = this.source;

        return clone;
    }
}
