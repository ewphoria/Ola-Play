package olaplay.hackerearth.com.olaplay.adapters;

import android.content.Context;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import olaplay.hackerearth.com.olaplay.R;
import olaplay.hackerearth.com.olaplay.models.Song;

/**
 * Created by love on 17/12/17.
 */

public class SongsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    /**
     * Field storing all the songs original songs
     */
    private List<Song> songs;

    /**
     * Field storing all the filtered songs based on user's search.
     * If no search input is there all the values from above field 'songs' are assigned to songsListFiltered.
     */
    private List<Song> songsListFiltered;

    private int rowLayout;

    /**
     * Context for communication with HomeActivity or handling threads.
     */
    private Context context;

    /**
     * When user scrolls to the bottom of list, we load more songs.
     * For better UX we fetch more songs when user is about to reach bottom of the list.
     * INDEX_TO_LOAD_ITEMS_AT decides when to fetch more songs.
     */
    private int INDEX_TO_LOAD_ITEMS_AT = 3;

    /**
     * Interface to notify HomeActivity to load more songs.
     */
    OnLoadMoreListener loadMoreListener;

    /**
     * Interface to handle user interactions like play, download and others.
     */
    public OnUserInteractionListener userInteractionListener;

    /**
     * isLoading is used to prevent calling of OnLoadMoreListener's onLoadMore if a request to fetch songs has already been sent.
     */
    boolean isLoading = false;


    /**
     * If more than one view type is being used like header footer view, this class defines their value.
     */
    private class VIEW_TYPES {
        public static final int Header = 1;
        public static final int SONG = 2;
        public static final int EMPTYVIEW = 3;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {

                final String charString = charSequence.toString();

                /**
                 * If search view is opened but with no text, show all the default songs.
                 */
                if (charString.isEmpty()) {

                    songsListFiltered = songs;

                } else {

                    final List<Song> filteredList = new ArrayList<>();

                    /**
                     * Performing the operation on background thread due to an error by Realm
                     * which requires the accessing of objects from same threads on which they were created.
                     */
                    Handler handler = new Handler(context.getMainLooper());

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {

                            for (Song row : songs) {

                                if (row.getSong().toLowerCase().contains(charString.toLowerCase())) {
                                    filteredList.add(row);
                                }
                            }
                        }
                    };

                    handler.post(runnable);


                    songsListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = songsListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                songsListFiltered = (ArrayList<Song>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    public class EmptyViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }


    public static class SongViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.song_layout)
        ConstraintLayout songLayout;

        @BindView(R.id.title)
        TextView title;

        @BindView(R.id.artist)
        TextView artist;

        @BindView(R.id.imgCover)
        ImageView imgCover;

        @BindView(R.id.imgDownload)
        ImageButton imgDownload;

        @BindView(R.id.more)
        ImageButton imgMore;

        @BindView(R.id.progressBar)
        ProgressBar progressBar;

        Context context;

        RequestOptions requestOptions;

        public SongViewHolder(View v, final Context context) {
            super(v);
            ButterKnife.bind(this, v);
            this.context = context;

            requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.ic_audiotrack_black_24dp);
            requestOptions.apply(RequestOptions.circleCropTransform());

        }

        void bindData(Song songModel){

            title.setText(songModel.getSong());
            artist.setText(songModel.getArtists());
            Glide.with(context)
                    .load(songModel.getCoverImage())
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imgCover);

            /**
             * If download progress is equal to 1, show progress bar,
             * else show the download button.
             */
            if (songModel.getDownloadProgress() == Song.DownloadStatus.DOWNLOAD_IN_PROGRESS){

                imgDownload.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);

            } else {
                imgDownload.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
            }

            /**
             * If songpath exists, hide the download option.
             */
            if (songModel.songExistsLocally()){
                imgDownload.setVisibility(View.GONE);
            }
        }

    }

    public SongsAdapter(int rowLayout, Context context) {

        this.rowLayout = rowLayout;
        this.context = context;
    }

    public void setSongs(List<Song> movies){
        this.songs = movies;
        setFilteredSongs(this.songs);
    }

    public void setFilteredSongs(List<Song> movies){

        this.songsListFiltered = movies;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {

        LayoutInflater inflater = LayoutInflater.from(context);
        RecyclerView.ViewHolder rowView = null;


        switch (viewType){

            case VIEW_TYPES.SONG:
                rowView = new SongViewHolder(inflater.inflate(rowLayout,parent,false),this.context);
                break;

            case VIEW_TYPES.EMPTYVIEW:{
                rowView = new EmptyViewHolder(inflater.inflate(R.layout.nodata,parent,false));
                break;
            }

        }

        return rowView;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        /**
         * User is about the reach the end of list, tell listener activity to get more items, if not already fetching.
         */
        if(position>=getItemCount()-INDEX_TO_LOAD_ITEMS_AT && !isLoading && loadMoreListener!=null){
            isLoading = true;
            loadMoreListener.onLoadMore();
        }

        if(getItemViewType(position)==VIEW_TYPES.SONG){

            ((SongViewHolder)holder).bindData(songsListFiltered.get(position));

            ((SongViewHolder)holder).songLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (userInteractionListener != null){
                        userInteractionListener.userPlayedSong(songsListFiltered.get(position));
                    }
                }
            });

            ((SongViewHolder)holder).imgDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (userInteractionListener != null){
                        userInteractionListener.userRequestedSongDownload(songsListFiltered.get(position), position);
                    }
                }
            });

            ((SongViewHolder)holder).imgMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (userInteractionListener != null){
                        userInteractionListener.userRequestedMoreOptions(songsListFiltered.get(position));
                    }
                }
            });

        }

    }

    @Override
    public int getItemViewType(int position) {

        if (songsListFiltered.size() == 0) {
            return VIEW_TYPES.EMPTYVIEW;
        }

        if(songs.get(position) instanceof Song)
            return VIEW_TYPES.SONG;

        return VIEW_TYPES.EMPTYVIEW;

    }

    @Override
    public int getItemCount() {
        return songsListFiltered.size() > 0 ? songsListFiltered.size() : 1;
    }

    public void notifyDataChanged(){
        notifyDataSetChanged();
        isLoading = false;
    }

    public interface OnLoadMoreListener{
        void onLoadMore();
    }

    public void setLoadMoreListener(OnLoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }

    public void setUserInteractionListener(OnUserInteractionListener userInteractionListener) {
        this.userInteractionListener = userInteractionListener;
    }

    public interface OnUserInteractionListener {

        /**
         * When user clicks on a song to play.
         * @param song
         */
        void userPlayedSong(Song song);

        /**
         * When user clicks on download button.
         * @param song
         */
        void userRequestedSongDownload(Song song, int position);

        /**
         * When user clicks on more options.
         * @param song
         */
        void userRequestedMoreOptions(Song song);
    }
}
