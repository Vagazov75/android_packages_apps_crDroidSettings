/*
 * Copyright (C) 2016-2018 crDroid Android Project
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
package com.crdroid.settings.fragments.about.update.xml;

import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

import com.crdroid.settings.R;

public class OTAParser {

    private static final String ns = null;
    private static final String FILENAME_TAG = "filename";
    private static final String MAINTAINER_TAG = "maintainer";
    private static final String CHANGELOG_TAG = "changelog";
    private static final String DOWNLOAD_TAG = "download";
    private static final String GAPPS_TAG = "gapps";
    private static final String FORUM_TAG = "forum";
    private static final String URL_TAG = "url";

    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String URL = "url";

    private String mDeviceName = null;
    private OTADevice mDevice = null;

    private static OTAParser mInstance;
    private static Context mContext;

    private OTAParser() {
    }

    public static OTAParser getInstance(Context context) {
        mContext = context;
        if (mInstance == null) {
            mInstance = new OTAParser();
        }
        return mInstance;
    }

    public OTADevice parse(InputStream in, String deviceName) throws XmlPullParserException, IOException {
        this.mDeviceName = deviceName;

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            startParsing(parser);
            return mDevice;
        } finally {
            in.close();
        }
    }

    private void startParsing(XmlPullParser parser) throws XmlPullParserException, IOException {
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            readManufacturer(parser);
        }
    }

    private void readManufacturer(XmlPullParser parser) throws XmlPullParserException, IOException {
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equalsIgnoreCase(mDeviceName)) {
                readDevice(parser);
            } else {
                skip(parser);
            }
        }
    }

    private void readDevice(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, mDeviceName);
        mDevice = new OTADevice();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equalsIgnoreCase(FILENAME_TAG)) {
                String tagValue = readTag(parser, tagName);
                mDevice.setLatestVersion(tagValue);
            } else if (tagName.equalsIgnoreCase(MAINTAINER_TAG)) {
                String tagValue = readTag(parser, tagName);
                mDevice.setMaintainer(tagValue);
            } else if (mContext != null && (tagName.equalsIgnoreCase(CHANGELOG_TAG) ||
                    tagName.equalsIgnoreCase(DOWNLOAD_TAG) ||
                    tagName.equalsIgnoreCase(GAPPS_TAG) ||
                    tagName.equalsIgnoreCase(FORUM_TAG))) {
                OTALink link = readLink(parser, tagName);
                mDevice.addLink(link);
            } else if (isUrlTag(tagName)) {
                OTALink link = readLink(parser, tagName);
                mDevice.addLink(link);
            } else {
                skip(parser);
            }
        }
    }

    private OTALink readLink(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, tag);

        String id = parser.getAttributeValue(null, ID);
        if (id == null || id.isEmpty()) {
            id = tag;
        }
        OTALink link = new OTALink(id);
        String title, description;
        if (tag.equalsIgnoreCase(CHANGELOG_TAG)) {
            title = mContext.getResources().getString(R.string.changelog_title);
            description = mContext.getResources().getString(R.string.changelog_summary);
        } else if (tag.equalsIgnoreCase(DOWNLOAD_TAG)) {
            title = mContext.getResources().getString(R.string.download_title);
            description = mContext.getResources().getString(R.string.download_summary);
        } else if (tag.equalsIgnoreCase(GAPPS_TAG)) {
            title = mContext.getResources().getString(R.string.gapps_title);
            description = mContext.getResources().getString(R.string.gapps_summary);
        } else if (tag.equalsIgnoreCase(FORUM_TAG)) {
            title = mContext.getResources().getString(R.string.forum_title);
            description = mContext.getResources().getString(R.string.forum_summary);
        } else {
            title = parser.getAttributeValue(null, TITLE);
            description = parser.getAttributeValue(null, DESCRIPTION);
        }
        link.setTitle(title);
        link.setDescription(description);
        String url = readText(parser);
        link.setUrl(url);

        parser.require(XmlPullParser.END_TAG, ns, tag);
        return link;
    }

    private String readTag(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tag);
        String text = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, tag);
        return text;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private static boolean isUrlTag(String tagName) {
        return tagName.toLowerCase().endsWith(URL_TAG.toLowerCase());
    }
}
