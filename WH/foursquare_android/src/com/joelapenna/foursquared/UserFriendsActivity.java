/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquared;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.FoursquaredSettings;
import com.joelapenna.foursquare.types.Group;
import com.joelapenna.foursquare.types.User;
import com.joelapenna.foursquared.app.LoadableListActivity;
import com.joelapenna.foursquared.location.LocationUtils;
import com.joelapenna.foursquared.util.NotificationsUtil;
import com.joelapenna.foursquared.widget.FriendListAdapter;

/**
 * Shows a list of friends for the user id passed as an intent extra.
 * 
 * @date March 9, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 */
public class UserFriendsActivity extends LoadableListActivity {
    static final String TAG = "UserFriendsActivity";
    static final boolean DEBUG = FoursquaredSettings.DEBUG;
    
    public static final String EXTRA_USER_ID = Foursquared.PACKAGE_NAME
        + ".UserFriendsActivity.EXTRA_USER_ID";

    public static final String EXTRA_SHOW_ADD_FRIEND_OPTIONS = Foursquared.PACKAGE_NAME
        + ".UserFriendsActivity.EXTRA_SHOW_ADD_FRIEND_OPTIONS";

    private StateHolder mStateHolder;
    private FriendListAdapter mListAdapter;

    
    private BroadcastReceiver mLoggedOutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "onReceive: " + intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(mLoggedOutReceiver, new IntentFilter(Foursquared.INTENT_ACTION_LOGGED_OUT));

        Object retained = getLastNonConfigurationInstance();
        if (retained != null && retained instanceof StateHolder) {
            mStateHolder = (StateHolder) retained;
            mStateHolder.setActivityForTaskFriends(this);
        } else {

            mStateHolder = new StateHolder();
            if (getIntent().getExtras().containsKey(EXTRA_USER_ID) == false) {
                Log.e(TAG, "UserFriendsActivity requires a userid in its intent extras.");
                finish();
                return;
            }
            
            mStateHolder.setUserId(getIntent().getExtras().getString(EXTRA_USER_ID));
            mStateHolder.setShowAddFriendOptions(getIntent().getBooleanExtra(
                    EXTRA_SHOW_ADD_FRIEND_OPTIONS, false));
            mStateHolder.startTaskFriends(this, mStateHolder.getUserId());
        }
        
