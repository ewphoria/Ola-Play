package olaplay.hackerearth.com.olaplay.activities;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.RealmResults;
import olaplay.hackerearth.com.olaplay.R;
import olaplay.hackerearth.com.olaplay.adapters.DividerItemDecorator;
import olaplay.hackerearth.com.olaplay.adapters.SongsAdapter;
import olaplay.hackerearth.com.olaplay.application.OlaPlay;
import olaplay.hackerearth.com.olaplay.database.RealmController;
import olaplay.hackerearth.com.olaplay.fragments.PlayerFragment;
import olaplay.hackerearth.com.olaplay.managers.DataManager;
import olaplay.hackerearth.com.olaplay.models.Song;
import olaplay.hackerearth.com.olaplay.utilities.Constants;
import olaplay.hackerearth.com.olaplay.utilities.NetworkUtil;
import olaplay.hackerearth.com.olaplay.utilities.Utilities;

/**
 * Created by love on 17/12/17.
 */

public class HomeActivity extends AppCompatActivity implements SongsAdapter.OnUserInteractionListener{

    private static final String TAG = HomeActivity.class.getSimpleName();

    /**
     * RecyclerView used to show the list of available songs.
     */
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    /**
     * Swipe refresh layout used to implementing the refresh functionality.
     */
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;

    /**
     * Frame layout contains a fragment which includes the player.
     * Layout shows up on song selection.
     */
    @BindView(R.id.player)
    FrameLayout playerFrameLayout;

    /**
     * Main data source for songs adapter.
     */
    List<Song> songs;

    /**
     * SearchView for achieving the search functionality.
     */
    public SearchView searchView;

    /**
     * Songs adapter for showing the songs in recycler view.
     */
    SongsAdapter songsAdapter;

    /**
     * PAGE was included to achieve the paging functionality.
     * But since no parameter was provided in api, leaving it as it is.
     */
    private int PAGE = 1;

    /**
     * Intent for monitoring the states of internet connectivity.
     */
    IntentFilter intentConnectivity = new IntentFilter(Constants.CONNECTIVITY_CHANGED);

    /**
     * Boolean value used to store the single back press on activity.
     */
    private boolean doubleBackToExitPressedOnce;

