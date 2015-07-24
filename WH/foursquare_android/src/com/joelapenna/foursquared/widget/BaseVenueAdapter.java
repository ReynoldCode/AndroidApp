/**
 * Copyright 2008 Joe LaPenna
 */

package com.joelapenna.foursquared.widget;

import android.content.Context;

import com.joelapenna.foursquare.types.Venue;

/**
 * @author Joe LaPenna (joe@joelapenna.com)
 */
abstract public class BaseVenueAdapter extends BaseGroupAdapter<Venue> {

    public BaseVenueAdapter(Context context) {
        super(context);
    }
}
