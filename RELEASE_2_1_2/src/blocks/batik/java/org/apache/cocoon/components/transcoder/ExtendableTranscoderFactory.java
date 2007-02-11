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
package org.apache.cocoon.components.transcoder;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.image.TIFFTranscoder;

import java.util.HashMap;
import java.util.Map;

/**
 * An extendable Batik Transcoder factory.
 * When given a MIME type, find a Transcoder which supports that MIME
 * type. This factory is extendable as new <code>Transcoder</code>s can
 * be added at runtime.
 *
 * @author <a href="mailto:rossb@apache.org">Ross Burton</a>
 * @version CVS $Id: ExtendableTranscoderFactory.java,v 1.1 2003/03/09 00:02:37 pier Exp $
 */
public class ExtendableTranscoderFactory implements TranscoderFactory {

    protected static Map transcoders = new HashMap();

    protected final static TranscoderFactory singleton = new ExtendableTranscoderFactory();

    private ExtendableTranscoderFactory() {
        // Add the default transcoders which come with Batik
        addTranscoder("image/jpeg", JPEGTranscoder.class);
        addTranscoder("image/jpg", JPEGTranscoder.class);
        addTranscoder("image/png", PNGTranscoder.class);
        addTranscoder("image/tiff", TIFFTranscoder.class);
    }

    /**
     * Get a reference to this Transcoder Factory.
     */
    public final static TranscoderFactory getTranscoderFactoryImplementation() {
        return singleton;
    }

    /**
     * Create a transcoder for a specified MIME type.
     * @param mimeType The MIME type of the destination format
     * @return A suitable transcoder, or <code>null</code> if one cannot be found
     */
    public Transcoder createTranscoder(String mimeType) {
        Class transcoderClass = (Class) transcoders.get(mimeType);
        if (transcoderClass == null) {
            return null;
        } else {
            try {
                return (Transcoder) transcoderClass.newInstance();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * Add a mapping from the specified MIME type to a transcoder.
     * Note: The transcoder must have a no-argument constructor.
     * @param mimeType The MIME type of the Transcoder
     * @param transcoderClass The <code>Class</code> object for the Transcoder.
     */
    public void addTranscoder(String mimeType, Class transcoderClass) {
        transcoders.put(mimeType, transcoderClass);
    }

    /**
     * Remove the mapping from a specified MIME type.
     * @param mimeType The MIME type to remove from the mapping.
     */
    public void removeTranscoder(String mimeType) {
        transcoders.remove(mimeType);
    }
}