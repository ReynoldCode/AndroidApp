/**
 * Copyright 2008 Joe LaPenna
 */

package com.joelapenna.foursquared.widget;

import android.content.Context;

import com.joelapenna.foursquare.types.User;

/**
 * @author Joe LaPenna (joe@joelapenna.com)
 */
public abstract class BaseUserAdapter extends BaseGroupAdapter<User> {

    public BaseUserAdapter(Context context) {
        super(context);
    }
}
