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
package org.apache.cocoon.components.treeprocessor.sitemap;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.cocoon.acting.Action;
import org.apache.cocoon.components.treeprocessor.AbstractParentProcessingNodeBuilder;
import org.apache.cocoon.components.treeprocessor.CategoryNode;
import org.apache.cocoon.components.treeprocessor.CategoryNodeBuilder;
import org.apache.cocoon.components.treeprocessor.LinkedProcessingNodeBuilder;
import org.apache.cocoon.components.treeprocessor.ProcessingNode;
import org.apache.cocoon.components.treeprocessor.variables.VariableResolverFactory;

/**
 *
 * @author <a href="mailto:sylvain@apache.org">Sylvain Wallez</a>
 * @version CVS $Id: ActNodeBuilder.java,v 1.2 2003/07/29 14:29:10 stephan Exp $
 */

public class ActNodeBuilder extends AbstractParentProcessingNodeBuilder
  implements LinkedProcessingNodeBuilder {

    private ActSetNode  actSetNode;
    private String      actSetName;

    public ProcessingNode buildNode(Configuration config) throws Exception {

        // Is it an action-set call ?
        this.actSetName = config.getAttribute("set", null);
        if (actSetName == null) {

            String name = config.getAttribute("name", null);
            String source = config.getAttribute("src", null);
            String type = this.treeBuilder.getTypeForStatement(config, Action.ROLE + "Selector");

            ActTypeNode actTypeNode = new ActTypeNode(
                type,
                VariableResolverFactory.getResolver(source, this.manager),
                name
            );
            this.treeBuilder.setupNode(actTypeNode, config);

            actTypeNode.setChildren(buildChildNodes(config));

            return actTypeNode;

        } else {

            // Action set call
            if (config.getAttribute("src", null) != null) {
                getLogger().warn("The 'src' attribute is ignored for action-set call at " + config.getLocation());
            }
            this.actSetNode = new ActSetNode();
            this.treeBuilder.setupNode(this.actSetNode, config);

            this.actSetNode.setChildren(buildChildNodes(config));

            return this.actSetNode;
        }
    }

    public void linkNode() throws Exception {

        if (this.actSetNode != null) {
            // Link action-set call to the action set
            CategoryNode actionSets = CategoryNodeBuilder.getCategoryNode(this.treeBuilder, "action-sets");

            if (actionSets == null)
                throw new ConfigurationException("This sitemap contains no action sets. Cannot call at " + actSetNode.getLocation());

            ActionSetNode actionSetNode = (ActionSetNode)actionSets.getNodeByName(this.actSetName);

            this.actSetNode.setActionSet(actionSetNode);
        }
    }
}
