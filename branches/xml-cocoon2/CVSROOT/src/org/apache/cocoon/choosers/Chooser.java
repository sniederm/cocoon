/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.choosers;

import org.apache.cocoon.sitemap.SitemapComponent;
import org.apache.cocoon.Request;

/**
 *
 * @author <a href="mailto:Giacomo.Pati@pwr.ch">Giacomo Pati</a>
 * @version CVS $Revision: 1.1.2.1 $ $Date: 2000-07-02 13:24:44 $
 */
public interface Chooser extends SitemapComponent {
    /**
     * Choosers test pattern against some <code>Request</code> values
     * and signals success with the returned boolean value
     * @param expression The expression to test.
     * @param request    The <code>Request</code> object which can be used
     *                   to select values to test the expression.
     * @return boolean   Signals successfull test.
     */
    public boolean choose (String expression, Request request);
}


