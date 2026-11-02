package com.nodimobilebanking.data.api

import com.nodimobilebanking.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface SaccoApiService {

    // ── Onboarding (member_no only → OTP → auto-activate) ───────────────────
    @POST("auth/onboard/initiate/")
    suspend fun onboardInitiate(@Body body: Map<String, String>): Response<OtpSentResponse>

    @POST("auth/onboard/verify-otp/")
    suspend fun onboardVerifyOtp(@Body body: Map<String, String>): Response<LoginResponse>

    @POST("auth/onboard/resend-otp/")
    suspend fun onboardResendOtp(@Body body: Map<String, String>): Response<MessageResponse>

    // ── Auth (OTP-only login — no password) ──────────────────────────────────
    @POST("auth/login/")
    suspend fun loginRequestOtp(@Body body: Map<String, String>): Response<OtpSentResponse>

    @POST("auth/login/verify-otp/")
    suspend fun loginVerifyOtp(@Body body: Map<String, String>): Response<LoginResponse>

    // ── Auth (PIN login) ────────────────────────────────────────────────────
    @POST("auth/login/pin/")
    suspend fun loginWithPin(@Body body: Map<String, String>): Response<LoginResponse>

    @POST("auth/set-pin/")
    suspend fun setPin(@Body body: Map<String, String>): Response<MessageResponse>

    @POST("auth/login/refresh/")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<Map<String, String>>

    @POST("auth/logout/")
    suspend fun logout(@Body body: Map<String, String>): Response<MessageResponse>

    // Password Reset (3-step)
    @POST("auth/password-reset/request/")
    suspend fun passwordResetRequest(@Body body: Map<String, String>): Response<PasswordResetRequestResponse>

    @POST("auth/password-reset/verify-otp/")
    suspend fun passwordResetVerifyOtp(@Body body: Map<String, String>): Response<PasswordResetVerifyResponse>

    @POST("auth/password-reset/confirm/")
    suspend fun passwordResetConfirm(@Body body: Map<String, String>): Response<MessageResponse>

    @GET("auth/device-lookup/")
    suspend fun deviceLookup(@Query("device_id") deviceId: String): Response<Map<String, String?>>

    @GET("auth/user/")
    suspend fun getCurrentUser(): Response<UserProfile>

    // ── Member ────────────────────────────────────────────────────────────────
    @GET("members/me/")
    suspend fun getMemberProfile(): Response<MemberProfile>

    @GET("members/kins/")
    suspend fun getNextOfKins(): Response<List<NextOfKin>>

    @GET("members/my-guarantors/")
    suspend fun getMyGuarantors(): Response<List<GuarantorEntry>>

    @GET("members/my-guarantees/")
    suspend fun getMyGuarantees(): Response<List<GuarantorEntry>>

    @GET("members/accounts/")
    suspend fun getMemberAccounts(): Response<MemberAccountsResponse>

    // ── Mobile Loan Types & Limits ────────────────────────────────────────────
    @GET("accounts/mobile-loans/")
    suspend fun getMobileLoanTypes(): Response<List<MobileLoanType>>

    @GET("members/loan-limit/")
    suspend fun getMemberLoanLimit(): Response<LoanLimitResponse>

    // ── Balances ──────────────────────────────────────────────────────────────
    @GET("balances/summary/")
    suspend fun getBalanceSummary(): Response<BalanceSummary>

    // ── Transactions ──────────────────────────────────────────────────────────
    @GET("savings-transactions/")
    suspend fun getSavingsTransactions(): Response<PaginatedTransactions<SavingsTransaction>>

    @GET("loan-transactions/")
    suspend fun getLoanTransactions(): Response<PaginatedTransactions<LoanTransaction>>

    @GET("loan-history/")
    suspend fun getLoanHistory(): Response<PaginatedTransactions<LoanHistory>>

    // ── Actions ───────────────────────────────────────────────────────────────
    @POST("actions/pay/")
    suspend fun makePayment(@Body r: PaymentRequest): Response<ActionResponse>

    @POST("actions/withdraw/")
    suspend fun makeWithdrawal(@Body r: WithdrawRequest): Response<WithdrawalActionResponse>

    @POST("actions/withdrawal-quote/")
    suspend fun withdrawalQuote(@Body body: Map<String, String>): Response<WithdrawalQuote>

    @POST("actions/deposit/")
    suspend fun makeDeposit(@Body r: DepositRequest): Response<DepositResponse>

    @POST("actions/apply-loan/")
    suspend fun applyLoan(@Body r: LoanApplicationRequest): Response<LoanApprovalResponse>

    @POST("actions/loan-quote/")
    suspend fun loanQuote(@Body body: Map<String, String>): Response<LoanQuoteResponse>

    // M-Pesa STK Push (payment prompt)
    @POST("actions/stk-push/")
    suspend fun stkPush(@Body body: Map<String, String>): Response<Map<String, String>>

    // ── Statements ────────────────────────────────────────────────────────────
    @GET("statements/full/")
    suspend fun getFullStatement(
        @Query("from_date") fromDate: String,
        @Query("to_date") toDate: String
    ): Response<FullStatementResponse>

    @GET("statements/{account_type}/")
    suspend fun getAccountStatement(
        @Path("account_type") accountType: String,
        @Query("from_date") fromDate: String? = null,
        @Query("to_date") toDate: String? = null
    ): Response<AccountStatement>

    // ── Branding & Content ────────────────────────────────────────────────────
    @GET("company-info/")
    suspend fun getCompanyInfo(): Response<CompanyInfo>

    @GET("promotions/")
    suspend fun getPromotions(): Response<List<Promotion>>

    @GET("share-capital/")
    suspend fun getShareCapital(): Response<ShareCapitalResponse>

    @GET("dividend-slips/")
    suspend fun getDividendSlips(): Response<DividendSlipsListResponse>

    @Streaming
    @GET("dividend-slips/{detailId}/pdf/")
    suspend fun downloadDividendSlipPdf(
        @Path("detailId") detailId: Int
    ): Response<okhttp3.ResponseBody>
}
