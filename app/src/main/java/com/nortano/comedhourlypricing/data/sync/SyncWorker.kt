package com.nortano.comedhourlypricing.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nortano.comedhourlypricing.data.PriceRepository
import com.nortano.comedhourlypricing.data.local.PriceCacheStore
import com.nortano.comedhourlypricing.data.remote.RetrofitClient

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val cacheStore = PriceCacheStore(applicationContext)
        val apiService = RetrofitClient.apiService
        val repository = PriceRepository(apiService, cacheStore)
        
        // Fetch current hour average, the repository saves it to cache which triggers UI updates
        repository.fetchCurrentHourAverage()
        
        return Result.success()
    }
    
    companion object {
        const val WORK_NAME = "PriceSyncWorker"
    }
}
