/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.cocoon;

/**
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @version CVS $Revision: 1.1.2.20 $ $Date: 2001-04-26 15:45:01 $
 */

public interface Constants {

    String NAME          = "@name@";
    String VERSION       = "@version@";
    String COMPLETE_NAME = NAME + " " + VERSION;
    String CONF_VERSION  = "2.0";
    String YEAR          = "@year@";

    String RELOAD_PARAM   = "cocoon-reload";
    String SHOWTIME_PARAM = "cocoon-showtime";
    String VIEW_PARAM     = "cocoon-view";
    String ACTION_PARAM   = "cocoon-action";

    String TEMPDIR_PROPERTY    = "org.apache.cocoon.properties.tempdir";
    String DEFAULT_CONTEXT_DIR = "./webapp";
    String DEFAULT_DEST_DIR    = "./site";
    String DEFAULT_WORK_DIR    = "./work";
    String DEFAULT_CONF_FILE   = "cocoon.xconf";

    String PARSER_PROPERTY = "org.apache.cocoon.components.parser.Parser";
    String DEFAULT_PARSER  = "org.apache.cocoon.components.parser.JaxpParser";

    String XSP_PREFIX          = "xsp";
    String XSP_URI             = "http://apache.org/xsp";
    String XSP_REQUEST_PREFIX  = "xsp-request";
    String XSP_REQUEST_URI     = XSP_URI + "/request";
    String XSP_RESPONSE_PREFIX = "xsp-response";
    String XSP_RESPONSE_URI    = XSP_URI + "/response";

    String XML_NAMESPACE_URI = "http://www.w3.org/XML/1998/namespace";

    String LINK_CONTENT_TYPE     = "application/x-cocoon-links";
    String LINK_VIEW             = "links";
    String LINK_CRAWLING_ROLE    = "static";

    String REQUEST_OBJECT  = "request";
    String RESPONSE_OBJECT = "response";
    String CONTEXT_OBJECT  = "context";
    String LINK_OBJECT     = "link";

    String INDEX_URI = "index";

    String ERROR_NAMESPACE_URI = "http://apache.org/cocoon/" + CONF_VERSION + "/error";
    String ERROR_NAMESPACE_PREFIX = "error";

    String CONTEXT_ENVIRONMENT_CONTEXT = "environment-context";
    String CONTEXT_ROOT_PATH       = "root-path";
    String CONTEXT_CLASS_LOADER    = "class-loader";
    String CONTEXT_WORK_DIR        = "work-directory";
    String CONTEXT_UPLOAD_DIR      = "upload-directory";
    String CONTEXT_CLASSPATH       = "classpath";
    String CONTEXT_CONFIG_URL      = "config-url";
    String CONTEXT_LOG_DIR         = "log-directory";
}
