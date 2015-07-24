/**
 * Copyright 2009 Joe LaPenna
 */

package com.joelapenna.foursquared.widget;

import android.content.Context;

import com.joelapenna.foursquare.types.Tip;

/**
 * @author Joe LaPenna (joe@joelapenna.com)
 */
abstract public class BaseTipAdapter extends BaseGroupAdapter<Tip> {

    public BaseTipAdapter(Context context) {
        super(context);
    }
}
