/**
 * Copyright 2009 Joe LaPenna
 */

package com.joelapenna.foursquared;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.FoursquaredSettings;
import com.joelapenna.foursquare.error.FoursquareException;
import com.joelapenna.foursquare.types.Checkin;
import com.joelapenna.foursquare.types.Group;
import com.joelapenna.foursquared.app.LoadableListActivity;
import com.joelapenna.foursquared.location.LocationUtils;
import com.joelapenna.foursquared.util.CheckinTimestampSort;
import com.joelapenna.foursquared.util.Comparators;
import com.joelapenna.foursquared.util.MenuUtils;
import com.joelapenna.foursquared.util.NotificationsUtil;
import com.joelapenna.foursquared.util.UserUtils;
import com.joelapenna.foursquared.widget.CheckinListAdapter;
import com.joelapenna.foursquared.widget.SeparatedListAdapter;

/**
 * @author Joe LaPenna (joe@joelapenna.com)
 * @author Mark Wyszomierski (markww@gmail.com)
 *         -Added dummy location observer, new menu icon logic, 
 *          links to new user activity (3/10/2010).
 *         -Sorting checkins by distance/time. (3/18/2010)
 */
public class FriendsActivity extends LoadableListActivity {
    static final String TAG = "FriendsActivity";
    static final boolean DEBUG = FoursquaredSettings.DEBUG;

    public static final String QUERY_NEARBY = null;

    private static final int CITY_RADIUS_IN_METERS = 20 * 1000; // 20km
    private static final long SLEEP_TIME_IF_NO_LOCATION = 3000L;

    private static final int MENU_REFRESH = 1;
    private static final int MENU_SHOUT = 2;
    private static final int MENU_STATS = 3;
    private static final int MENU_MYINFO = 4;
    private static final int MENU_GROUP_SEARCH = 0;

    public static SearchResultsObservable searchResultsObservable;
    
    private SearchTask mSearchTask;
    private SearchHolder mSearchHolder = new SearchHolder();
    private SeparatedListAdapter mListAdapter;
    private SearchLocationObserver mSearchLocationObserver = new SearchLocationObserver();

    
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

        searchResultsObservable = new SearchResultsObservable();

        initListViewAdapter();

