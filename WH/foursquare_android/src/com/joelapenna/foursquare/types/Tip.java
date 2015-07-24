/**
 * Copyright 2009 Joe LaPenna
 */

package com.joelapenna.foursquare.types;

import android.os.Parcel;
import android.os.Parcelable;

import com.joelapenna.foursquare.util.ParcelUtils;

/**
 * Auto-generated: 2009-11-12 21:45:35.385718
 *
 * @author Joe LaPenna (joe@joelapenna.com)
 * @author Mark Wyszomierski (markww@gmail.com), implemented Parcelable.
 */
public class Tip implements FoursquareType, Parcelable {

    private String mCreated;
    private String mDistance;
    private String mId;
    private String mText;
    private User mUser;
    private Venue mVenue;

    public Tip() {
    }

    private Tip(Parcel in) {
        mCreated = ParcelUtils.readStringFromParcel(in);
        mDistance = ParcelUtils.readStringFromParcel(in);
        mId = ParcelUtils.readStringFromParcel(in);
        mText = ParcelUtils.readStringFromParcel(in);
        
        if (in.readInt() == 1) {
            mUser = in.readParcelable(User.class.getClassLoader());
        }
        
        if (in.readInt() == 1) {
            mUser = in.readParcelable(Venue.class.getClassLoader());
        }
    }
    
    public static final Parcelable.Creator<Tip> CREATOR = new Parcelable.Creator<Tip>() {
        public Tip createFromParcel(Parcel in) {
            return new Tip(in);
        }

        @Override
        public Tip[] newArray(int size) {
            return new Tip[size];
        }
    };
    
    public String getCreated() {
        return mCreated;
    }

    public void setCreated(String created) {
        mCreated = created;
    }

    public String getDistance() {
        return mDistance;
    }

    public void setDistance(String distance) {
        mDistance = distance;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public User getUser() {
        return mUser;
    }

    public void setUser(User user) {
        mUser = user;
    }

    public Venue getVenue() {
        return mVenue;
    }

    public void setVenue(Venue venue) {
        mVenue = venue;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        ParcelUtils.writeStringToParcel(out, mCreated);
        ParcelUtils.writeStringToParcel(out, mDistance);
        ParcelUtils.writeStringToParcel(out, mId);
        ParcelUtils.writeStringToParcel(out, mText);
        
        if (mUser != null) {
            out.writeInt(1); 
            out.writeParcelable(mUser, flags);
        } else {
            out.writeInt(0);
        }
         
        if (mVenue != null) {
            out.writeInt(1);
            out.writeParcelable(mVenue, flags);
        } else {
            out.writeInt(0);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
