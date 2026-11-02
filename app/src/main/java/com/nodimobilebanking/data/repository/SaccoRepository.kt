package com.nodimobilebanking.data.repository

import com.nodimobilebanking.data.api.SaccoApiService
import com.nodimobilebanking.data.models.*
import com.nodimobilebanking.utils.Resource
import com.nodimobilebanking.utils.TokenManager
import org.json.JSONObject

class SaccoRepository(
    private val api: SaccoApiService,
    private val tokenManager: TokenManager
) {

    // ── Onboarding (member_no only → OTP → auto-activate) ──────────────────
    suspend fun onboardInitiate(memberNo: String): Resource<OtpSentResponse> = safeCall {
        val r = api.onboardInitiate(mapOf("member_no" to memberNo.trim()))
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun onboardVerifyOtp(memberNo: String, otp: String): Resource<LoginResponse> = safeCall {
        val deviceId = tokenManager.getDeviceId()
        val r = api.onboardVerifyOtp(mapOf(
            "member_no" to memberNo.trim(), "otp_code" to otp.trim(), "device_id" to deviceId
        ))
        if (r.isSuccessful && r.body() != null) {
            val b = r.body()!!
            tokenManager.saveTokens(b.access, b.refresh)
            tokenManager.saveDeviceId(deviceId)
            tokenManager.saveUsername(b.username)
            tokenManager.saveMemberNo(memberNo.trim())
            Resource.Success(b)
        } else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun onboardResendOtp(memberNo: String): Resource<String> = safeCall {
        val r = api.onboardResendOtp(mapOf("member_no" to memberNo.trim()))
        if (r.isSuccessful) Resource.Success(r.body()!!.message)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    // ── Auth (OTP-only login) ────────────────────────────────────────────────
    suspend fun loginRequestOtp(memberNo: String): Resource<OtpSentResponse> = safeCall {
        val deviceId = tokenManager.getDeviceId()
        val r = api.loginRequestOtp(mapOf("member_no" to memberNo.trim(), "device_id" to deviceId))
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun loginVerifyOtp(memberNo: String, otp: String): Resource<LoginResponse> = safeCall {
        val deviceId = tokenManager.getDeviceId()
        val r = api.loginVerifyOtp(mapOf(
            "member_no" to memberNo.trim(), "otp_code" to otp.trim(), "device_id" to deviceId
        ))
        if (r.isSuccessful && r.body() != null) {
            val b = r.body()!!
            tokenManager.saveTokens(b.access, b.refresh)
            tokenManager.saveDeviceId(deviceId)
            tokenManager.saveUsername(b.username)
            tokenManager.saveMemberNo(memberNo.trim())
            Resource.Success(b)
        } else Resource.Error(parseError(r.errorBody()?.string()))
    }

    // ── Auth (PIN login) ──────────────────────────────────────────────────────
    suspend fun loginWithPin(memberNo: String, pin: String): Resource<LoginResponse> = safeCall {
        val deviceId = tokenManager.getDeviceId()
        val r = api.loginWithPin(mapOf(
            "member_no" to memberNo.trim(), "pin" to pin, "device_id" to deviceId
        ))
        if (r.isSuccessful && r.body() != null) {
            val b = r.body()!!
            tokenManager.saveTokens(b.access, b.refresh)
            tokenManager.saveDeviceId(deviceId)
            tokenManager.saveUsername(b.username)
            tokenManager.saveMemberNo(memberNo.trim())
            Resource.Success(b)
        } else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun setPin(pin: String): Resource<String> = safeCall {
        val r = api.setPin(mapOf("pin" to pin))
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!.message)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun logout(): Resource<Unit> = safeCall {
        try { api.logout(mapOf("refresh" to (tokenManager.getRefreshToken() ?: ""))) }
        catch (_: Exception) {}
        tokenManager.clearTokens()
        Resource.Success(Unit)
    }

    suspend fun passwordResetRequest(memberNo: String): Resource<PasswordResetRequestResponse> = safeCall {
        val r = api.passwordResetRequest(mapOf("member_no" to memberNo.trim()))
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun passwordResetVerifyOtp(memberNo: String, otp: String): Resource<PasswordResetVerifyResponse> = safeCall {
        val r = api.passwordResetVerifyOtp(mapOf(
            "member_no" to memberNo.trim(), "otp_code" to otp.trim()
        ))
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun passwordResetConfirm(resetToken: String, newPassword: String, confirm: String): Resource<String> = safeCall {
        val r = api.passwordResetConfirm(mapOf(
            "reset_token" to resetToken,
            "new_password" to newPassword, "confirm_password" to confirm
        ))
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!.message)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    // ── Member ───────────────────────────────────────────────────────────────
    suspend fun getMemberProfile(): Resource<MemberProfile> = safeCall {
        val r = api.getMemberProfile()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun getNextOfKins(): Resource<List<NextOfKin>> = safeCall {
        val r = api.getNextOfKins()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun getMyGuarantors(): Resource<List<GuarantorEntry>> = safeCall {
        val r = api.getMyGuarantors()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun getMyGuarantees(): Resource<List<GuarantorEntry>> = safeCall {
        val r = api.getMyGuarantees()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun getMemberAccounts(): Resource<List<MemberAccount>> = safeCall {
        val r = api.getMemberAccounts()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!.results)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    // ── Mobile Loan Types & Limits ────────────────────────────────────────────
    suspend fun getMobileLoanTypes(): Resource<List<MobileLoanType>> = safeCall {
        val r = api.getMobileLoanTypes()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun getMemberLoanLimit(): Resource<LoanLimitResponse> = safeCall {
        val r = api.getMemberLoanLimit()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    // ── Balances ──────────────────────────────────────────────────────────────
    suspend fun getBalanceSummary(): Resource<BalanceSummary> = safeCall {
        val r = api.getBalanceSummary()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    // ── Transactions ──────────────────────────────────────────────────────────
    suspend fun getSavingsTransactions(): Resource<List<SavingsTransaction>> = safeCall {
        val r = api.getSavingsTransactions()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!.results)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun getLoanTransactions(): Resource<List<LoanTransaction>> = safeCall {
        val r = api.getLoanTransactions()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!.results)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun getLoanHistory(): Resource<List<LoanHistory>> = safeCall {
        val r = api.getLoanHistory()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!.results)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    suspend fun makePayment(memberNo: String, savingType: String, amount: String, desc: String): Resource<String> = safeCall {
        val r = api.makePayment(PaymentRequest(memberNo, savingType, amount, desc))
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!.message)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun makeWithdrawal(
        memberNo: String,
        savingType: String,
        amount: String,
        desc: String,
        otpCode: String = "",
    ): Resource<WithdrawalActionResponse> = safeCall {
        val deviceId = tokenManager.getDeviceId()
        val r = api.makeWithdrawal(
            WithdrawRequest(memberNo, savingType, amount, desc, otpCode, deviceId)
        )
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    /** Charge preview before confirming a withdrawal */
    suspend fun withdrawalQuote(savingType: String, amount: String): Resource<WithdrawalQuote> = safeCall {
        val r = api.withdrawalQuote(mapOf("saving_type" to savingType, "amount" to amount))
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    /** Unified deposit — handles both savings deposits and loan repayments */
    suspend fun makeDeposit(memberNo: String, accountType: String, amount: String, desc: String): Resource<String> = safeCall {
        val r = api.makeDeposit(DepositRequest(memberNo, accountType, amount, desc))
        if (r.isSuccessful && r.body() != null) Resource.Success("${r.body()!!.type}: ${r.body()!!.message}")
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun applyLoan(memberNo: String, amount: String, loanType: String, period: Int, purpose: String, otpCode: String = ""): Resource<LoanApprovalResponse> = safeCall {
        val deviceId = tokenManager.getDeviceId()
        val r = api.applyLoan(LoanApplicationRequest(memberNo, amount, loanType, period, purpose, otpCode, deviceId))
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    /** Loan charge preview before committing to an application */
    suspend fun loanQuote(loanType: String, amount: String, period: Int): Resource<LoanQuoteResponse> = safeCall {
        val r = api.loanQuote(mapOf("loan_type" to loanType, "amount" to amount, "period" to period.toString()))
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    // ── Statements ────────────────────────────────────────────────────────────
    suspend fun getFullStatement(fromDate: String, toDate: String): Resource<FullStatementResponse> = safeCall {
        val r = api.getFullStatement(fromDate, toDate)
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun getAccountStatement(
        accountType: String,
        fromDate: String? = null,
        toDate: String? = null
    ): Resource<AccountStatement> = safeCall {
        val r = api.getAccountStatement(accountType, fromDate, toDate)
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun stkPush(accountType: String, amount: String, accountReference: String = ""): Resource<String> = safeCall {
        val r = api.stkPush(mapOf(
            "account_type" to accountType,
            "amount" to amount,
            "account_reference" to accountReference
        ))
        if (r.isSuccessful && r.body() != null) {
            Resource.Success(r.body()!!["message"] ?: "Payment prompt sent")
        } else Resource.Error(parseError(r.errorBody()?.string()))
    }

    // ── Branding & content ────────────────────────────────────────────────────
    suspend fun getCompanyInfo(): Resource<CompanyInfo> = safeCall {
        val r = api.getCompanyInfo()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun getPromotions(): Resource<List<Promotion>> = safeCall {
        val r = api.getPromotions()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun getShareCapital(): Resource<ShareCapitalResponse> = safeCall {
        val r = api.getShareCapital()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    suspend fun getDividendSlips(): Resource<DividendSlipsListResponse> = safeCall {
        val r = api.getDividendSlips()
        if (r.isSuccessful && r.body() != null) Resource.Success(r.body()!!)
        else Resource.Error(parseError(r.errorBody()?.string()))
    }

    /**
     * Streams the PDF for a dividend slip and saves it to the **shared public
     * Downloads folder** (visible in the system Files / Downloads app).
     *
     *  • Android 10+ (API 29+): writes via `MediaStore.Downloads` — no
     *    runtime permission required, survives uninstall.
     *  • Android 9 and below: writes via `Environment.DIRECTORY_DOWNLOADS`
     *    (legacy public Downloads dir) — requires WRITE_EXTERNAL_STORAGE
     *    which is declared with maxSdkVersion=28 in the manifest.
     *
     * Returns a `PdfSaveResult` containing both the content Uri (preferred
     * for `Intent.ACTION_VIEW`) and the absolute path (for user-facing
     * messages).
     */
    suspend fun downloadDividendSlipPdf(
        detailId: Int,
        context: android.content.Context,
        filename: String,
    ): Resource<PdfSaveResult> = safeCall {
        val r = api.downloadDividendSlipPdf(detailId)
        if (!r.isSuccessful || r.body() == null) {
            return@safeCall Resource.Error(parseError(r.errorBody()?.string()))
        }
        val body = r.body()!!
        val contentType = body.contentType()?.toString().orEmpty()
        if (contentType.contains("json", ignoreCase = true)) {
            val text = body.string()
            return@safeCall Resource.Error(parseError(text))
        }

        val safeName = if (filename.endsWith(".pdf", true)) filename else "$filename.pdf"

        try {
            val saved = savePdfToDownloads(context, body.byteStream(), safeName)
            if (!saved.verified) {
                // Best-effort cleanup of a bad write, then surface real reason
                runCatching { saved.delete(context) }
                return@safeCall Resource.Error(
                    "Server did not return a valid PDF" +
                            (saved.previewText?.let { ": $it" } ?: "")
                )
            }
            Resource.Success(saved)
        } catch (e: Exception) {
            Resource.Error("Could not save PDF: ${e.message ?: "I/O error"}")
        }
    }

    /**
     * Persists the input stream to the shared Downloads folder and returns
     * a [PdfSaveResult] describing where it landed. The file is checked
     * to start with the PDF magic header so we never claim success when
     * the server returned HTML or garbled bytes.
     */
    private fun savePdfToDownloads(
        context: android.content.Context,
        input: java.io.InputStream,
        filename: String,
    ): PdfSaveResult {
        // ── Android 10+ (Q): MediaStore.Downloads — no permission needed ─────
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = android.provider.MediaStore.Downloads
                .getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)

            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_DOWNLOADS)
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values)
                ?: throw java.io.IOException("MediaStore refused to allocate Downloads entry")

            var bytesWritten = 0L
            resolver.openOutputStream(uri).use { out ->
                if (out == null) throw java.io.IOException("Cannot open Downloads output stream")
                input.use { bytesWritten = it.copyTo(out) }
            }

            // Verify it's an actual PDF before publishing
            val verified = streamStartsWithPdfMagic(resolver, uri)
            val preview = if (!verified) firstBytesAsText(resolver, uri, 300) else null

            // Publish to MediaStore so it appears in Files
            values.clear()
            values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            // Build a visible-path string for the user-facing toast
            val path = "Downloads/$filename"
            return PdfSaveResult(
                uri = uri,
                displayPath = path,
                verified = verified,
                previewText = preview,
                bytesWritten = bytesWritten,
                usedMediaStore = true,
            )
        }

        // ── Android 9 and below: legacy public Downloads dir ────────────────
        val downloads = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        if (!downloads.exists()) downloads.mkdirs()
        val outFile = java.io.File(downloads, filename)
        var bytes = 0L
        input.use { src ->
            java.io.FileOutputStream(outFile).use { dst -> bytes = src.copyTo(dst) }
        }
        val verified = startsWithPdfMagic(outFile)
        val preview = if (!verified) runCatching { outFile.readText().take(300) }.getOrNull() else null
        // Notify the media scanner so it appears in Files immediately
        android.media.MediaScannerConnection.scanFile(
            context, arrayOf(outFile.absolutePath), arrayOf("application/pdf"), null
        )
        return PdfSaveResult(
            uri = android.net.Uri.fromFile(outFile),
            displayPath = outFile.absolutePath,
            verified = verified,
            previewText = preview,
            bytesWritten = bytes,
            usedMediaStore = false,
        )
    }

    private fun streamStartsWithPdfMagic(
        resolver: android.content.ContentResolver,
        uri: android.net.Uri
    ): Boolean = try {
        resolver.openInputStream(uri)?.use { ins ->
            val buf = ByteArray(5)
            if (ins.read(buf) != 5) false
            else buf[0] == '%'.code.toByte() && buf[1] == 'P'.code.toByte() &&
                 buf[2] == 'D'.code.toByte() && buf[3] == 'F'.code.toByte() &&
                 buf[4] == '-'.code.toByte()
        } ?: false
    } catch (e: Exception) { false }

    private fun firstBytesAsText(
        resolver: android.content.ContentResolver,
        uri: android.net.Uri,
        max: Int
    ): String? = try {
        resolver.openInputStream(uri)?.use { ins ->
            val buf = ByteArray(max)
            val n = ins.read(buf)
            if (n > 0) String(buf, 0, n, Charsets.UTF_8) else null
        }
    } catch (e: Exception) { null }

    private fun startsWithPdfMagic(file: java.io.File): Boolean {
        return try {
            java.io.FileInputStream(file).use { fis ->
                val buf = ByteArray(5)
                if (fis.read(buf) != 5) return false
                buf[0] == '%'.code.toByte() &&
                buf[1] == 'P'.code.toByte() &&
                buf[2] == 'D'.code.toByte() &&
                buf[3] == 'F'.code.toByte() &&
                buf[4] == '-'.code.toByte()
            }
        } catch (e: Exception) { false }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private suspend fun <T> safeCall(block: suspend () -> Resource<T>): Resource<T> {
        return try { block() }
        catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun parseError(body: String?): String {
        if (body.isNullOrEmpty()) return "Request failed"
        return try {
            val json = JSONObject(body)
            val sb   = StringBuilder()
            if (json.has("detail")) sb.append(json.getString("detail")).append(" ")
            json.keys().forEach { key ->
                if (key == "detail") return@forEach
                val v = json.get(key)
                val text = when (v) {
                    is String -> v
                    else -> v.toString().trim('[', ']', '"')
                }
                sb.append(text).append(" ")
            }
            sb.toString().trim().ifEmpty { "Request failed" }
        } catch (_: Exception) { body.take(300) }
    }
}
