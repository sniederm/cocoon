/*-- $Id: Configurations.java,v 1.8 2000-07-06 03:40:11 balld Exp $ --

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) @year@ The Apache Software Foundation. All rights reserved.

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

 4. The names "Cocoon" and  "Apache Software Foundation"  must not be used to
    endorse  or promote  products derived  from this  software without  prior
    written permission. For written permission, please contact
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
package org.apache.cocoon.framework;

import java.util.*;
import java.io.*;

/**
 * This class encapsulates all the configurations needed by a Configurable
 * class to work.
 *
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @version $Revision: 1.8 $ $Date: 2000-07-06 03:40:11 $
 */

public class Configurations {

    protected String baseName;
	protected Properties properties;

    public Configurations() {
		properties = new Properties();
    }

    /**
     * Create the class from a the file
     */
    public Configurations(String file) throws Exception {
        this(file, null);
    }

    /**
     * Create the class with given defaults and from the file
     */
    public Configurations(String file, Configurations defaults) throws Exception {
		if (defaults != null) {
        	properties = new Properties(defaults.properties);
		} else {
			properties = new Properties();
		}
        InputStream input = new FileInputStream(file);
        properties.load(input);
        input.close();
    }

    /**                                                                  
     * Create the class from a the InputStream                           
     */                                                                  
    public Configurations(InputStream stream) throws Exception {         
        this(stream, null);                                              
    }                                                                    
                                                                         
    /**
     * Create the class with given defaults and from the URL resource
     */
    public Configurations(InputStream stream, Configurations defaults) throws Exception {
        if (defaults != null) {
            properties = new Properties(defaults.properties);
        } else {
            properties = new Properties();
        }
        properties.load(stream);
    }

    /**
     * Create the class with given defaults.
     */
    public Configurations(Configurations c) {
		properties = new Properties(c.properties);
    }

    /**
     * Set the configuration.
     */
    public void set(String key, Object value) {
        properties.put(key, value);
    }

	/**
	 * The JavaModule interpreter is using this method. should it?
	 */
	public void put(String key, Object value) {
		properties.put(key,value);
	}

    /**
     * Get the configuration.
     */
    public Object get(String key) {
        return properties.get(key);
    }

    /**
     * Get the configuration and use the given default value if not found.
     */
    public Object get(String key, Object def) {
        Object o = properties.get(key);
        return (o == null) ? def : o;
    }

    /**
     * Get the configuration, throw an exception if not present.
     */
    public Object getNotNull(String key) {
        Object o = properties.get(key);
        if (o == null) {
            throw new RuntimeException("Cocoon configuration item '" + ((baseName == null) ? "" : baseName + "." + key) + "' is not set");
        } else {
            return o;
        }
    }

    /**
     * Get a vector of configurations when the syntax is incremental
     */
    public Vector getVector(String key) {
        Vector v = new Vector();

        for (int i = 0; ; i++) {
            Object n = get(key + "." + i);
            if (n == null) break;
            v.addElement(n);
        }

        return v;
    }

    /**
     * Create a subconfiguration starting from the base node.
     */
    public Configurations getConfigurations(String base) {
        Configurations c = new Configurations();
        c.setBasename((baseName == null) ? base : baseName + "." + base);
        String prefix = base + ".";

        Enumeration keys = properties.propertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();

            if (key.startsWith(prefix)) {
                c.set(key.substring(prefix.length()), this.get(key));
            } else if (key.equals(base)) {
                c.set("", this.get(key));
            }
        }

        return c;
    }

    /**
     * Get the Properties from the Configuration
     */
    public Properties getProperties() {
        return this.properties;
    }
	
    public void setBasename(String baseName) {
        this.baseName = baseName;
    }

	/**
	 * Used by XSPProcessor, but this maybe should be getKeys()?
	 */
	public Enumeration keys() {
		return properties.keys();
	}
}