    /**
     * Used to prevent crash in API 19 devices.
     */
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecorator(this, DividerItemDecoration.VERTICAL, 0));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                getSongsForPage();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){

        getMenuInflater().inflate(R.menu.main_menu, menu);

        initializeSearchView(menu);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Method to initialize search view and setting listeners
     * @param menu
     */
    private void initializeSearchView(Menu menu){

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        searchView = (SearchView) menu.findItem(R.id.action_search)
                .getActionView();
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                songsAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                songsAdapter.getFilter().filter(query);
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {

        // close search view on back button pressed
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
            return;
        }

        /**
         * If back was already pressed once, close the application.
         */
        if (doubleBackToExitPressedOnce) {
            finish();
            super.onBackPressed();
        }

        /**
         * Back was pressed first time, show the toast.
         */
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        /**
         * Handler used to change the value of doubleBackToExitPressedOnce back to false,
         * if user did not press back again within two seconds.
         */
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2*1000);


    }

    @Override
    protected void onResume() {

        super.onResume();

        /**
         * Register listener for receiving network updates.
         */
        registerReceiver(networkChangeReceiver, intentConnectivity);

        /**
         * If user is returning to the app from somewhere and lists are empty try getting the data again.
         */
        if (songs == null || songs.size() == 0){

            getSongsForPage();
        }

    }

    @Override
    protected void onPause() {

        /**
         * Stop listening to the network updates.
         */
        unregisterReceiver(networkChangeReceiver);

        /**
         * Stop player if activity pauses.
         */
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.adFragment);

        PlayerFragment playerFragment = null;

        if (fragment instanceof PlayerFragment){

            playerFragment = (PlayerFragment) fragment;
            playerFragment.stopSong();

        }

        super.onPause();

    }

    /**
     * BroadcastReceiver for handling the states whenever network connectivity changes.
     */
    BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            boolean isConnected = NetworkUtil.isNetworkConnected(HomeActivity.this);

            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {

                String message = "";

                /**
                 * Network connectivity state is set as unknown at the time of application setup.
                 * When broadcast receiver is registered the first time below if condition satisfies
                 * and current network state is set as application's network state.
                 */
                if (OlaPlay.getInstance().connectionState == OlaPlay.ConnectionState.UNKNOWN){

                    if (isConnected)    OlaPlay.getInstance().connectionState = OlaPlay.ConnectionState.CONNECTED;

                    else if (!isConnected)  OlaPlay.getInstance().connectionState = OlaPlay.ConnectionState.NOTCONNECTED;

                    return;
                }

                if (isConnected){

                    message = "You are back online. Enjoy!";

                    /**
                     * If at the time of network connectivity obtaining the lists are not populated,
                     * Try refreshing the data
                     */
                    if (songs == null || songs.size() == 0){

                        getSongsForPage();
                    }

                    /**
                     * Setting current network state as application's network state.
                     */
                    OlaPlay.getInstance().connectionState = OlaPlay.ConnectionState.CONNECTED;

                } else {

                    message = "We are facing some trouble connecting to internet. Please check your internet connection.";
                    /**
                     * Setting current network state as application's network state.
                     */
                    OlaPlay.getInstance().connectionState = OlaPlay.ConnectionState.NOTCONNECTED;
                }

                /**
                 * Notify user about the change in network state.
                 */
                final Snackbar snackbar = Snackbar
                        .make(swipeRefreshLayout, message, Snackbar.LENGTH_INDEFINITE).setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });

                snackbar.show();

            }

        }
    };


    /**
     * Method used to get the songs listing.
     */
    private void getSongsForPage(){

        /**
         * Notify user of the fetch operation.
         */
        swipeRefreshLayout.setRefreshing(true);

        DataManager.getSharedInstance().getSongs(new DataManager.RetrofitCallListener<Object>() {
            @Override
            public void onCallBackCompleted(int statusCode, String message, Object object) {


                /**
                 * Hide the refresh view if set as refreshing.
                 */
                if (swipeRefreshLayout.isRefreshing()){
                    swipeRefreshLayout.setRefreshing(false);
                }

                /**
                 * In case of some error status code value will be less than 1.
                 */
                if (statusCode == DataManager.Status.ERROR){

                    final Snackbar snackbar = Snackbar
                            .make(swipeRefreshLayout, R.string.error_fetching_songs, Snackbar.LENGTH_INDEFINITE).setAction("Dismiss", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            });

                    snackbar.show();

                }

                populateSongs();
            }
        },HomeActivity.this);

    }

    /**
     * This method fetches songs from database and populates the listing accordingly.
     */
    private void populateSongs(){

        /**
         * Get all the available songs from database even if the songs fetch request has failed from server.
         */

        RealmResults<Song> songsRealmResponse = RealmController.with(HomeActivity.this).getAllSongs();

        /**
         * This condition is true when user has swiped the for refreshing.
         * We reset the value of PAGE to 1 in onRefresh method and again call the getSongsForPage method.
         * This removes all the present songs from the array.
         */
        if (PAGE == 1 && songs != null && songs.size() > 0){
            songs.clear();
        }

        /**
         * If array is not initialized, initialize it.
         */
        if (songs == null){
            songs = new ArrayList<>();
        }

        /**
         * Add all the songs retrieved from database.
         */
        for (Song song : songsRealmResponse){
            songs.add(song);
        }

        /**
         * Initialize the adapter if null.
         * Else inform it of changes.
         */
        if (recyclerView.getAdapter() == null){
            recyclerView.setAdapter(getSongsAdapter());
        }
        else{
            songsAdapter.setSongs(songs);
            songsAdapter.notifyDataChanged();
        }
    }

    /**
     * Initialize the songs adapter and set songs as data source.
     * @return
     */
    private SongsAdapter getSongsAdapter(){

        if (songsAdapter == null){

            songsAdapter = new SongsAdapter(R.layout.row_item_song, HomeActivity.this);
            songsAdapter.setUserInteractionListener(HomeActivity.this);
            songsAdapter.setSongs(songs);
        }

        return songsAdapter;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void userPlayedSong(Song song) {

        /**
         * If user selected songs from search listing, close the keyboard.
         */

        Utilities.hideKeyboard(HomeActivity.this);

        /**
         * Get the instance of PlayerFragment and play the song.
         */
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.adFragment);

        PlayerFragment playerFragment = null;

        if (fragment instanceof PlayerFragment){

            playerFragment = (PlayerFragment) fragment;

        }

        togglePlayerState(true);

        if (playerFragment != null){
            playerFragment.playSong(song);
        }
    }

    @Override
    public void userRequestedSongDownload(final Song song, final int position) {

        DataManager.getSharedInstance().downloadSong(song, HomeActivity.this, new DataManager.RetrofitCallListener<Object>() {
            @Override
            public void onCallBackCompleted(int statusCode, String message, Object object) {

                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);

                SongsAdapter.SongViewHolder songViewHolder = (SongsAdapter.SongViewHolder)viewHolder;

                ProgressBar progressBar = songViewHolder.itemView.findViewById(R.id.progressBar);
                ImageButton downloadButton = songViewHolder.itemView.findViewById(R.id.imgDownload);

                switch (statusCode){

                    case DataManager.Status.DOWNLOAD_STARTED:{

                            if (progressBar != null){
                                progressBar.setVisibility(View.VISIBLE);
                            }

                            if (downloadButton != null){
                                downloadButton.setVisibility(View.INVISIBLE);
                            }

                    }break;

                    case DataManager.Status.DOWNLOAD_COMPLETE:{
                        Toast.makeText(HomeActivity.this, "Song downloaded.", Toast.LENGTH_SHORT).show();

                        if (progressBar != null){
                            progressBar.setVisibility(View.INVISIBLE);
                        }

                        if (downloadButton != null){
                            downloadButton.setVisibility(View.INVISIBLE);
                        }

                    }break;

                    case DataManager.Status.ERROR:{
                        Toast.makeText(HomeActivity.this, "Song download failed.", Toast.LENGTH_SHORT).show();

                        if (progressBar != null){
                            progressBar.setVisibility(View.INVISIBLE);
                        }

                        if (downloadButton != null){
                            downloadButton.setVisibility(View.VISIBLE);
                        }

                    }break;
                }
            }
        },HomeActivity.this);
    }

    @Override
    public void userRequestedMoreOptions(Song song) {

        //TODO Show bottom sheet with options to mark favorites, adding to playlists and other options.
    }

    /**
     * Show or hide the player at the bottom of the screen based on boolean value.
     * @param shouldShow
     */
    private void togglePlayerState(boolean shouldShow){

        if (shouldShow){

            LinearLayout.LayoutParams recyclerViewParams = new LinearLayout.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    0.12f
            );
            recyclerView.setLayoutParams(recyclerViewParams);

            LinearLayout.LayoutParams playerFrameLayoutParams = new LinearLayout.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    0.88f
            );
            playerFrameLayout.setLayoutParams(playerFrameLayoutParams);

        } else {

            LinearLayout.LayoutParams recyclerViewParams = new LinearLayout.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    0f
            );
            recyclerView.setLayoutParams(recyclerViewParams);
        }
    }
}
