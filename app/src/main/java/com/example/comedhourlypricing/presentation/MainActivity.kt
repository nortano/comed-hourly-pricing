/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.comedhourlypricing.presentation

import CurrentAvg
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.comedhourlypricing.presentation.theme.ComedHourlyPricingTheme
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response


class MainActivity : ComponentActivity() {
    private var curPrice = "<NO DATA>"
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        Log.d("AARON", "WE'RE CREATING!")
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        getPrice { price ->
            if (price != null) {
                curPrice = price
            }
        }
        setContent {
            Log.d("AARON", "WE'RE SETTING CONTENT, ON CREATE")
            WearApp(curPrice)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d("AARON", "WE'RE DESTROYING!")
    }
    override fun onPause() {
        super.onPause()
        Log.d("AARON", "WE'RE PAUSING!")
    }
    override fun onResume() {
        super.onResume()
        Log.d("AARON", "WE'RE RESUMING!")
        getPrice { price ->
            if (price != null) {
                curPrice = price
            }
        }
        setContent {
            Log.d("AARON", "WE'RE SETTING CONTENT, ON CREATE")
            WearApp(curPrice)
        }
    }
    override fun onStart() {
        super.onStart()
        Log.d("AARON", "WE'RE STARTING!")
        getPrice { price ->
            if (price != null) {
                curPrice = price
            }
        }
        setContent {
            Log.d("AARON", "WE'RE SETTING CONTENT, ON CREATE")
            WearApp(curPrice)
        }
    }
    override fun onStop() {
        super.onStop()
        Log.d("AARON", "WE'RE STOPPING!")
    }
    override fun onRestart() {
        super.onRestart()
        Log.d("AARON", "WE'RE RESTARTING!")
        getPrice { price ->
            if (price != null) {
                curPrice = price
            }
        }
        setContent {
            Log.d("AARON", "WE'RE SETTING CONTENT, ON CREATE")
            WearApp(curPrice)
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("AARON", "WE'RE SAVING!")
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d("AARON", "WE'RE RESTORING!")
    }
}

@Composable
fun WearApp(lastHourPrice: String) {
    ComedHourlyPricingTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            DisplayPrice(lastHourPrice = lastHourPrice)
        }
    }
}

@Composable
fun DisplayPrice(lastHourPrice: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = "Current Price:\n $lastHourPrice cents per kWh"
    )
}

fun getPrice(callback: (String?) -> Unit) {
    val call = ApiClient.apiService.getCurrentAvg()
    call.enqueue(object : Callback<List<CurrentAvg>> {
        override fun onResponse(call: Call<List<CurrentAvg>>, response: Response<List<CurrentAvg>>){
            if (response.isSuccessful) {
                val curAvg = response.body()
                Log.d("AARON","BODY $curAvg")
                val price = curAvg?.get(0)?.price
                callback(price)
            } else {
                Log.d("AARON", "Response failed $response")
            }
        }

        override fun onFailure(p0: Call<List<CurrentAvg>>, p1: Throwable) {
            Log.d("AARON", "Failed to fetch data. Error: ${p1.message}")
        }
    })
}