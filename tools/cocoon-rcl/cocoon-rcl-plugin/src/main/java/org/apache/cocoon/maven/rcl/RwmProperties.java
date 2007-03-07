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
package org.apache.cocoon.maven.rcl;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.maven.plugin.MojoExecutionException;

public class RwmProperties {

    private static final String COB_INF_DIR = "src/main/resources/COB-INF";
    private static final String BLOCK_CONTEXT_URL_PARAM = "/contextPath";
    private static final String CLASSES_DIR = "%classes-dir"; 
    private static final String EXCLUDE_LIB = "%exclude-lib"; 
    private static final String TARGET_CLASSES_DIR = "target/classes";
    
    private Configuration props;

    public RwmProperties(File propsFile) throws ConfigurationException {
        props = new PropertiesConfiguration(propsFile);
    }
    
    public Set getClassesDirs() throws MojoExecutionException {
        Set returnSet = new HashSet();
        for (Iterator rclIt = props.getKeys(); rclIt.hasNext();) {
            String key = (String) rclIt.next();
            if (key.endsWith(CLASSES_DIR)) {
                String[] values = this.props.getStringArray(key);
                for (int i = 0; i < values.length; i++) {
                    String path = values[i];
                    String url = null;
                    try {
                        url = new File(path).toURL().toExternalForm();
                    } catch (MalformedURLException e) {
                        throw new MojoExecutionException("Can't create URL to  " + path, e);
                    }
                    returnSet.add(url);
                }
            }
        }        
        return returnSet;
    }
    
    public Set getExcludedLibProps() throws MojoExecutionException {
        Set returnSet = new HashSet();
        for (Iterator rclIt = props.getKeys(); rclIt.hasNext();) {
            String key = (String) rclIt.next();
            if (key.endsWith(EXCLUDE_LIB)) {
                String[] values = this.props.getStringArray(key);
                for (int i = 0; i < values.length; i++) {
                    returnSet.add(values[i]);
                }
            }
        }        
        return returnSet;
    }    
    
    public Properties getSpringProperties() throws MojoExecutionException {
        Properties springProps = new Properties();
        for(Iterator rclIt = props.getKeys(); rclIt.hasNext();) {
            String key = (String) rclIt.next();
            
            // a [block-id]/COB-INF property was set explicitly
            if(key.endsWith(BLOCK_CONTEXT_URL_PARAM)) {
                String path = null;
                try {
                    path = new File(this.props.getString(key)).toURL().toExternalForm();
                } catch (MalformedURLException e) {
                    throw new MojoExecutionException("Can't create URL to  " + path, e);
                }            
                springProps.put(key, path);
            }
            
            // a %CLASSES_DIR property --> generate a */COB-INF property out of it
            else if(key.endsWith(CLASSES_DIR) && !CLASSES_DIR.equals(key)) {
                String path = null;
                try {
                    path = new File(this.props.getString(key)).toURL().toExternalForm();
                } catch (MalformedURLException e) {
                    throw new MojoExecutionException("Can't create URL to  " + this.props.getString(key), e);
                }  
                
                if(path.endsWith(TARGET_CLASSES_DIR)) {
                    path = path + "/";
                }
                
                if(!path.endsWith(TARGET_CLASSES_DIR + "/")) {
                    throw new MojoExecutionException("A */" + CLASSES_DIR + 
                            " property can only point to a directory that ends with " + TARGET_CLASSES_DIR + ".");
                }
                
                // path calculation
                if(path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                path = path.substring(0, path.length() - "target/classes".length());
                
                String newKey = key.substring(0, key.length() - CLASSES_DIR.length()) + BLOCK_CONTEXT_URL_PARAM;                
                springProps.put(newKey, path + COB_INF_DIR);
            }
            
            // copy all other properties
            else if(!key.endsWith(CLASSES_DIR) && key.indexOf('/') > -1) {
                springProps.put(key, this.props.getString(key));
            }
            
        } 
        return springProps;
    }    

    public Properties getCocoonProperties() {
        Properties cocoonProps = new Properties();
        for(Iterator rclIt = props.getKeys(); rclIt.hasNext();) {
            String key = (String) rclIt.next();
            if(key.indexOf(CLASSES_DIR) == -1 &&
                    key.indexOf(EXCLUDE_LIB) == -1 &&
                    key.indexOf('/') == -1) {
                cocoonProps.put(key, this.props.getString(key));
            }
        }
        return cocoonProps;
    }
    
}    