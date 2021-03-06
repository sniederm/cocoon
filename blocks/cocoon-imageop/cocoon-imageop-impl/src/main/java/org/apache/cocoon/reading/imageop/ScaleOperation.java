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
package org.apache.cocoon.reading.imageop;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.WritableRaster;
import org.apache.avalon.framework.parameters.Parameters;

public class ScaleOperation
    implements ImageOperation {

    private String  prefix;
    private boolean enabled;
    private float   scale;

    public void setPrefix( String prefix ) {
        this.prefix = prefix;
    }

    public void setup( Parameters params ) {
        enabled = params.getParameterAsBoolean( prefix + "enabled", true);
        scale = params.getParameterAsFloat( prefix + "scale", 1.0f );
    }

    public WritableRaster apply( WritableRaster image ) {
        if( ! enabled ) {
            return image;
        }
        AffineTransform scale = AffineTransform.getScaleInstance( this.scale, this.scale );
        AffineTransformOp op = new AffineTransformOp( scale, AffineTransformOp.TYPE_BILINEAR );
        WritableRaster scaledRaster = op.filter( image, null );
        return scaledRaster;
    }

    public String getKey() {
        return "scale:" 
               + ( enabled ? "enable" : "disable" )
               + ":" + scale
               + ":" + prefix;
    }
} 