        ensureUi();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        if (isFinishing()) {
            mStateHolder.cancelTasks();
            mListAdapter.removeObserver();
            unregisterReceiver(mLoggedOutReceiver);
        }
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        mStateHolder.setActivityForTaskFriends(null);
        return mStateHolder;
    }

    private void ensureUi() {
        mListAdapter = new FriendListAdapter(this, 
            ((Foursquared) getApplication()).getRemoteResourceManager());
        mListAdapter.setGroup(mStateHolder.getFriends());
        
        ListView listView = getListView();
        listView.setAdapter(mListAdapter);
        listView.setSmoothScrollbarEnabled(true);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                User user = (User) parent.getAdapter().getItem(position);
                Intent intent = new Intent(UserFriendsActivity.this, UserDetailsActivity.class);
                intent.putExtra(UserDetailsActivity.EXTRA_USER_PARCEL, user);
                intent.putExtra(UserDetailsActivity.EXTRA_SHOW_ADD_FRIEND_OPTIONS, 
                    mStateHolder.getShowAddFriendOptions());
                startActivity(intent);
            }
        });
        
        if (mStateHolder.getIsRunningFriendsTask()) {
            setLoadingView();
        } else if (mStateHolder.getFetchedFriendsOnce() && mStateHolder.getFriends().size() == 0) {
            setEmptyView();
        }
    }

    private void onFriendsTaskComplete(Group<User> group, Exception ex) {
        mListAdapter.removeObserver();
        mListAdapter = new FriendListAdapter(this, 
            ((Foursquared) getApplication()).getRemoteResourceManager());
        if (group != null) {
            mStateHolder.setFriends(group);
            mListAdapter.setGroup(mStateHolder.getFriends());
            getListView().setAdapter(mListAdapter);
        }
        else {
            mStateHolder.setFriends(new Group<User>());
            mListAdapter.setGroup(mStateHolder.getFriends());
            getListView().setAdapter(mListAdapter);
            
            NotificationsUtil.ToastReasonForFailure(this, ex);
        }
        mStateHolder.setIsRunningFriendsTask(false);
        mStateHolder.setFetchedFriendsOnce(true);
        
        // TODO: We can probably tighten this up by just calling ensureUI() again.
        if (mStateHolder.getFriends().size() == 0) {
            setEmptyView();
        }
    }
    
    /**
     * Gets friends of the current user we're working for.
     */
    private static class FriendsTask extends AsyncTask<String, Void, Group<User>> {

        private UserFriendsActivity mActivity;
        private Exception mReason;

        public FriendsTask(UserFriendsActivity activity) {
            mActivity = activity;
        }
        
        @Override
        protected void onPreExecute() {
            mActivity.setLoadingView();
        }

        @Override
        protected Group<User> doInBackground(String... params) {
            try {
                Foursquared foursquared = (Foursquared) mActivity.getApplication();
                Foursquare foursquare = foursquared.getFoursquare();
                return foursquare.friends(
                    params[0], LocationUtils.createFoursquareLocation(foursquared.getLastKnownLocation()));
            } catch (Exception e) {
                mReason = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Group<User> users) {
            if (mActivity != null) {
                mActivity.onFriendsTaskComplete(users, mReason);
            }
        }

        @Override
        protected void onCancelled() {
            if (mActivity != null) {
                mActivity.onFriendsTaskComplete(null, mReason);
            }
        }
        
        public void setActivity(UserFriendsActivity activity) {
            mActivity = activity;
        }
    }
    
    
    private static class StateHolder {
        
        /** User id. */
        private String mUserId;
        
        /** Friends of the current user. */
        private Group<User> mFriends;
        
        private FriendsTask mTaskFriends;
        private boolean mIsRunningFriendsTask;
        private boolean mFetchedFriendsOnce;
        
        private boolean mShowAddFriendOptions;

        
        public StateHolder() {
            mIsRunningFriendsTask = false;
            mFetchedFriendsOnce = false;
            mFriends = new Group<User>();
            mShowAddFriendOptions = false;
        }
 
        public String getUserId() {
            return mUserId;
        }
        
        public void setUserId(String userId) {
            mUserId = userId;
        }
        
        public Group<User> getFriends() {
            return mFriends;
        }
        
        public void setFriends(Group<User> friends) {
            mFriends = friends;
        }
        
        public void setShowAddFriendOptions(boolean showAddFriendOptions) {
            mShowAddFriendOptions = showAddFriendOptions;
        }
        
        public boolean getShowAddFriendOptions() {
            return mShowAddFriendOptions;
        }

        public void startTaskFriends(UserFriendsActivity activity,
                                     String userId) {
            mIsRunningFriendsTask = true;
            mTaskFriends = new FriendsTask(activity);
            mTaskFriends.execute(userId);
        }

        public void setActivityForTaskFriends(UserFriendsActivity activity) {
            if (mTaskFriends != null) {
                mTaskFriends.setActivity(activity);
            }
        }

        public void setIsRunningFriendsTask(boolean isRunning) {
            mIsRunningFriendsTask = isRunning;
        }

        public boolean getIsRunningFriendsTask() {
            return mIsRunningFriendsTask;
        }
        
        public void setFetchedFriendsOnce(boolean fetchedOnce) {
            mFetchedFriendsOnce = fetchedOnce;
        }
        
        public boolean getFetchedFriendsOnce() {
            return mFetchedFriendsOnce;
        }
        
        public void cancelTasks() {
            if (mTaskFriends != null) {
                mTaskFriends.setActivity(null);
                mTaskFriends.cancel(true);
            }
        }
    }
}
