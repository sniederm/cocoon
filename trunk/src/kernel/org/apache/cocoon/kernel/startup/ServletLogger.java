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
package org.apache.cocoon.kernel.startup;

import javax.servlet.ServletContext;

import org.apache.cocoon.kernel.logging.Logger;

/**
 * <p>The {@link ServletLogger} is a simple {@link Logger} implementation
 * logging individual lines to a {@link ServletContext}.</p>
 *
 * @author <a href="mailto:pier@apache.org">Pier Fumagalli</a>
 * @version 1.0 (CVS $Revision: 1.2 $)
 */
public class ServletLogger extends AbstractLogger {

    /** <p>Our {@link ServletContext} instance.</p> */
    private ServletContext context = null;
    
    /**
     * <p>Create a new {@link ServletLogger} associated with a specific
     * {@link ServletContext}.</p>
     *
     * @param context the {@link ServletContext} to log to.
     */
    public ServletLogger(ServletContext context) {
        this(DEBUG, true, context);
    }
    
    /**
     * <p>Create a new {@link ServletLogger} associated with a specific
     * {@link ServletContext}.</p>
     *
     * @param level the level of output.
     * @param trace if <b>true</b> exception stack traces will be produced.
     * @param context the {@link ServletContext} to log to.
     */
    public ServletLogger(int level, boolean trace, ServletContext context) {
        super(null, level, false, trace);

        if (context == null) throw new NullPointerException("Null context");
        this.context = context;
    }
    
    /* ====================================================================== */

    /**
     * <p>Write a line to the output.</p>
     *
     * @param line the line to write.
     */
    public void output(String line) {
        this.context.log(line);
    }
}
