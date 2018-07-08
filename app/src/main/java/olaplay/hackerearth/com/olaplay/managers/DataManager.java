package olaplay.hackerearth.com.olaplay.managers;

/**
 * Created by love on 17/12/17.
 */

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import okhttp3.ResponseBody;
import olaplay.hackerearth.com.olaplay.database.RealmController;
import olaplay.hackerearth.com.olaplay.models.Song;
import olaplay.hackerearth.com.olaplay.networking.ApiClient;
import olaplay.hackerearth.com.olaplay.networking.ApiInterface;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Data manager acts as an intermediator between the views and data.
 * All the data required by views like activities and fragments is requested through Data Manager class.
 * All the API requests are made here and the data is passed to database which through callbacks informs of the transactions done.
 */
public class DataManager {

    private static final String TAG = DataManager.class.getSimpleName();

    private static DataManager dataManager = null;

    private DataManager(){

    }

    public class Status {

        public static final int OK = 1;
        public static final int ERROR = -1;
        public static final int DOWNLOAD_STARTED = 2;
        public static final int DOWNLOAD_COMPLETE = 3;
    }

    public interface RetrofitCallListener<T>
    {
        /**
         * /**
         * When an API is called, its response is handled using three parameters
         * @param statusCode if there is handling of data via status codes like 200, 404 etc, we pass it from request here.
         * @param message If there is an error or success message to be shown we pass it through this variable.
         * @param object If we need to pass custom objects, we pass them from here.
         */
        void onCallBackCompleted(int statusCode, String message, T object);
    }

    public static DataManager getSharedInstance(){

        if (dataManager == null){
            dataManager = new DataManager();
        }

        return dataManager;
    }

    /**
     * API call to fetch the available songs from service.
     * @param listener After the songs are fetched and inserted into db, we are notified through this listener.
     * @param activity Required by RealmController class for initialization purpose.
     */
    public void getSongs(final RetrofitCallListener<Object> listener, final Activity activity){

        ApiInterface apiService =
                ApiClient.getClient().create(ApiInterface.class);

        Call<List<Song>> call = apiService.getSongs();

        call.enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>>call, Response<List<Song>> response) {

                Response<List<Song>>  songsResponse = (Response<List<Song>>) response;

                final List<Song> songList = songsResponse.body();

                if (songList != null && songList.size() > 0){

                    RealmController.with(activity).insertOrUpdateSongs(songList, new RealmController.RealmTransactionListener() {
                        @Override
                        public void onTransactionCompleted(int statusCode, String message) {

                            if (statusCode > 0){
                                listener.onCallBackCompleted(Status.OK,"success",null);

                            }
                        }
                    });
                    return;
                }
                listener.onCallBackCompleted(Status.ERROR,"no songs",null);
            }

            @Override
            public void onFailure(Call<List<Song>>call, Throwable t) {
                listener.onCallBackCompleted(Status.ERROR,t.getLocalizedMessage(),null);
                Log.e(TAG, t.toString());
            }
        });
    }

    /**
     * When user requests to download a song, we call the service and store the file locally and update its download status in db.
     * Value of song model's download progress is changed to 1 to indicate reycler view that download has started and call the service.
     * @param song Song object to be downloaded.
     * @param context Required for getting application directory path.
     * @param listener Listening to operation status.
     * @param activity Required by RealmController.
     */
    public void downloadSong(final Song song, final Context context, final RetrofitCallListener<Object> listener, final Activity activity){

        if (song == null){
            return;
        }


        RealmController.with(activity).setDownloadStatusOfSongStatus(Song.DownloadStatus.DOWNLOAD_IN_PROGRESS, song.getId(), new RealmController.RealmTransactionListener() {
            @Override
            public void onTransactionCompleted(int statusCode, String message) {

                if (statusCode > 0){

                    listener.onCallBackCompleted(Status.DOWNLOAD_STARTED,"success",null);

                    ApiInterface apiService =
                            ApiClient.getClient().create(ApiInterface.class);

                    Call<ResponseBody> call = apiService.downloadFileWithUrl(song.getUrl());

                    call.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                            if (response.body() != null) {

                                Log.d(TAG, "server contacted and has file");

                                final String writtenSongPath = writeResponseBodyToDisk(response.body(), song, context);

                                Log.d(TAG, "file download was a success? " + writtenSongPath);

                                if (writtenSongPath != null){

                                    RealmController.with(activity).setSongPathOfDownloadedSong(writtenSongPath, song, new RealmController.RealmTransactionListener() {
                                        @Override
                                        public void onTransactionCompleted(int statusCode, String message) {

                                            listener.onCallBackCompleted(DataManager.Status.DOWNLOAD_COMPLETE,"success",null);
                                        }
                                    });
                                    return;
                                }
                                resetDownloadProgressOfSongAfterFailure(song, listener,activity);

                            } else {
                                resetDownloadProgressOfSongAfterFailure(song, listener,activity);
                            }

                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            resetDownloadProgressOfSongAfterFailure(song, listener,activity);
                            Log.d(TAG, "server contact failed");
                        }
                    });
                }
            }
        });
    }

    private void resetDownloadProgressOfSongAfterFailure(Song song, final RetrofitCallListener listener, Activity activity){

        RealmController.with(activity).setDownloadStatusOfSongStatus(Song.DownloadStatus.DOWNLOAD_NOT_IN_PROGRESS, song.getId(), new RealmController.RealmTransactionListener() {
            @Override
            public void onTransactionCompleted(int statusCode, String message) {
                if (statusCode == Status.OK){
                    listener.onCallBackCompleted(Status.ERROR,"error in downloading song",null);
                }
            }
        });
    }

    /**
     * Downloaded song is stored locally and its path is stored in db.
     * @param body body which includes the mp3 file downloaded from server.
     * @param song Song model object used to get id
     * @param context used for getting path of application directory.
     * @return If writing operation was successfull, we return the path of local file else return null.
     */
    private String writeResponseBodyToDisk(ResponseBody body, Song song, Context context) {
        try {
            // todo change the file location/name according to your needs

            final String applicationDirectory = context.getApplicationInfo().dataDir;

            File songFile = new File(applicationDirectory + File.separator + song.getId() + ".mp3");

            if (songFile.exists()){
                songFile.delete();
            }

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(songFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return songFile.getAbsolutePath();
            } catch (IOException e) {
                return null;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return null;
        }
    }

}
