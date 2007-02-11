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
package org.apache.cocoon.forms.datatype.convertor;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.apache.cocoon.forms.Constants;
import org.apache.cocoon.xml.AttributesImpl;

import java.util.Locale;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * A Convertor for {@link java.util.Date Date} objects backed by the
 * {@link java.text.SimpleDateFormat SimpleDateFormat} class.
 *
 * <p>It can be configured to use one of three <strong>variants</strong>: date,
 * time or datetime and one of four <strong>styles</strong>: long, full, medium or short.
 *
 * <p>Alternatively, a <strong>formatting pattern</strong> can be used. This can either be a locale-dependent
 * or locale-independent formatting pattern. When looking up a formatting pattern, a mechansim
 * similar to resource bundle lookup is used. Suppose the locale is nl-BE, then first a formatting
 * pattern for nl-BE will be sought, then one for nl, and if that is not
 * found, finally the locale-independent formatting pattern will be used.
 *
 * @version $Id$
 */
public class FormattingDateConvertor implements Convertor {
    /** See {@link #setStyle}. */
    private int style;
    /** See {@link #setVariant}. */
    private String variant;
    /** Locale-specific formatting patterns. */
    private LocaleMap localizedPatterns;
    /** Non-locale specific formatting pattern. */
    private String nonLocalizedPattern;

    public static final String DATE = "date";
    public static final String TIME = "time";
    public static final String DATE_TIME = "datetime";

    public FormattingDateConvertor() {
        this.style = DateFormat.SHORT;
        this.variant = DATE;
        this.localizedPatterns = new LocaleMap();
    }

    public ConversionResult convertFromString(String value, Locale locale, Convertor.FormatCache formatCache) {
        SimpleDateFormat dateFormat = getDateFormat(locale, formatCache);
        try {
            return new ConversionResult(dateFormat.parse(value));
        } catch (ParseException e) {
            return ConversionResult.create("date." + this.variant);
        }
    }

    public String convertToString(Object value, Locale locale, Convertor.FormatCache formatCache) {
        SimpleDateFormat dateFormat = getDateFormat(locale, formatCache);
        return dateFormat.format((Date)value);
    }

    private final SimpleDateFormat getDateFormat(Locale locale, Convertor.FormatCache formatCache) {
        SimpleDateFormat dateFormat = null;
        if (formatCache != null)
            dateFormat = (SimpleDateFormat)formatCache.get();
        if (dateFormat == null) {
            dateFormat = getDateFormat(locale);
            if (formatCache != null)
                formatCache.store(dateFormat);
        }
        return dateFormat;
    }

    protected SimpleDateFormat getDateFormat(Locale locale) {
        SimpleDateFormat dateFormat = null;

        if (this.variant.equals(DATE)) {
            //dateFormat = I18nSupport.getInstance().getDateFormat(style, locale);
            dateFormat = (SimpleDateFormat)DateFormat.getDateInstance(style, locale);
        } else if (this.variant.equals(TIME)) {
            //dateFormat = I18nSupport.getInstance().getTimeFormat(style, locale);
            dateFormat = (SimpleDateFormat)DateFormat.getTimeInstance(style, locale);
        } else if (this.variant.equals(DATE_TIME)) {
            //dateFormat = I18nSupport.getInstance().getDateTimeFormat(style, style, locale);
            dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance(style, style, locale);
        }

        String pattern = (String)localizedPatterns.get(locale);

        if (pattern != null)
            // Note: this was previously using applyLocalizedPattern() which allows to use
            // a locale-specific pattern syntax, e.g. in french "j" (jour) for "d" and
            // "a" (annee) for "y". But the localized pattern syntax is very little known and thus
            // led to some weird pattern syntax error messages.
            dateFormat.applyPattern(pattern);
        else if (nonLocalizedPattern != null)
            dateFormat.applyPattern(nonLocalizedPattern);

        return dateFormat;
    }

    public Class getTypeClass() {
        return Date.class;
    }

    /**
     *
     * @param style one of the constants FULL, LONG, MEDIUM or SHORT defined in the {@link Date} class.
     */
    public void setStyle(int style) {
        this.style = style;
    }

    public void setVariant(String variant) {
        if (DATE.equals(variant) || TIME.equals(variant) || DATE_TIME.equals(variant)) {
            this.variant = variant;
        } else {
            throw new IllegalArgumentException("Invalid value for variant parameter.");
        }
    }

    public void addFormattingPattern(Locale locale, String pattern) {
        localizedPatterns.put(locale, pattern);
    }

    public void setNonLocalizedPattern(String pattern) {
        this.nonLocalizedPattern = pattern;
    }

    private static final String CONVERTOR_EL = "convertor";

    public void generateSaxFragment(ContentHandler contentHandler, Locale locale) throws SAXException {
        String pattern = getDateFormat(locale).toPattern();

        if (pattern != null) {
            AttributesImpl attrs = new AttributesImpl();
            attrs.addCDATAAttribute("pattern", pattern);
            attrs.addCDATAAttribute("variant", this.variant);
            contentHandler.startElement(Constants.INSTANCE_NS, CONVERTOR_EL, Constants.INSTANCE_PREFIX_COLON + CONVERTOR_EL, attrs);
            contentHandler.endElement(Constants.INSTANCE_NS, CONVERTOR_EL, Constants.INSTANCE_PREFIX_COLON + CONVERTOR_EL);
        }
    }
}