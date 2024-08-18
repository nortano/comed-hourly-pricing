import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("api?type=5minutefeed")
    fun getFiveMinFeed(): Call<FiveMinFeed>

    @GET("api?type=currenthouraverage")
    fun getCurrentAvg(): Call<List<CurrentAvg>>
}