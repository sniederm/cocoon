/***************************************************************************** 
 * Copyright (C) The Apache Software Foundation. All rights reserved.        * 
 * ------------------------------------------------------------------------- * 
 * This software is published under the terms of the Apache Software License * 
 * version 1.1, a copy of which has been included  with this distribution in * 
 * the LICENSE file.                                                         * 
 *****************************************************************************/ 
package org.apache.cocoon.selection; 

import org.w3c.dom.DocumentFragment;
 
/** 
 * This class generates source code which tests a specific browser pattern
 * agains the requesting user-agent
 * 
 * @author <a href="mailto:Giacomo.Pati@pwr.ch">Giacomo Pati</a> 
 * @version CVS $Revision: 1.1.2.2 $ $Date: 2000-07-17 21:06:12 $ 
 */ 

public class BrowserSelectorFactory implements SelectorFactory {
    public String generate (String test_expression, DocumentFragment conf) {
        return "return true;";
    }
}