        if (getLastNonConfigurationInstance() != null) {
            if (DEBUG) Log.d(TAG, "Restoring state.");
            SearchHolder holder = (SearchHolder) getLastNonConfigurationInstance();
            if (holder.results == null) {
                executeSearchTask(holder.query);
            } else {
                mSearchHolder.query = holder.query;
                setSearchResults(holder.results);
                putSearchResultsInAdapter(holder.results);
            }
        } else {
            onNewIntent(getIntent());
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();

        ((Foursquared) getApplication()).requestLocationUpdates(mSearchLocationObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        
        ((Foursquared) getApplication()).removeLocationUpdates(mSearchLocationObserver);

        if (isFinishing()) {
            mListAdapter.removeObserver();
            unregisterReceiver(mLoggedOutReceiver);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSearchTask != null) {
            mSearchTask.cancel(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(MENU_GROUP_SEARCH, MENU_REFRESH, Menu.NONE, R.string.refresh_label) //
                .setIcon(R.drawable.ic_menu_refresh);
        menu.add(Menu.NONE, MENU_SHOUT, Menu.NONE, R.string.shout_action_label) //
                .setIcon(R.drawable.ic_menu_shout);
        menu.add(Menu.NONE, MENU_STATS, Menu.NONE, R.string.stats_label) //
                .setIcon(R.drawable.ic_menu_leaderboard);

        int sdk = new Integer(Build.VERSION.SDK).intValue();
        if (sdk < 4) {
            int menuIcon = UserUtils.getDrawableForMeMenuItemByGender(
                ((Foursquared) getApplication()).getUserGender());
            menu.add(Menu.NONE, MENU_MYINFO, Menu.NONE, R.string.myinfo_label) //
                    .setIcon(menuIcon);
        }

        MenuUtils.addPreferencesToMenu(this, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_REFRESH:
                executeSearchTask(null);
                return true;
            case MENU_SHOUT:
                Intent intent = new Intent(this, CheckinOrShoutGatherInfoActivity.class);
                intent.putExtra(CheckinOrShoutGatherInfoActivity.INTENT_EXTRA_IS_SHOUT, true);
                startActivity(intent);
                return true;
            case MENU_STATS:
                startActivity(new Intent(FriendsActivity.this, StatsActivity.class));
                return true;
            case MENU_MYINFO:
                Intent intentUser = new Intent(FriendsActivity.this, UserDetailsActivity.class);
                intentUser.putExtra(UserDetailsActivity.EXTRA_USER_ID,
                        ((Foursquared) getApplication()).getUserId());
                startActivity(intentUser);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (DEBUG) Log.d(TAG, "New Intent: " + intent);
        if (intent == null) {
            if (DEBUG) Log.d(TAG, "No intent to search, querying default.");
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            if (DEBUG) Log.d(TAG, "onNewIntent received search intent and saving.");
        }
        executeSearchTask(intent.getStringExtra(SearchManager.QUERY));
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mSearchHolder;
    }

    @Override
    public int getNoSearchResultsStringId() {
        return R.string.no_friend_checkins;
    }

    private void initListViewAdapter() {
        mListAdapter = new SeparatedListAdapter(this);
        
        ListView listView = getListView();
        listView.setAdapter(mListAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Checkin checkin = (Checkin) parent.getAdapter().getItem(position);
                if (checkin.getUser() != null) {
                    Intent intent = new Intent(FriendsActivity.this, UserDetailsActivity.class);
                    intent.putExtra(UserDetailsActivity.EXTRA_USER_PARCEL, checkin.getUser());
                    intent.putExtra(UserDetailsActivity.EXTRA_SHOW_ADD_FRIEND_OPTIONS, true);
                    startActivity(intent);
                }
            }
        });
        listView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                return false;
            }
        });
    }

    private void setSearchResults(Group<Checkin> searchResults) {
        if (DEBUG) Log.d(TAG, "Setting search results.");
        mSearchHolder.results = searchResults;
        searchResultsObservable.notifyObservers();
    }

    private void executeSearchTask(String query) {
        if (DEBUG) Log.d(TAG, "sendQuery()");
        mSearchHolder.query = query;
        // not going through set* because we don't want to notify search result
        // observers.
        mSearchHolder.results = null;

        // If a task is already running, don't start a new one.
        if (mSearchTask != null && mSearchTask.getStatus() != AsyncTask.Status.FINISHED) {
            if (DEBUG) Log.d(TAG, "Query already running attempting to cancel: " + mSearchTask);
            if (!mSearchTask.cancel(true) && !mSearchTask.isCancelled()) {
                if (DEBUG) Log.d(TAG, "Unable to cancel search? Notifying the user.");
                Toast.makeText(this, "A search is already in progress.", Toast.LENGTH_SHORT);
                return;
            }
        }
        mSearchTask = (SearchTask) new SearchTask().execute();
    }

    private void ensureTitle(boolean finished) {
        if (finished) {
            setTitle(R.string.friendsactivity_title_finished);
        } else {
            setTitle(R.string.friendsactivity_title_searching);
        }

    }

    private class SearchTask extends AsyncTask<Void, Void, Group<Checkin>> {

        private Exception mReason = null;

        @Override
        public void onPreExecute() {
            if (DEBUG) Log.d(TAG, "SearchTask: onPreExecute()");
            setProgressBarIndeterminateVisibility(true);
            ensureTitle(false);
            setLoadingView();
        }

        @Override
        public Group<Checkin> doInBackground(Void... params) {
            try {
                return search();
            } catch (Exception e) {
                mReason = e;
            }
            return null;
        }

        @Override
        public void onPostExecute(Group<Checkin> checkins) {
            try {
                if (checkins == null) {
                    NotificationsUtil.ToastReasonForFailure(FriendsActivity.this, mReason);
                }
                setSearchResults(checkins);
                putSearchResultsInAdapter(checkins);

            } finally {
                setProgressBarIndeterminateVisibility(false);
                ensureTitle(true);
                setEmptyView();
            }
        }

        Group<Checkin> search() throws FoursquareException, IOException {
            Foursquare foursquare = ((Foursquared) getApplication()).getFoursquare();
            
            // If we're the startup tab, it's likely that we won't have a geo location
            // immediately. For now we can use this ugly method of sleeping for three
            // seconds to at least let network location get a lock. We're only trying
            // to discern between same-city, so we can even use LocationManager's
            // getLastKnownLocation() method because we don't care if we're even a few
            // miles off.
            Location loc = ((Foursquared) getApplication())
                    .getLastKnownLocation();
            if (loc == null) {
                try { Thread.sleep(SLEEP_TIME_IF_NO_LOCATION); } catch (InterruptedException ex) {}
                loc = ((Foursquared) getApplication())
                    .getLastKnownLocation();
            }
            
            Group<Checkin> checkins = foursquare.checkins(LocationUtils
                    .createFoursquareLocation(loc));
            
            Collections.sort(checkins, Comparators.getCheckinRecencyComparator());
            
            return checkins;
        }
    }
    
    /**
     * Sort checkin results first by distance [same city | different city],
     * then within the [same city] bucket, sort by last three hours, today,
     * and yesterday. If we had no geoloation at the time of the search, we
     * won't have any distance parameter to do the first level of sorting,
     * in this case we just place all our friends in the [same city] bucket.
     */
    private void putSearchResultsInAdapter(Group<Checkin> checkins) {
        
        Group<Checkin> recent = new Group<Checkin>();
        Group<Checkin> today = new Group<Checkin>();
        Group<Checkin> yesterday = new Group<Checkin>();
        Group<Checkin> older = new Group<Checkin>();
        Group<Checkin> other = new Group<Checkin>();

        if (checkins != null && checkins.size() > 0) {

            CheckinTimestampSort timestamps = new CheckinTimestampSort();
            for (Checkin it : checkins) {
    
                // If we can't parse the distance value, it's possible that we
                // did not have a geolocation for the device at the time the
                // search was run. In this case just assume this friend is nearby
                // to sort them in the time buckets.
                int meters = 0;
                try {
                    meters = Integer.parseInt(it.getDistance());
                } catch (NumberFormatException ex) {
                    if (DEBUG) Log.d(TAG, "Couldn't parse distance for checkin during friend search.");
                    meters = 0;
                }
      
                if (meters > CITY_RADIUS_IN_METERS) {
                    other.add(it);
                } else {
                    try { 
                        Date date = new Date(it.getCreated());
                        if (date.after(timestamps.getBoundaryRecent())) {
                            recent.add(it);
                        } else if (date.after(timestamps.getBoundaryToday())) {
                            today.add(it); 
                        } else if (date.after(timestamps.getBoundaryYesterday())) {
                            yesterday.add(it);
                        } else {
                            older.add(it);
                        }
                    } catch (Exception ex) {
                        older.add(it);
                    }
                }
            }
        }
        
        mListAdapter.removeObserver();
        mListAdapter.clear();
        mListAdapter = new SeparatedListAdapter(this);
        if (recent.size() > 0) {
            CheckinListAdapter adapter = new CheckinListAdapter(this, 
                    ((Foursquared) getApplication()).getRemoteResourceManager());
            adapter.setGroup(recent);
            mListAdapter.addSection(getResources().getString(
                    R.string.friendsactivity_title_sort_recent), adapter);
        }
        if (today.size() > 0) {
            CheckinListAdapter adapter = new CheckinListAdapter(this, 
                    ((Foursquared) getApplication()).getRemoteResourceManager());
            adapter.setGroup(today);
            mListAdapter.addSection(getResources().getString(
                    R.string.friendsactivity_title_sort_today), adapter);
        }
        if (yesterday.size() > 0) {
            CheckinListAdapter adapter = new CheckinListAdapter(this, 
                    ((Foursquared) getApplication()).getRemoteResourceManager());
            adapter.setGroup(yesterday);
            mListAdapter.addSection(getResources().getString(
                    R.string.friendsactivity_title_sort_yesterday), adapter);
        }
        if (older.size() > 0) {
            CheckinListAdapter adapter = new CheckinListAdapter(this, 
                    ((Foursquared) getApplication()).getRemoteResourceManager());
            adapter.setGroup(older);
            mListAdapter.addSection(getResources().getString(
                    R.string.friendsactivity_title_sort_older), adapter);
        }
        if (other.size() > 0) {
            CheckinListAdapter adapter = new CheckinListAdapter(this, 
                    ((Foursquared) getApplication()).getRemoteResourceManager());
            adapter.setGroup(other);
            mListAdapter.addSection(getResources().getString(
                    R.string.friendsactivity_title_sort_other_city), adapter);
        }
        
        getListView().setAdapter(mListAdapter);
    }

    private static class SearchHolder {
        Group<Checkin> results;
        String query;
    }

    class SearchResultsObservable extends Observable {

        @Override
        public void notifyObservers(Object data) {
            setChanged();
            super.notifyObservers(data);
        }

        public Group<Checkin> getSearchResults() {
            return mSearchHolder.results;
        }

        public String getQuery() {
            return mSearchHolder.query;
        }
    };
    
    /** 
     * This is really just a dummy observer to get the GPS running
     * since this is the new splash page. After getting a fix, we
     * might want to stop registering this observer thereafter so
     * it doesn't annoy the user too much.
     */
    private class SearchLocationObserver implements Observer {
        @Override
        public void update(Observable observable, Object data) {
        }
    }
}
