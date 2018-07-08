package olaplay.hackerearth.com.olaplay.networking;

import java.util.List;

import okhttp3.ResponseBody;
import olaplay.hackerearth.com.olaplay.models.Song;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by love on 17/12/17.
 */

public interface ApiInterface {

    @GET("studio")
    Call<List<Song>> getSongs();

    @GET
    Call<ResponseBody> downloadFileWithUrl(@Url String fileUrl);
}
