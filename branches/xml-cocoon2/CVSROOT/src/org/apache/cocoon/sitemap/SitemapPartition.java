/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.sitemap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.arch.Component;
import org.apache.arch.ComponentManager;
import org.apache.arch.Composer;
import org.apache.arch.config.Configurable;
import org.apache.arch.config.Configuration;
import org.apache.arch.config.ConfigurationException;
import org.apache.cocoon.Processor;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.Request;
import org.apache.cocoon.Response;
import org.xml.sax.SAXException;

/**
 *
 * @author <a href="mailto:fumagalli@exoffice.com">Pierpaolo Fumagalli</a>
 *         (Apache Software Foundation, Exoffice Technologies)
 * @version CVS $Revision: 1.1.2.5 $ $Date: 2000-02-28 18:43:21 $
 */
public class SitemapPartition
implements Composer, Configurable, Processor, LinkResolver {

    /** The list of perocessors */
    private Vector processors=new Vector();
    /** The component manager instance */
    private ComponentManager manager=null;
    /** The sitemap */
    protected Sitemap sitemap=null;
    /** This partition name */
    protected String name=null;
    

    /**
     * Create a new <code>SitemapPartition</code> instance.
     */
    public SitemapPartition(Sitemap sitemap) {
        super();
        this.sitemap=sitemap;
    }

    /**
     * Set the current <code>ComponentManager</code> instance used by this
     * <code>Composer</code>.
     */
    public void setComponentManager(ComponentManager manager) {
        this.manager=manager;
    }

    /**
     * Pass a <code>Configuration</code> instance to this
     * <code>Configurable</code> class.
     */
    public void setConfiguration(Configuration conf)
    throws ConfigurationException {
        if (!conf.getName().equals("partition"))
            throw new ConfigurationException("Invalid partition configuration",
                                             conf);
        this.name=conf.getAttribute("name","default");
        // Set components
        Enumeration e=conf.getConfigurations("process");
        while (e.hasMoreElements()) {
            Configuration co=(Configuration)e.nextElement();
            GenericProcessor p=new GenericProcessor(this);
            p.setComponentManager(this.manager);
            p.setConfiguration(co);
            this.processors.addElement(p);
        }
        e=conf.getConfigurations("resource");
        while (e.hasMoreElements()) {
            Configuration co=(Configuration)e.nextElement();
            ResourceProcessor p=new ResourceProcessor();
            p.setComponentManager(this.manager);
            p.setConfiguration(co);
            this.processors.addElement(p);
        }
        if (this.processors.size()==0)
            throw new ConfigurationException("No processors configured in "+
                                            "partition \""+this.name+"\"",conf);
    }

    /**
     * Process the given <code>Request</code> producing the output to the
     * specified <code>Response</code> and <code>OutputStream</code>.
     */
    public boolean process(Request req, Response res, OutputStream out)
    throws SAXException, IOException, ProcessingException {
        Enumeration e=this.processors.elements();
        while (e.hasMoreElements()) {
            Processor p=(Processor)e.nextElement();
            if (p.process(req,res,out)) return(true);
        }
        return(false);
    }

    /**
     * Resolve a link against a source into the target URI space.
     */
    public String resolve(String source, String partition) {
        if (source==null) return(null);
        if (this.name.equals(partition)) {
            Enumeration e=this.processors.elements();
            while (e.hasMoreElements()) {
                LinkResolver r=(LinkResolver)e.nextElement();
                String translated=r.resolve(source,partition);
                if (translated!=null) return(translated);
            }
        }
        return(null);
    }

}
