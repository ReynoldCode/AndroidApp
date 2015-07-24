/**
 * Copyright 2009 Joe LaPenna
 */

package com.joelapenna.foursquared.widget;

import android.content.Context;

import com.joelapenna.foursquare.types.Badge;

/**
 * @author Joe LaPenna (joe@joelapenna.com)
 */
abstract public class BaseBadgeAdapter extends BaseGroupAdapter<Badge> {

    public BaseBadgeAdapter(Context context) {
        super(context);
    }
}
