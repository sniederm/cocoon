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

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 Stefano Mazzocchi  <stefano@apache.org>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.cocoon.samples.xmlform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.avalon.framework.CascadingRuntimeException;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 *
 * A sample domain object used as a Form model.
 * Notice that it has mixed content: 
 * JavaBean properties and 
 * DOM Nodes, which are handled correctly by the
 * framework when referenced via XPath.
 *
 * @version CVS $Id: UserBean.java,v 1.3 2004/03/01 03:50:58 antonio Exp $
 */
public class UserBean
{
  private String fname = "Donald";
  private String lname = "Duck";
  private String email = "donald_duck@disneyland.com";
  private int age = 5;
  private int count = 1;
  private short numInstalls = 1; 
  private String liveUrl = "http://";
  private boolean publish = true;
  private List favorites = new ArrayList();
  private List roles = new ArrayList();
  private String hobbies[];
  private HashMap allHobbies;
  private String notes = "<your notes here>";
  
  private boolean hidden = false; 
  
  private Node system;

  public UserBean ()
  {
    initDomNode();
    initRoles();
    initFavorites();
    initHobbies();
  }

  public String getFirstName() {
    return fname;
  }
  
  public void setFirstName(String newName) {
    fname = newName;
  }

  public String getLastName() {
    return lname;
  }
  
  public void setLastName(String newName) {
    lname = newName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String newEmail) {
    email = newEmail;
  }


  public String getLiveUrl() {
    return liveUrl;
  }

  public void setLiveUrl( String newUrl ) {
    liveUrl = newUrl;
  }

  public int getAge() 
    {
    return age;
    }
  
  public void setAge( int newAge ) 
    {
    age = newAge;
    }
  

  public short getNumber() 
    {
    return numInstalls;
    }
  
  public void setNumber( short num ) 
    {
    numInstalls = num;
    }
  
  public boolean getPublish() 
    {
    return publish;
    }
  
  public void setPublish( boolean newPublish ) 
    {
    publish = newPublish;
    }
  
  
   public Node getSystem() 
      {
      return system;
     }

    public void setSystem( Node newSystem ) 
      {
      system = newSystem;
     }

   public boolean getHidden() 
      {
      return hidden;
     }

    public void setHidden( boolean newHidden ) 
      {
      hidden = newHidden;
     }

  public int getCount() {
    return count;
  }

  public void incrementCount() {
    count++;
  }

  public void initDomNode()
  {
    DOMImplementation impl;
    try
    {
      // Find the implementation
      DocumentBuilderFactory factory 
       = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(false);
      factory.setValidating ( false );
      DocumentBuilder builder = factory.newDocumentBuilder();
      impl = builder.getDOMImplementation();
    }
    catch (Exception ex)
    {
      throw new CascadingRuntimeException("Failed to initialize DOM factory.", ex);
    }

    // initialize system as dom node
    Document doc = impl.createDocument( null, "XMLForm_Wizard_System_Node", null);
    Node rootElement = doc.getDocumentElement();

    Node os = doc.createElement ( "os" );
    Text text = doc.createTextNode( "Linux" );
    os.appendChild(text);
    rootElement.appendChild( os );

    Node processor = doc.createElement ( "processor" );
    text = doc.createTextNode( "p4" );
    processor.appendChild(text);
    rootElement.appendChild( processor );

    Attr ram = doc.createAttribute ( "ram" );
    ram.setValue ( "512" );
    NamedNodeMap nmap = rootElement.getAttributes();
    nmap.setNamedItem ( ram );

    Node servletEngine = doc.createElement ( "servletEngine" );
    text = doc.createTextNode( "Tomcat" );
    servletEngine.appendChild(text);
    rootElement.appendChild( servletEngine );

    Node javaVersion = doc.createElement ( "javaVersion" );
    text = doc.createTextNode( "1.3" );
    javaVersion.appendChild(text);
    rootElement.appendChild( javaVersion );

    system = rootElement;

  }
  
  public List getRole()
  {
    return roles;
  }
  
  public void setRole( List newRoles )
  {
    roles = newRoles;
  }
  
  public String[] getHobby()
  {
    return hobbies;
  }
  
  public void setHobby( String[] newHobbies )
  {
    hobbies = newHobbies;
  }

  public Set getAllHobbies()
  {
    return allHobbies.entrySet();
  }
  
  public List getFavorite()
  {
    return favorites;
  }
  
  public void setFavorite( List newFavorites )
  {
    favorites = newFavorites;
  }
  
  public String getNotes()
  {
    return notes;
  }
  
  public void setNotes( String newNotes )
  {
    notes = newNotes;
  }
  
  public void initRoles()
  {
    roles = new ArrayList();
  }
  
  
  public void initHobbies()
  {
    hobbies = new String[] {"swim", "movies", "ski", "gym", "soccer"};
    
    // initialize the reference list of all hobbies
    allHobbies = new HashMap();
    allHobbies.put( "swim", "Swimming" );
    allHobbies.put( "gym", "Body Building" );
    allHobbies.put( "ski", "Skiing" );
    allHobbies.put( "run", "Running" );
    allHobbies.put( "football", "Football" );
    allHobbies.put( "read", "Reading" );
    allHobbies.put( "write", "Writing" );
    allHobbies.put( "soccer", "Soccer" );
    allHobbies.put( "blog", "Blogging" );
  }
  
  
  public void initFavorites()
  {
    favorites.add( "http://cocoon.apache.org" );
    favorites.add( "http://jakarta.apache.org" );
    favorites.add( "http://www.google.com" );
    favorites.add( "http://www.slashdot.org" );
    favorites.add( "http://www.yahoo.com" );
  }
  
}
