package olaplay.hackerearth.com.olaplay.models;

/**
 * Created by love on 17/12/17.
 */

import com.google.gson.annotations.SerializedName;

import java.io.File;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Song class serves two purposes.
 * One is to be used as model object by Retrofit and other by Realm for db operations.
 */
public class Song extends RealmObject {

    public class DownloadStatus {

        public static final int DOWNLOAD_IN_PROGRESS = 1;
        public static final int DOWNLOAD_NOT_IN_PROGRESS = 0;

    }
    /**
     * id key is not provided in service so right now the position of song in array is used as id parameter.
     */
    @PrimaryKey
    private String id;

    @SerializedName("song")
    private String song;

    @SerializedName("url")
    private String url;

    @SerializedName("artists")
    private String artists;

    @SerializedName("cover_image")
    private String coverImage;

    /**
     * songPath is used by Realm to store the path of downloaded song locally.
     */
    private String songPath;

    /**
     * downloadProgress is used to notify user of the download operation.
     * 1 if download operation is currently in progress.
     * 0 if download operation is not in progress.
     */
    private int downloadProgress;

    public String getSong() {
        return song;
    }

    public void setSong(String song) {
        this.song = song;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getArtists() {
        return artists;
    }

    public void setArtists(String artists) {
        this.artists = artists;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getSongPath() {
        return songPath;
    }

    public void setSongPath(String songPath) {
        this.songPath = songPath;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadProgress(int downloadProgress) {
        this.downloadProgress = downloadProgress;
    }

    public boolean songExistsLocally(){

        if (this.getSongPath() != null && this.getSongPath().equalsIgnoreCase("")){

            File file = new File(getSongPath());

            return file.exists();
        }
        return false;
    }
}
