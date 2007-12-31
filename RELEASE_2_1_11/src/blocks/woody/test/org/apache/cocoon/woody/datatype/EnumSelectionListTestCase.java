/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cocoon.woody.datatype;

import java.io.Writer;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.cocoon.core.container.ContainerTestCase;
import org.apache.cocoon.transformation.I18nTransformer;
import org.apache.cocoon.woody.Constants;
import org.apache.cocoon.woody.datatype.typeimpl.EnumType;
import org.apache.cocoon.xml.dom.DOMBuilder;
import org.apache.excalibur.source.impl.ResourceSource;
import org.custommonkey.xmlunit.Diff;
import org.w3c.dom.Document;

/**
 * Test case for Woody's DynamicSelectionList datatype.
 * @version CVS $Id$
 */
public class EnumSelectionListTestCase extends ContainerTestCase {

    protected DatatypeManager datatypeManager;
    protected DocumentBuilder parser;

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        datatypeManager = (DatatypeManager) this.lookup(DatatypeManager.ROLE);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        parser = factory.newDocumentBuilder();
    }
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        if (datatypeManager != null) {
            this.release(datatypeManager);
        }
        super.tearDown();
    }
    
    /**
     * Test the generateSaxFragment method.
     * @throws MalformedURLException
     * @throws ParserConfigurationException
     */
    public void testGenerateSaxFragment() throws Exception {
        DOMBuilder dest = new DOMBuilder();
        EnumSelectionList list = 
            new EnumSelectionList(Sex.class.getName(), new EnumType(), false);
        list.generateSaxFragment(dest, Locale.ENGLISH);
        ResourceSource expectedSource =
            new ResourceSource("resource://org/apache/cocoon/woody/datatype/EnumSelectionListTestCase.dest-no-null.xml");
        Document expected = parser.parse(expectedSource.getInputStream());
        assertEqual("Test if output is what is expected",
                expected, dest.getDocument());
    }
    
    /**
     * Test the generateSaxFragment method with a nullable selection list
     * @throws MalformedURLException
     * @throws ParserConfigurationException
     */
    public void testGenerateSaxFragmentNullable() throws Exception {
        DOMBuilder dest = new DOMBuilder();
        EnumSelectionList list = 
            new EnumSelectionList(Sex.class.getName(), new EnumType(), true);
        list.generateSaxFragment(dest, Locale.ENGLISH);
        ResourceSource expectedSource =
            new ResourceSource("resource://org/apache/cocoon/woody/datatype/EnumSelectionListTestCase.dest.xml");
        Document expected = parser.parse(expectedSource.getInputStream());
        assertEqual("Test if output is what is expected",
                expected, dest.getDocument());
    }
    
    /**
     * Check is the source document is equal to the one produced by the method under test.
     * @param message A message to print in case of failure.
     * @param expected The expected (source) document.
     * @param actual The actual (output) document.
     */
    private void assertEqual(String message, Document expected, Document actual) {
        expected.getDocumentElement().normalize();
        actual.getDocumentElement().normalize();
        // DIRTY HACK WARNING: we add the "xmlns:*" attributes reported
        // by DOM, as expected, but not generated by the method under test,
        // otherwise the comparison would fail. 
        actual.getDocumentElement().setAttribute(Constants.WI_PREFIX,
                Constants.WI_NS);
        actual.getDocumentElement().setAttribute("i18n",
                I18nTransformer.I18N_NAMESPACE_URI);
        Diff diff =  new Diff(expected, actual);
        assertTrue(message + ", " + diff.toString(), diff.similar());
    }

    /**
     * Print a document to a writer for debugging purposes.
     * @param document The document to print.
     * @param out The writer to write to.
     */
    public final void print(Document document, Writer out) {
        TransformerFactory factory = TransformerFactory.newInstance();
        try {
            javax.xml.transform.Transformer serializer =
                factory.newTransformer();
            serializer.transform(
                new DOMSource(document),
                new StreamResult(out));
            out.write('\n');
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
