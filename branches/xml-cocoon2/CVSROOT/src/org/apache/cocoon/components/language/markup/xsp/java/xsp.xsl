<?xml version="1.0"?>
<!--
 *****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * _________________________________________________________________________ *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************
-->

<!--
 * @author <a href="mailto:ricardo@apache.org>Ricardo Rocha</a>
 * @version CVS $Revision: 1.1.2.18 $ $Date: 2001-01-02 11:09:51 $
-->

<!-- XSP Core logicsheet for the Java language -->
<xsl:stylesheet
  version="1.0"

  xmlns:xsp="http://apache.org/xsp"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
  <xsl:output method="text"/>

  <xsl:variable name="prefix">xsp</xsl:variable>

  <xsl:template match="/">
    <code xml:space="preserve">
      <xsl:apply-templates select="xsp:page"/>
    </code>
  </xsl:template>

  <xsl:template match="xsp:page">
    package <xsl:value-of select="translate(@file-path, '/', '.')"/>;

    import java.io.*;
    import java.net.*;
    import java.util.*;

    import org.w3c.dom.*;
    import org.xml.sax.*;
    import org.xml.sax.helpers.*;

    import org.apache.avalon.*;
    import org.apache.avalon.utils.*;
    import org.apache.log.*;
    import org.apache.cocoon.*;
    import org.apache.cocoon.generation.*;
    import org.apache.cocoon.components.parser.*;
    import org.apache.cocoon.util.*;

    import org.apache.cocoon.components.language.markup.xsp.XSPGenerator;
    import org.apache.cocoon.components.language.markup.xsp.XSPRequestHelper;
    import org.apache.cocoon.components.language.markup.xsp.XSPResponseHelper;


    /* User Imports */
    <xsl:for-each select="xsp:structure/xsp:include">
      import <xsl:value-of select="."/>;
    </xsl:for-each>

    /**
     * Generated by XSP. Edit at your own risk, :-)
     */
    public class <xsl:value-of select="@file-name"/> extends XSPGenerator {
        Logger log = null;
        Logger cocoonLogger = LogKit.getLoggerFor("cocoon");

        static {
            dateCreated = <xsl:value-of select="@creation-date"/>L;
            dependencies = new File[] {
          <xsl:for-each select="//xsp:dependency">
                  new File("<xsl:value-of select="translate(., '\','/')"/>"),
                </xsl:for-each>
            };
        }

        /* User Class Declarations */
        <xsl:apply-templates select="xsp:logic"/>

        /**
        * Generate XML data.
        */
      public void generateContent() throws SAXException {
        AttributesImpl xspAttr = new AttributesImpl();

        <!-- Process only 1st non-XSP element as generated root -->
        <xsl:call-template name="process-first-element">
          <xsl:with-param
                          name="content"
                          select="*[not(starts-with(name(.), 'xsp:'))]"/>
        </xsl:call-template>
      }

        public void generate() throws SAXException {
            this.contentHandler.startDocument();
            AttributesImpl xspAttr = new AttributesImpl();

            <!-- Generate top-level processing instructions -->
            <xsl:apply-templates select="/processing-instruction()"/>

            <!-- Start top-level namespace prefix mapping -->
            <xsl:for-each select="namespace::*[not(local-name(.) = 'xsp')]">
              this.contentHandler.startPrefixMapping(
                      "<xsl:value-of select="local-name(.)"/>",
                      "<xsl:value-of select="."/>"
                  );
            </xsl:for-each>

        generateContent();

            <!-- End top-level namespace prefix mapping -->
            <xsl:for-each select="namespace::*[not(local-name(.) = 'xsp')]">
              this.contentHandler.endPrefixMapping(
                      "<xsl:value-of select="local-name(.)"/>"
                  );
            </xsl:for-each>

            this.contentHandler.endDocument();
        }
    }
  </xsl:template>

  <xsl:template name="process-first-element">
    <xsl:param name="content"/>
    <xsl:apply-templates select="$content[1]"/>
  </xsl:template>

  <xsl:template match="xsp:element">
    <xsl:variable name="uri">
      <xsl:call-template name="get-parameter">
        <xsl:with-param name="name">uri</xsl:with-param>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="prefix">
      <xsl:call-template name="get-parameter">
        <xsl:with-param name="name">prefix</xsl:with-param>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="name">
      <xsl:call-template name="get-parameter">
        <xsl:with-param name="name">name</xsl:with-param>
        <xsl:with-param name="required">true</xsl:with-param>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="raw-name">
      <xsl:if test="
        ($uri = '&quot;&quot;' and $prefix != '&quot;&quot;') or
        ($uri != '&quot;&quot;' and $prefix = '&quot;&quot;')
      ">
        <xsl:call-template name="error">
          <xsl:with-param name="message">[&lt;xsp:element&gt;]
Either both 'uri' and 'prefix' or none of them must be specified
          </xsl:with-param>
        </xsl:call-template>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="$prefix = '&quot;&quot;'">
          <xsl:copy-of select="$name"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:copy-of select="$prefix"/> + ":" + <xsl:copy-of select="$name"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:for-each select="namespace::*">
      this.contentHandler.startPrefixMapping(
        "<xsl:value-of select="local-name(.)"/>",
        "<xsl:value-of select="."/>"
      );
    </xsl:for-each>

    <xsl:apply-templates select="xsp:attribute"/>

    this.contentHandler.startElement(
      <xsl:copy-of select="$uri"/>,
      <xsl:copy-of select="$name"/>,
      <xsl:copy-of select="$raw-name"/>,
      xspAttr
    );

    xspAttr.clear();

    <xsl:apply-templates select="node()[not(name(.) = 'xsp:attribute')]"/>

    this.contentHandler.endElement(
      <xsl:copy-of select="$uri"/>,
      <xsl:copy-of select="$name"/>,
      <xsl:copy-of select="$raw-name"/>
    );

    <xsl:for-each select="namespace::*">
      this.contentHandler.endPrefixMapping(
        "<xsl:value-of select="local-name(.)"/>"
      );
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="xsp:attribute">
    <xsl:variable name="uri">
      <xsl:call-template name="get-parameter">
        <xsl:with-param name="name">uri</xsl:with-param>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="prefix">
      <xsl:call-template name="get-parameter">
        <xsl:with-param name="name">prefix</xsl:with-param>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="name">
      <xsl:call-template name="get-parameter">
        <xsl:with-param name="name">name</xsl:with-param>
        <xsl:with-param name="required">true</xsl:with-param>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="raw-name">
      <xsl:if test="
        ($uri = '&quot;&quot;' and $prefix != '&quot;&quot;') or
        ($uri != '&quot;&quot;' and $prefix = '&quot;&quot;')
      ">
        <xsl:call-template name="error">
          <xsl:with-param name="message">[&lt;xsp:attribute&gt;]
Either both 'uri' and 'prefix' or none of them must be specified
          </xsl:with-param>
        </xsl:call-template>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="$prefix = '&quot;&quot;'">
          <xsl:copy-of select="$name"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:copy-of select="$prefix"/> + ":" + <xsl:copy-of select="$name"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="content">
      <xsl:for-each select="text()|xsp:expr|xsp:text">
        <xsl:choose>
          <xsl:when test="name(.) = 'xsp:expr'">
            String.valueOf(<xsl:value-of select="."/>)
          </xsl:when>
          <xsl:otherwise>
            "<xsl:value-of select="."/>"
          </xsl:otherwise>
        </xsl:choose>
        +
      </xsl:for-each>
      ""
    </xsl:variable>

    xspAttr.addAttribute(
      <xsl:copy-of select="$uri"/>,
      <xsl:copy-of select="$name"/>,
      <xsl:copy-of select="$raw-name"/>,
      "CDATA",
      <xsl:value-of select="normalize-space($content)"/>
    );
  </xsl:template>

  <xsl:template match="xsp:expr">
    <xsl:choose>
      <xsl:when test="starts-with(name(..), 'xsp:') and name(..) != 'xsp:content' and name(..) != 'xsp:element'">
        <!--
             Expression is nested inside another XSP tag:
             preserve its Java type
        -->
        (<xsl:value-of select="."/>)
      </xsl:when>
      <xsl:otherwise>
        <!-- Coerce to String and output as character data -->
        this.characters(String.valueOf(<xsl:value-of select="."/>));
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- FIXME: Is this _really_ necessary? -->
  <xsl:template match="xsp:content">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="xsp:logic">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="xsp:pi">
    <xsl:variable name="target">
      <xsl:call-template name="get-parameter">
        <xsl:with-param name="name">target</xsl:with-param>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="content">
      <xsl:for-each select="text()|xsp:expr">
        <xsl:choose>
          <xsl:when test="name(.) = 'xsp:expr'">
           String.valueOf(<xsl:value-of select="."/>)
          </xsl:when>
          <xsl:otherwise>
            "<xsl:value-of select="."/>"
          </xsl:otherwise>
        </xsl:choose>
       +
      </xsl:for-each>
      ""
    </xsl:variable>

    this.contentHandler.processingInstruction(
      <xsl:copy-of select="$target"/>,
      <xsl:value-of select="normalize-space($content)"/>
    );
  </xsl:template>

  <!-- FIXME: How to create comments in SAX? -->
  <xsl:template match="xsp:comment">
    this.comment("<xsl:value-of select="."/>");
  </xsl:template>


  <xsl:template match="*[not(starts-with(name(.), 'xsp:'))]">
    <xsl:for-each select="namespace::*">
      this.contentHandler.startPrefixMapping(
        "<xsl:value-of select="local-name(.)"/>",
        "<xsl:value-of select="."/>"
      );
    </xsl:for-each>

    <xsl:apply-templates select="@*"/>

    <xsl:apply-templates select="xsp:attribute"/>

    this.contentHandler.startElement(
      "<xsl:value-of select="namespace-uri(.)"/>",
      "<xsl:value-of select="local-name(.)"/>",
      "<xsl:value-of select="name(.)"/>",
      xspAttr
    );

    xspAttr.clear();

    <xsl:apply-templates select="node()[not(name(.) = 'xsp:attribute')]"/>

    this.contentHandler.endElement(
      "<xsl:value-of select="namespace-uri(.)"/>",
      "<xsl:value-of select="local-name(.)"/>",
      "<xsl:value-of select="name(.)"/>"
    );

    <xsl:for-each select="namespace::*">
      this.contentHandler.endPrefixMapping(
        "<xsl:value-of select="local-name(.)"/>"
      );
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="@*">
    xspAttr.addAttribute(
      "<xsl:value-of select="namespace-uri(.)"/>",
      "<xsl:value-of select="local-name(.)"/>",
      "<xsl:value-of select="name(.)"/>",
      "CDATA",
      "<xsl:value-of select="."/>"
    );
  </xsl:template>

  <xsl:template match="text()">
    <xsl:choose>
      <xsl:when test="name(..) = 'xsp:logic' or name(..) = 'xsp:expr'">
        <xsl:value-of select="."/>
      </xsl:when>
      <xsl:otherwise>
        this.characters("<xsl:value-of select="."/>");
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="text()" mode="value">
    <xsl:value-of select="."/>
  </xsl:template>

  <xsl:template match="processing-instruction()">
    this.contentHandler.processingInstruction(
      "<xsl:value-of select="name()"/>",
      "<xsl:value-of select="."/>"
    );
  </xsl:template>

  <!-- Utility templates -->
  <xsl:template name="get-parameter">
    <xsl:param name="name"/>
    <xsl:param name="default"/>
    <xsl:param name="required">false</xsl:param>

    <xsl:variable name="qname">
      <xsl:value-of select="concat($prefix, ':param')"/>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="@*[name(.) = $name]">"<xsl:value-of select="@*[name(.) = $name]"/>"</xsl:when>
      <xsl:when test="(*[name(.) = $qname])[@name = $name]">
        <xsl:call-template name="get-nested-content">
          <xsl:with-param name="content"
                          select="(*[name(.) = $qname])[@name = $name]"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="string-length($default) = 0">
            <xsl:choose>
              <xsl:when test="$required = 'true'">
                <xsl:call-template name="error">
                  <xsl:with-param name="message">[Logicsheet processor]
Parameter '<xsl:value-of select="$name"/>' missing in dynamic tag &lt;<xsl:value-of select="name(.)"/>&gt;
                  </xsl:with-param>
                </xsl:call-template>
              </xsl:when>
              <xsl:otherwise>""</xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise><xsl:copy-of select="$default"/></xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="get-nested-content">
    <xsl:param name="content"/>
    <xsl:choose>
      <xsl:when test="$content/*">
        <xsl:apply-templates select="$content/*"/>
      </xsl:when>
      <xsl:otherwise>"<xsl:value-of select="$content"/>"</xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="error">
    <xsl:param name="message"/>
    <xsl:message terminate="yes"><xsl:value-of select="$message"/></xsl:message>
  </xsl:template>

  <!-- Ignored elements -->
  <xsl:template match="xsp:logicsheet|xsp:dependency|xsp:param"/>
</xsl:stylesheet>
