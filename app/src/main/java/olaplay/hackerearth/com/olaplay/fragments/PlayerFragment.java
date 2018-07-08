package olaplay.hackerearth.com.olaplay.fragments;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import olaplay.hackerearth.com.olaplay.R;
import olaplay.hackerearth.com.olaplay.models.Song;
import olaplay.hackerearth.com.olaplay.utilities.NetworkUtil;

import static io.realm.internal.SyncObjectServerFacade.getApplicationContext;

/**
 * Created by love on 17/12/17.
 */

public class PlayerFragment extends Fragment {

    private static final String TAG = PlayerFragment.class.getSimpleName();

    @BindView(R.id.artist)
    TextView artist;

    @BindView(R.id.title)
    TextView title;

    @BindView(R.id.imgCover)
    ImageView imgCover;

    @BindView(R.id.video_view)
    SimpleExoPlayerView playerView;

    @BindView(R.id.seekbar)
    SeekBar seekBar;

    @BindView(R.id.imgPlayPause)
    ImageView imgPlayPause;

    @BindView(R.id.progressBarWhite)
    ProgressBar progressBar;

    /**
     * Local field of SimpleExoPlayer used to play songs.
     */
    private SimpleExoPlayer player;

    /**
     * This handler is used to run the seekbar.
     */
    Handler handler;

    /**
     * Holds the instance of Song currently being played.
     */
    Song currentSong;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.row_player, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view,savedInstanceState);

        handler = new Handler(getActivity().getMainLooper());

        player = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(getActivity()),
                new DefaultTrackSelector(), new DefaultLoadControl());

        playerView.setPlayer(player);

        player.addListener(eventListener);


    }

    Player.EventListener eventListener = new Player.EventListener() {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {

        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

            if (playbackState == ExoPlayer.STATE_ENDED) {

            }
            else if (playbackState == ExoPlayer.STATE_BUFFERING)
            {
                progressBar.setVisibility(View.VISIBLE);
                imgPlayPause.setVisibility(View.INVISIBLE);
            }
            else if (playbackState == ExoPlayer.STATE_READY)
            {
                progressBar.setVisibility(View.INVISIBLE);
                imgPlayPause.setVisibility(View.VISIBLE);
            }

        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {

        }

        @Override
        public void onPositionDiscontinuity(int reason) {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }

        @Override
        public void onSeekProcessed() {

        }
    };

    public void playSong(Song song){

        if (song != null){

            currentSong = song;

            title.setText(song.getSong());
            artist.setText(song.getArtists());

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.ic_audiotrack_black_24dp);
            requestOptions.apply(RequestOptions.circleCropTransform());

            Glide.with(getActivity())
                    .load(song.getCoverImage())
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imgCover);
        }

        initializePlayer(song);

    }

    private void initializePlayer(Song song) {

        try {
            stopSong();

            player.setPlayWhenReady(true);

            Uri uri;

            MediaSource mediaSource;

            /**
             * If the songs is present locally, play it from the file else stream it online.
             */

            if (song.songExistsLocally()){

                uri = Uri.parse(song.getSongPath());

                mediaSource = buildMediaSourceForLocalFile(uri);

            } else {

                if (!NetworkUtil.isNetworkConnected(getActivity())){
                    Toast.makeText(getActivity(), "Please check your internet connection and try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                uri = Uri.parse(song.getUrl());

                mediaSource = buildMediaSourceForStreaming(uri);
            }

            player.prepare(mediaSource, true, false);

            handler.postDelayed(runnable,1000);

            imgPlayPause.setSelected(true);

        } catch (Exception e){

            e.printStackTrace();
        }

    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {

            if (player != null){

                long timeRemaining = player.getDuration() - player.getCurrentPosition();

                seekBar.setProgress((int) player.getCurrentPosition());
                seekBar.setMax((int) player.getDuration());

            }
            handler.postDelayed(runnable,1000);
        }
    };


    private MediaSource buildMediaSourceForStreaming(Uri uri) {

        return new ExtractorMediaSource(uri,
                new DefaultHttpDataSourceFactory(
                        "ua",
                        null,
                        DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                        DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                        true

                        /* allowCrossProtocolRedirects setting true allows the redirection from "http://hck.re/Rh8KTk to
                        * https://s3-ap-southeast-1.amazonaws.com/he-public-data/Afreen%20Afreen%20(DjRaag.Net)2cc6f8b.mp3*/
                ),
                new DefaultExtractorsFactory(), null, null);
    }

    private MediaSource buildMediaSourceForLocalFile(Uri uri){

        DataSpec dataSpec = new DataSpec(uri);
        final FileDataSource fileDataSource = new FileDataSource();
        try {
            fileDataSource.open(dataSpec);
        } catch (FileDataSource.FileDataSourceException e) {
            e.printStackTrace();
        }

        DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return fileDataSource;
            }
        };
        return new ExtractorMediaSource(fileDataSource.getUri(),
                factory, new DefaultExtractorsFactory(), null, null);
    }

    public void stopSong(){

        if (player != null){
            player.stop();
        }

        if (handler != null){
            handler.removeCallbacks(runnable);
        }

        imgPlayPause.setSelected(false);
    }

    public void pausePlayer(){

        player.setPlayWhenReady(false);
        handler.removeCallbacks(runnable);
        imgPlayPause.setSelected(false);

    }

    public void resumePlayer(){

        player.setPlayWhenReady(true);
        handler.postDelayed(runnable,1000);
        imgPlayPause.setSelected(true);
    }


    @OnClick(R.id.imgPlayPause)
    public void togglePlayPause(ImageView button){

        if (button.isSelected()){

            pausePlayer();

        } else {

            resumePlayer();
        }

    }

    private void releasePlayer() {

        if (player != null) {

            player.release();
            player = null;
        }
    }

}
