/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquared.widget;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.FoursquaredSettings;
import com.joelapenna.foursquare.types.Group;
import com.joelapenna.foursquare.types.User;
import com.joelapenna.foursquared.R;
import com.joelapenna.foursquared.util.RemoteResourceManager;

/**
 * @date March 8, 2010
 * @author Mark Wyszomierski (markww@gmail.com), foursquare.
 */
public class FriendListAdapter extends BaseGroupAdapter<User> 
    implements ObservableAdapter {

    private static final String TAG = "";
    private static final boolean DEBUG = FoursquaredSettings.DEBUG;

    private LayoutInflater mInflater;
    private int mLayoutToInflate;
    private RemoteResourceManager mRrm;
    private RemoteResourceManagerObserver mResourcesObserver;
    private Handler mHandler = new Handler();
    private int mLoadedPhotoIndex;

    
    public FriendListAdapter(Context context, RemoteResourceManager rrm) {
        super(context);
        mInflater = LayoutInflater.from(context);
        mLayoutToInflate = R.layout.friend_list_item;
        mRrm = rrm;
        mResourcesObserver = new RemoteResourceManagerObserver();
        mLoadedPhotoIndex = 0;

        mRrm.addObserver(mResourcesObserver);
    }
    
    public void removeObserver() {
        mHandler.removeCallbacks(mRunnableLoadPhotos);
        mRrm.deleteObserver(mResourcesObserver);
    }

    public FriendListAdapter(Context context, int layoutResource) {
        super(context);
        mInflater = LayoutInflater.from(context);
        mLayoutToInflate = layoutResource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unnecessary
        // calls to findViewById() on each row.
        ViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no
        // need to re-inflate it. We only inflate a new View when the
        // convertView supplied by ListView is null.
        if (convertView == null) {
            convertView = mInflater.inflate(mLayoutToInflate, null);

            // Creates a ViewHolder and store references to the two children
            // views we want to bind data to.
            holder = new ViewHolder();
            holder.photo = (ImageView) convertView.findViewById(R.id.friendListItemPhoto);
            holder.name = (TextView) convertView.findViewById(R.id.friendListItemName);

            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }

        User user = (User) getItem(position);
        Uri photoUri = Uri.parse(user.getPhoto());
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(mRrm.getInputStream(photoUri));
            holder.photo.setImageBitmap(bitmap);
        } catch (IOException e) {
            if (Foursquare.MALE.equals(user.getGender())) {
                holder.photo.setImageResource(R.drawable.blank_boy);
            } else {
                holder.photo.setImageResource(R.drawable.blank_girl);
            }
        }

        holder.name.setText(user.getFirstname() + " "
                + (user.getLastname() != null ? user.getLastname() : ""));

        return convertView;
    }

    public void removeItem(int position) throws IndexOutOfBoundsException {
        group.remove(position);
        notifyDataSetInvalidated();
    }
    
    @Override
    public void setGroup(Group<User> g) {
        super.setGroup(g);
        mLoadedPhotoIndex = 0;
        
        mHandler.postDelayed(mRunnableLoadPhotos, 10L);
        
//        for (int i = 0; i < g.size(); i++) {
//            Uri photoUri = Uri.parse(g.get(i).getPhoto());
//            if (!mRrm.exists(photoUri)) {
//                mRrm.request(photoUri);
//            }
//        }
    }

    private class RemoteResourceManagerObserver implements Observer {
        @Override
        public void update(Observable observable, Object data) {
            if (DEBUG) Log.d(TAG, "Fetcher got: " + data);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }
    
    private Runnable mRunnableLoadPhotos = new Runnable() {
        @Override
        public void run() {
            if (mLoadedPhotoIndex < getCount()) {
                User user = (User)getItem(mLoadedPhotoIndex++);
                Uri photoUri = Uri.parse(user.getPhoto());
                if (!mRrm.exists(photoUri)) {
                    mRrm.request(photoUri);
                }
                mHandler.postDelayed(mRunnableLoadPhotos, 200L);
            }
        }
    };

    static class ViewHolder {
        ImageView photo;
        TextView name;
    }
}
