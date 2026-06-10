package wadotu.orthodoxy.orthowear.presentation;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface OrthoCalService {
    @GET("api/{calendar}/")
    Call<OrthoDay> getToday(@Path("calendar") String calendar);

    @GET("api/{calendar}/{year}/{month}/{day}/")
    Call<OrthoDay> getDay(
            @Path("calendar") String calendar,
            @Path("year") int year,
            @Path("month") int month,
            @Path("day") int day
    );
}
