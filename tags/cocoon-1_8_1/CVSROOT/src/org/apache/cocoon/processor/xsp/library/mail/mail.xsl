<?xml version="1.0"?>
<!--
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
-->
<!--
 <description>
 This is the stylesheet to replace elements in the mail namespace with
 calls to the java mail API to retrieve and display mail from a server.
 </description>

 <author>Donald A. Ball Jr.</author>
 <author>Ugo Cei</author>
 <version>1.2</version>
-->
<xsl:stylesheet
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:xsp="http://www.apache.org/1999/XSP/Core"
 xmlns:mail="http://apache.org/cocoon/mail/v1"
 version="1.0"
>

<xsl:template match="xsp:page">
 <xsl:copy>
  <xsl:apply-templates select="@*"/>
  <xsp:structure>
   <xsp:include>javax.mail.Session</xsp:include>
   <xsp:include>javax.mail.Store</xsp:include>
   <xsp:include>javax.mail.Folder</xsp:include>
   <xsp:include>javax.mail.Message</xsp:include>
   <xsp:include>javax.mail.Address</xsp:include>
   <xsp:include>javax.mail.Part</xsp:include>
   <xsp:include>javax.mail.Multipart</xsp:include>
   <xsp:include>javax.mail.Flags</xsp:include>
   <xsp:include>javax.mail.internet.MimeMessage</xsp:include>
   <xsp:include>javax.mail.internet.MimePart</xsp:include>
   <xsp:include>javax.mail.internet.InternetAddress</xsp:include>
   <xsp:include>java.text.DateFormat</xsp:include>
  </xsp:structure>
  <xsl:apply-templates/>
  <xsp:logic>
   void _mail_process_content(
    HttpServletRequest request,
    HttpServletResponse response,
    Document document,
    Node xspParentNode,
    Node xspCurrentNode,
    Stack xspNodeStack,
    HttpSession session,
    MimePart part) throws Exception {
  if (part.isMimeType("text/plain")) {
   <xsp:content><xsp:expr>(String)part.getContent()</xsp:expr></xsp:content>
  } else if (part.isMimeType("message/rfc822")) {
   <xsp:content><rfc822><xsp:logic>
   _mail_process_content(request,response,document,xspParentNode,xspCurrentNode,xspNodeStack,session,(MimePart)part.getContent());
   </xsp:logic></rfc822></xsp:content>
  } else if (part.isMimeType("multipart/*")) {
   Multipart multipart = (Multipart)part.getContent();
   int count = multipart.getCount();
   for (int i=0; i&lt;count; i++) {
    <xsp:content><part><xsp:logic>
    _mail_process_content(request,response,document,xspParentNode,xspCurrentNode,xspNodeStack,session,(MimePart)multipart.getBodyPart(i));
    </xsp:logic></part></xsp:content>
   }
  }
    }
  </xsp:logic>
 </xsl:copy>
</xsl:template>

