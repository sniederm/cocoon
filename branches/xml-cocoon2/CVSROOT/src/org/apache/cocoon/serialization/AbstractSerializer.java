/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
 
package org.apache.cocoon.serialization;

import java.io.OutputStream;

import org.apache.cocoon.xml.AbstractXMLPipe;

/**
 * @author <a href="mailto:fumagalli@exoffice.com">Pierpaolo Fumagalli</a>
 *         (Apache Software Foundation, Exoffice Technologies)
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @version CVS $Revision: 1.1.2.8 $ $Date: 2000-09-27 16:15:33 $
 */
 
public abstract class AbstractSerializer extends AbstractXMLPipe implements Serializer {

    /**
     * The <code>OutputStream</code> used by this serializer.
     */    
    protected OutputStream output;

    /**
     * Set the <code>OutputStream</code> where the XML should be serialized.
     */
    public void setOutputStream(OutputStream out) {
        this.output = out;
    }

    /**
     * Get the mime-type of the output of this <code>Serializer</code>
     * This default implementation returns null to indicate that the 
     * mime-type specified in the sitemap is to be used
     */
    public String getMimeType() {
        return null;
    }
}
