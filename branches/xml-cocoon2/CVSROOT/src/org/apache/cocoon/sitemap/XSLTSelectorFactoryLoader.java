/***************************************************************************** 
 * Copyright (C) The Apache Software Foundation. All rights reserved.        * 
 * ------------------------------------------------------------------------- * 
 * This software is published under the terms of the Apache Software License * 
 * version 1.1, a copy of which has been included  with this distribution in * 
 * the LICENSE file.                                                         * 
 *****************************************************************************/ 
package org.apache.cocoon.sitemap; 

import org.apache.cocoon.selection.SelectorFactory;

import org.w3c.dom.DocumentFragment;
  
/** 
 * This class is used as a XSLT extension class. It is used by the sitemap 
 * generation stylesheet to load a <code>SelectorFactory</code> to get the 
 * generated source code.
 * 
 * @author <a href="mailto:Giacomo.Pati@pwr.ch">Giacomo Pati</a> 
 * @version CVS $Revision: 1.1.2.2 $ $Date: 2000-07-17 21:06:14 $ 
 */ 

public class XSLTSelectorFactoryLoader {

    public String getSource (String selectorFactoryClassname, String test, DocumentFragment conf) 
    throws ClassNotFoundException, InstantiationException, IllegalAccessException, Exception {
        Class cl = this.getClass().getClassLoader().loadClass(selectorFactoryClassname);
        SelectorFactory factory = (SelectorFactory) cl.newInstance();
        return factory.generate (test, conf);
    }
}
