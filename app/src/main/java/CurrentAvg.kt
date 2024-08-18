import com.google.gson.annotations.SerializedName

data class CurrentAvg (

    @SerializedName("millisUTC" ) var millisUTC : String? = null,
    @SerializedName("price"     ) var price     : String? = null

)