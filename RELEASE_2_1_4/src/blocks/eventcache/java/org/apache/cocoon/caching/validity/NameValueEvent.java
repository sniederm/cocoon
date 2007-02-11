/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

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

*/
package org.apache.cocoon.caching.validity;

/**
 * An external uncache event that consists of a name/value pair.  
 * An example might be "table_name", "primary_key"
 * 
 * @author Geoff Howard (ghoward@apache.org)
 * @version $Id: NameValueEvent.java,v 1.3 2003/11/21 15:41:09 unico Exp $
 */
public class NameValueEvent extends Event {

    private String m_name;
    private String m_value;
    private int m_hashcode;
    
    /**
     * Constructor requires two Strings - the name/value 
     * pair which defines this Event.
     * 
     * @param name
     * @param value
     */
	public NameValueEvent(String name, String value) {
        m_name = name;
        m_value = value;
        m_hashcode = (name + value).hashCode();
	}
    
    /**
     * Must return true when both name and value are 
     * equivalent Strings.
     */
	public boolean equals(Event e) {
		if (e instanceof NameValueEvent) {
            NameValueEvent nve = (NameValueEvent)e;
            return ( m_name.equals(nve.m_name) && 
                m_value.equals(nve.m_value) );
		}
		return false;
	}
    
    public int hashCode() {
        return m_hashcode;
    }
    
    public String toString() {
        return "NameValueEvent[" + m_name + "," + m_value + "]";
    }
}