/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquare.parsers;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.error.FoursquareError;
import com.joelapenna.foursquare.error.FoursquareParseException;
import com.joelapenna.foursquare.types.Category;

/**
 * @date March 6, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 * @param <T>
 */
public class CategoryParser extends AbstractParser<Category> {
    private static final Logger LOG = Logger.getLogger(StatsParser.class.getCanonicalName());
    private static final boolean DEBUG = Foursquare.PARSER_DEBUG;

    @Override
    public Category parseInner(XmlPullParser parser) throws XmlPullParserException, IOException,
            FoursquareError, FoursquareParseException {
        parser.require(XmlPullParser.START_TAG, null, null);

        Category category = new Category();

        while (parser.nextTag() == XmlPullParser.START_TAG) {
            String name = parser.getName();
            if ("id".equals(name)) {
                category.setId(parser.nextText());

            } else if ("fullpathname".equals(name)) {
                category.setFullPathName(parser.nextText());

            } else if ("nodename".equals(name)) {
                category.setNodeName(parser.nextText());

            } else if ("iconurl".equals(name)) {
                category.setIconUrl(parser.nextText());

            } else if ("categories".equals(name)) {
                category.setChildCategories(new GroupParser(new CategoryParser()).parse(parser));

            } else {
                // Consume something we don't understand.
                if (DEBUG) LOG.log(Level.FINE, "Found tag that we don't recognize: " + name);
                skipSubTree(parser);
            }
        }
        return category;
    }
}
