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
package org.apache.cocoon.environment.background;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.logger.Logger;
import org.apache.cocoon.Processor;
import org.apache.cocoon.environment.AbstractEnvironment;
import org.apache.cocoon.environment.Context;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.commandline.CommandLineContext;
import org.apache.cocoon.environment.commandline.CommandLineRequest;
import org.apache.cocoon.environment.commandline.CommandLineResponse;
import org.apache.cocoon.util.NullOutputStream;

/**
 * A simple implementation of <code>org.apache.cocoon.environment.Environment</code>
 * for pipeline calls which are not externally triggered.
 * 
 * @author <a href="http://apache.org/~reinhard">Reinhard Poetz</a> 
 * @version CVS $Id: BackgroundEnvironment.java,v 1.7 2004/06/04 11:08:08 cziegeler Exp $
 *
 * @since 2.1.4
 */
public class BackgroundEnvironment extends AbstractEnvironment {
	
	public BackgroundEnvironment(Logger logger, Context ctx) {
		super("", null, null);
		
		this.enableLogging(logger);
		
		this.outputStream = new NullOutputStream();    
     
		// TODO Would special Background*-objects have advantages?
		Request request = new CommandLineRequest(
            this,                  // environment
            "",                    // context path
            "",                    // servlet path
            "",                    // path info
            new HashMap(),         // attributes
            Collections.EMPTY_MAP, // parameters
            Collections.EMPTY_MAP  // headers
        );
		this.objectModel.put(ObjectModelHelper.REQUEST_OBJECT, request);  
		this.objectModel.put(ObjectModelHelper.RESPONSE_OBJECT,
							 new CommandLineResponse());
		this.objectModel.put(ObjectModelHelper.CONTEXT_OBJECT, ctx);
	}
	
	/** Needed by EnvironmentHelper.enterEnvironment */
	public Processor getProcessor() {
		return NullProcessor.INSTANCE;
	}
	
    /**
     * @param uri
     * @param view
     * @param context
     * @param stream
     * @param logger
     */
    public BackgroundEnvironment(String uri, 
                                 String view, 
                                 File context, 
                                 OutputStream stream,
                                 Logger logger) {
            
        super(uri, view);
        this.enableLogging(logger);
        this.outputStream = stream;    
     
        // TODO Would special Background*-objects have advantages?
        Request request = new CommandLineRequest(this, "", uri, null, null, null);
        this.objectModel.put(ObjectModelHelper.REQUEST_OBJECT, request);  
        this.objectModel.put(ObjectModelHelper.RESPONSE_OBJECT,
                             new CommandLineResponse());
        this.objectModel.put(ObjectModelHelper.CONTEXT_OBJECT,
                             new CommandLineContext(context.getAbsolutePath()) );

    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Environment#redirect(java.lang.String, boolean, boolean)
     */
    public void redirect(String newURL, boolean global, boolean permanent) throws IOException {
        
    }

    /**
     * @see org.apache.cocoon.environment.Environment#setContentType(java.lang.String)
     */
    public void setContentType(String mimeType) {
        
    }

    /**
     * @see org.apache.cocoon.environment.Environment#getContentType()
     */
    public String getContentType() {
        return null;
    }

    /**
     * @see org.apache.cocoon.environment.Environment#setContentLength(int)
     */
    public void setContentLength(int length) {
        
    }

    /**
     * Always return false
     * 
     * @see org.apache.cocoon.environment.Environment#isExternal()
     */
    public boolean isExternal() {
        return false;
    }
    
    /** Dumb implementation needed by CocoonComponentManager.enterEnvironment() */
    public static class NullProcessor implements Processor {
    	
    	public static final Processor INSTANCE = new NullProcessor();

		public boolean process(Environment environment) throws Exception {
			throw new UnsupportedOperationException();
		}

		public Map getComponentConfigurations() {
			throw new UnsupportedOperationException();
		}

		public Processor getRootProcessor() {
			throw new UnsupportedOperationException();
		}
       
        public InternalPipelineDescription buildPipeline(Environment environment)
        throws Exception {
            throw new UnsupportedOperationException();
        }

        public String getContext() {
            throw new UnsupportedOperationException();
        }

        public SourceResolver getSourceResolver() {
            throw new UnsupportedOperationException();
        }
    }
}
