package olaplay.hackerearth.com.olaplay.database;

/**
 * Created by love on 17/12/17.
 */

import android.app.Activity;
import android.app.Application;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import olaplay.hackerearth.com.olaplay.models.Song;

/**
 * RealmController is used to handle all the database operations.
 */
public class RealmController {

    /**
     * Callback to inform listener the status of operation requested by it.
     */
    public interface RealmTransactionListener
    {
        /**
         *
         * @param statusCode In most cases status greater than 0 is taken as success.
         * @param message
         */
        void onTransactionCompleted(int statusCode, String message);
    }

    private static RealmController instance;

    private Realm realm;

    public RealmController(Application application) {
        realm = Realm.getDefaultInstance();
    }

    public static RealmController with(Activity activity) {

        if (instance == null) {
            instance = new RealmController(activity.getApplication());
        }
        return instance;
    }

    public static RealmController getInstance() {

        return instance;
    }

    public Realm getRealm() {

        return realm;
    }

    public void refresh() {

        realm.refresh();
    }

    /**
     * Fetched list of songs from server is passed as songList.
     * If the song already exists, its fields are updated else a new songs is created and inserted into db.
     * @param songList
     * @param listener
     */

    public void insertOrUpdateSongs(final List<Song> songList, RealmTransactionListener listener){

        try {

            getRealm().executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {

                    for(int count = 0; count < songList.size();count++){

                        Song song = songList.get(count);

                        song.setId(Integer.toString(count));

                        Song existingSong = getSong(song.getId());

                        Song songRealmObject = null;

                        /**
                         * existingSong null means that song does not exist in db, so we create a new song.
                         */
                        if (existingSong == null){

                            songRealmObject = realm.createObject(Song.class,song.getId());

                            songRealmObject.setUrl(song.getUrl());
                            songRealmObject.setSong(song.getSong());
                            songRealmObject.setCoverImage(song.getCoverImage());
                            songRealmObject.setArtists(song.getArtists());
                            songRealmObject.setDownloadProgress(0);

                        } else {

                            songRealmObject = existingSong;

                            /**
                             * If the local path of song url does not match with the new one
                             * we assign the new path and set songdownloadedpath as null.
                             */
                            if (!songRealmObject.getUrl().equalsIgnoreCase(song.getUrl())){

                                songRealmObject.setUrl(song.getUrl());
                                songRealmObject.setSongPath(null);
                            }

                            songRealmObject.setSong(song.getSong());
                            songRealmObject.setCoverImage(song.getCoverImage());
                            songRealmObject.setArtists(song.getArtists());
                        }

                        realm.insertOrUpdate(songRealmObject);
                    }
                }
            });
        } finally {

            /**
             * Inform listener of all the updation operations done successfully.
             */
            if (listener != null){
                listener.onTransactionCompleted(1,"success");
            }
        }
    }

    /**
     * This method is used to set the song download progress as 1 or 0.
     * When a song download is initiated, its status is set as 1 and when download done the status is set back as 0.
     * @param status
     * @param songId
     * @param listener
     */
    public void setDownloadStatusOfSongStatus(final int status, final String songId, final RealmTransactionListener listener){

        try {

            getRealm().executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {

                    Song downloadingSong = getSong(songId);

                    if (downloadingSong != null) {

                        downloadingSong.setDownloadProgress(status);

                        realm.insertOrUpdate(downloadingSong);

                        listener.onTransactionCompleted(1,"success");
                    }

                }
            });

        }catch (Exception e){

            e.printStackTrace();

        }
    }

    /**
     * Used to set the local path of newly downloaded song into db.
     * Also once the path is present we set download progress as 0 which means that song has been downloaded.
     * @param pathOfDownloadedSong
     * @param song
     * @param listener
     */
    public void setSongPathOfDownloadedSong(final String pathOfDownloadedSong, final Song song ,final RealmTransactionListener listener){

        try{

            getRealm().executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {

                    Song downloadedSong = getSong(song.getId());

                    if (downloadedSong != null) {

                        downloadedSong.setSongPath(pathOfDownloadedSong);

                        downloadedSong.setDownloadProgress(Song.DownloadStatus.DOWNLOAD_NOT_IN_PROGRESS);

                        realm.insertOrUpdate(downloadedSong);

                        listener.onTransactionCompleted(1,"file path written");

                    }
                }
            });

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Fetch all the objects of songs from db.
     * @return
     */
    public RealmResults<Song> getAllSongs() {

        return getRealm().where(Song.class).findAll();
    }

    /**
     * Fetch a particular song with song id.
     * @param id
     * @return
     */
    public Song getSong(String id) {

        return getRealm().where(Song.class).equalTo("id", id).findFirst();
    }
}
