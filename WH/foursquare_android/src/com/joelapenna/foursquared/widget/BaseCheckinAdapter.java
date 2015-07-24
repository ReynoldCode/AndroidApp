/**
 * Copyright 2008 Joe LaPenna
 */

package com.joelapenna.foursquared.widget;

import android.content.Context;

import com.joelapenna.foursquare.types.Checkin;

/**
 * @author Joe LaPenna (joe@joelapenna.com)
 */
public abstract class BaseCheckinAdapter extends BaseGroupAdapter<Checkin> {

    public BaseCheckinAdapter(Context context) {
        super(context);
    }
}