<xsl:template match="mail:execute-query">
 <xsl:variable name="protocol">
  <xsl:call-template name="get-nested-string">
   <xsl:with-param name="content" select="mail:protocol"/>
  </xsl:call-template>
 </xsl:variable>
 <xsl:variable name="host">
  <xsl:call-template name="get-nested-string">
   <xsl:with-param name="content" select="mail:host"/>
  </xsl:call-template>
 </xsl:variable>
 <xsl:variable name="port">    <!-- POP3 is conventionally on port 110 -->
  <xsl:call-template name="get-nested-string">
   <xsl:with-param name="content" select="mail:port"/>
  </xsl:call-template>
 </xsl:variable>
 <xsl:variable name="username">
  <xsl:call-template name="get-nested-string">
   <xsl:with-param name="content" select="mail:username"/>
  </xsl:call-template>
 </xsl:variable>
 <xsl:variable name="password">
  <xsl:call-template name="get-nested-string">
   <xsl:with-param name="content" select="mail:password"/>
  </xsl:call-template>
 </xsl:variable>
 <xsl:variable name="mbox">
  <xsl:call-template name="get-nested-string">
   <xsl:with-param name="content" select="mail:mbox"/>
  </xsl:call-template>
 </xsl:variable>
 <xsp:logic>
  {
   Properties _mail_properties = new Properties();
   Session _mail_session = Session.getDefaultInstance(_mail_properties,null);
   <xsl:choose>
    <xsl:when test="$protocol">
     Store _mail_store = _mail_session.getStore(String.valueOf(<xsl:copy-of select="$protocol"/>));
    </xsl:when>
    <xsl:otherwise>
     Store _mail_store = _mail_session.getStore();
    </xsl:otherwise>
   </xsl:choose>
   Integer _mail_port = null;
   try {
    _mail_port = new Integer(String.valueOf(<xsl:copy-of select="$port"/>));
   } catch (Exception e) {}
   _mail_store.connect(
    String.valueOf(<xsl:copy-of select="$host"/>),
    _mail_port == null ? -1 : _mail_port.intValue(),
    String.valueOf(<xsl:copy-of select="$username"/>),
    String.valueOf(<xsl:copy-of select="$password"/>));
   Folder _mail_folder = _mail_store.getDefaultFolder();
   _mail_folder = _mail_folder.getFolder(String.valueOf(<xsl:copy-of select="$mbox"/>));
   _mail_folder.open(Folder.READ_WRITE);
   <xsl:apply-templates select="mail:results/*"/>
   _mail_folder.close(false);
   _mail_store.close();
  }
 </xsp:logic>
</xsl:template>

<xsl:template match="mail:results//mail:get-message-count">
 <xsp:expr>_mail_folder.getMessageCount()</xsp:expr>
</xsl:template>

<xsl:template match="mail:results//mail:get-messages">
  <xsl:variable name="start">
    <xsl:call-template name="get-nested-string">
      <xsl:with-param name="content" select="mail:limit-messages/mail:start-index"/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="end">
    <xsl:call-template name="get-nested-string">
      <xsl:with-param name="content" select="mail:limit-messages/mail:end-index"/>
    </xsl:call-template>
  </xsl:variable>

  <xsp:logic>
  {
    int _mail_start = 1;
    int _mail_end = 10; // _mail_folder.getMessageCount();
    try {
      _mail_start = Integer.parseInt(<xsl:copy-of select="$start"/>); 
    } catch (NumberFormatException _mail_exception) {}
    if (_mail_start &lt; 1) {
      _mail_start = 1;
    }
    try {
      _mail_end = Integer.parseInt(<xsl:copy-of select="$end"/>); 
    } catch (NumberFormatException _mail_exception) {}
    for (int _mail_i = _mail_start ; 
         _mail_i &lt;= _mail_end &amp;&amp; _mail_i &lt;= _mail_folder.getMessageCount() ;
         ++_mail_i) {
      <message>
        <xsp:attribute name="number"><xsp:expr>_mail_i</xsp:expr></xsp:attribute>
        <xsp:logic>
          MimeMessage _mail_message = (MimeMessage) _mail_folder.getMessage(_mail_i);
          if (_mail_message != null) {
            <xsl:apply-templates/>
	  }
        </xsp:logic>
      </message>
    }
  }
  </xsp:logic>
</xsl:template>

<xsl:template match="mail:results//mail:get-messages//mail:get-subject">
 <xsp:expr>_mail_message.getSubject()</xsp:expr>
</xsl:template>

<xsl:template match="mail:results//mail:get-messages//mail:get-sent-date">
 <xsp:expr>DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(_mail_message.getSentDate())</xsp:expr>
</xsl:template>

