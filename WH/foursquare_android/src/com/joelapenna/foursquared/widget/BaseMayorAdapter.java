/**
 * Copyright 2008 Joe LaPenna
 */

package com.joelapenna.foursquared.widget;

import android.content.Context;

import com.joelapenna.foursquare.types.Mayor;

/**
 * @author Joe LaPenna (joe@joelapenna.com)
 */
public abstract class BaseMayorAdapter extends BaseGroupAdapter<Mayor> {

    public BaseMayorAdapter(Context context) {
        super(context);
    }
}
