package com.nodimobilebanking.utils

import android.content.Context
import com.nodimobilebanking.data.api.RetrofitClient
import com.nodimobilebanking.data.repository.SaccoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fetches CompanyInfo from the backend once and caches it locally.
 * UI uses TokenManager.getCompanyName()/getCompanyLogoUrl() to read.
 * Fire-and-forget: failures are silent (UI falls back to "SACCO MIS").
 */
object CompanyInfoManager {

    fun refreshAsync(context: Context) {
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tm = TokenManager(appCtx)
                val repo = SaccoRepository(RetrofitClient.getInstance(tm, appCtx), tm)
                val r = repo.getCompanyInfo()
                if (r is Resource.Success) {
                    tm.saveCompanyInfo(
                        name = r.data.companyName,
                        logoUrl = r.data.companyLogoUrl,
                        footer = r.data.companyFooter,
                        address = r.data.companyAddress,
                        contact = r.data.companyContact,
                        location = r.data.companyLocation,
                    )
                }
            } catch (_: Exception) { /* silent — UI will fall back */ }
        }
    }
}