<xsl:template match="mail:results//mail:get-messages//mail:get-from">
 <xsp:logic>
  {
   Address _mail_from[] = _mail_message.getFrom();
   for (int _mail_j=0; _mail_j &lt; _mail_from.length; _mail_j++) {
     if (_mail_from[_mail_j] instanceof InternetAddress) {
       InternetAddress _mail_ia = (InternetAddress) _mail_from[_mail_j];
       <internet-address>
         <address><xsp:expr>_mail_ia.getAddress()</xsp:expr></address>
         <personal><xsp:expr>_mail_ia.getPersonal()</xsp:expr></personal>
       </internet-address>
     }
     <address><xsp:expr>_mail_from[_mail_j].toString()</xsp:expr></address>
   }
  }
 </xsp:logic>
</xsl:template>

<xsl:template match="mail:results//mail:get-messages//mail:get-recipients">
 <xsp:logic>
  {
   String _mail_type = "<xsl:value-of select="@type"/>";
   Message.RecipientType _mail_recipient_type = null;
   if ("TO".equals(_mail_type)) {
    _mail_recipient_type = Message.RecipientType.TO;
   } else if ("CC".equals(_mail_type)) {
    _mail_recipient_type = Message.RecipientType.CC;
   } else if ("BCC".equals(_mail_type)) {
    _mail_recipient_type = Message.RecipientType.BCC;
   }
   Address _mail_to[];
   if (null == _mail_recipient_type) {
    _mail_to = _mail_message.getAllRecipients();
   } else {
    _mail_to = _mail_message.getRecipients(_mail_recipient_type);
   }
   for (int _mail_j=0; _mail_j &lt; _mail_to.length; _mail_j++) {
    <address><xsp:expr>_mail_to[_mail_j].toString()</xsp:expr></address>
   }
  }
 </xsp:logic>
</xsl:template>

<xsl:template match="mail:results//mail:get-messages//mail:get-flags">
 <xsp:logic>
  {
    if (_mail_message.isSet(Flags.Flag.ANSWERED)) {
      <flag>answered</flag>
    }
    if (_mail_message.isSet(Flags.Flag.DELETED)) {
      <flag>deleted</flag>
    }
    if (_mail_message.isSet(Flags.Flag.DRAFT)) {
      <flag>draft</flag>
    }
    if (_mail_message.isSet(Flags.Flag.FLAGGED)) {
      <flag>flagged</flag>
    }
    if (_mail_message.isSet(Flags.Flag.RECENT)) {
      <flag>recent</flag>
    }
    if (_mail_message.isSet(Flags.Flag.SEEN)) {
      <flag>seen</flag>
    }
  }
 </xsp:logic>
</xsl:template>

<xsl:template match="mail:results//mail:get-messages//mail:get-all-headers">
 <xsp:logic>
 {
  Enumeration _mail_enum = _mail_message.getAllHeaderLines();
  while (_mail_enum.hasMoreElements()) {
   <header><xsp:expr>(String)_mail_enum.nextElement()</xsp:expr></header>
  }
 }
 </xsp:logic>
</xsl:template>

<xsl:template match="mail:results//mail:get-messages//mail:get-content">
 <xsp:logic>
  _mail_process_content(request,response,document,xspParentNode,xspCurrentNode,xspNodeStack,session,_mail_message);
 </xsp:logic>
</xsl:template>

<xsl:template match="@*|node()" priority="-1">
 <xsl:copy>
  <xsl:apply-templates select="@*|node()"/>
 </xsl:copy>
</xsl:template>

  <xsl:template name="get-nested-string">
  	<xsl:param name="content"/>
	<xsl:choose>
		<xsl:when test="$content/*">
			""
			<xsl:for-each select="$content/node()">
				<xsl:choose>
					<xsl:when test="name(.)">
						+ <xsl:apply-templates select="."/>
					</xsl:when>
					<xsl:otherwise>
						+ "<xsl:value-of select="translate(.,'&#9;&#10;&#13;','   ')"/>"
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>
		</xsl:when>
		<xsl:otherwise>"<xsl:value-of select="normalize-space($content)"/>"</xsl:otherwise>
	</xsl:choose>
</xsl:template>

</xsl:stylesheet>
