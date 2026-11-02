package com.nodimobilebanking.data.models

import com.google.gson.annotations.SerializedName

// ─── Onboarding ───────────────────────────────────────────────────────────────
data class OnboardInitRequest(@SerializedName("member_no") val memberNo: String, val email: String)
data class OtpVerifyRequest(@SerializedName("member_no") val memberNo: String, val email: String, @SerializedName("otp_code") val otpCode: String)
data class SetPasswordRequest(@SerializedName("member_no") val memberNo: String, val email: String, val password: String, @SerializedName("password_confirm") val passwordConfirm: String, @SerializedName("device_id") val deviceId: String = "")
data class ResendOtpRequest(@SerializedName("member_no") val memberNo: String, val email: String)
data class OnboardCompleteResponse(val message: String, val access: String, val refresh: String, val username: String, val email: String)

// ─── Auth ──────────────────────────────────────────────────────────────────────
data class LoginRequest(val email: String, val password: String, @SerializedName("device_id") val deviceId: String = "")
data class LoginResponse(val access: String, val refresh: String, val username: String, val email: String)

// ─── User ──────────────────────────────────────────────────────────────────────
data class UserProfile(
    val id: Int, val username: String, val email: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name")  val lastName: String,
    val phone: String, val role: String
)

// ─── Member ──────────────────────────────────────────────────────────────────
data class MemberProfile(
    val id: Int,
    @SerializedName("member_no")        val memberNo: String,
    @SerializedName("full_name")        val fullName: String,
    @SerializedName("first_name")       val firstName: String?,
    @SerializedName("middle_name")      val middleName: String?,
    @SerializedName("last_name")        val lastName: String?,
    @SerializedName("gender")           val gender: String?,
    @SerializedName("dob")              val dob: String?,
    @SerializedName("phone")           val mobile: String,
    @SerializedName("town")             val town: String?,
    @SerializedName("national_id")      val nationalId: String,
    @SerializedName("reg_email")        val regEmail: String?,
    @SerializedName("member_type")      val memberType: String,
    @SerializedName("member_status")    val memberStatus: String,
    @SerializedName("reg_date")         val regDate: String,
    @SerializedName("kra_pin")          val kraPin: String?
) {
    val displayName: String get() {
        val fn = firstName?.trim() ?: ""
        val ln = lastName?.trim()  ?: ""
        return when {
            fn.isNotEmpty() && ln.isNotEmpty() -> "$fn $ln"
            fn.isNotEmpty()                    -> fn
            fullName.isNotEmpty()              -> fullName
            else                               -> "Member"
        }
    }
    val displayEmail: String get() = regEmail?.ifBlank { null } ?: "—"
}

// ─── Next of Kin ──────────────────────────────────────────────────────────────
data class NextOfKin(
    val id: Int,
    @SerializedName("kin_name")         val kinName: String,
    @SerializedName("kin_relationship") val kinRelationship: String,
    @SerializedName("kin_phone")       val kinMobile: String?,
    @SerializedName("kin_national_id")  val kinNationalId: String?,
    val gender: String?,
    @SerializedName("kin_dob")          val kinDob: String?
)

// ─── Member Accounts ─────────────────────────────────────────────────────────
data class MemberAccount(
    val id: String,
    val text: String? = "",
    @SerializedName("account_code") val accountCode: String? = "",
    @SerializedName("acc_initials") val accInitials: String? = "",
    @SerializedName("account_name") val accountName: String? = "",
    @SerializedName("sub_text") val subText: String? = "",
    @SerializedName("is_loan") val isLoan: Boolean = false,
    @SerializedName("is_withdrawable") val isWithdrawable: Boolean = false,
    val balance: Double = 0.0
)
data class MemberAccountsResponse(val results: List<MemberAccount>)

// ─── Mobile Loan Types (from GLAccountsSetup where is_mobile_loan=True) ──────
data class LoanChargeItem(
    val name: String,
    @SerializedName("charge_type") val chargeType: String,
    val amount: Double,
    @SerializedName("is_mandatory") val isMandatory: Boolean
)

data class MobileLoanType(
    val id: Int,
    @SerializedName("account_name")          val accountName: String,
    @SerializedName("account_type")          val accountType: String,
    @SerializedName("max_repayment_period")  val maxRepaymentPeriod: Int,
    @SerializedName("interest_calc_method")  val interestCalcMethod: String?,
    @SerializedName("interest_rate")         val interestRate: Double = 12.0,
    @SerializedName("member_limit")          val memberLimit: Double,
    @SerializedName("available_limit")       val availableLimit: Double,
    val charges: List<LoanChargeItem> = emptyList()
)

// Loan application response with charges breakdown
data class LoanApprovalResponse(
    val message: String,
    @SerializedName("otp_sent") val otpSent: Boolean = false,
    @SerializedName("loan_number")        val loanNumber: String?,
    val principal: Double?,
    val charges: List<Map<String, Any>>?,
    @SerializedName("total_charges")      val totalCharges: Double?,
    @SerializedName("net_disbursed")      val netDisbursed: Double?,
    val installment: Double?,
    val period: Int?,
    @SerializedName("disbursement_status") val disbursementStatus: String?
)

// ─── Loan Quote (charge preview before applying) ─────────────────────────────
data class LoanQuoteResponse(
    val message: String = "",
    @SerializedName("loan_type")        val loanType: String = "",
    @SerializedName("account_name")     val accountName: String = "",
    val principal: Double = 0.0,
    val charges: List<Map<String, Any>> = emptyList(),
    @SerializedName("total_charges")    val totalCharges: Double = 0.0,
    @SerializedName("net_disbursed")    val netDisbursed: Double = 0.0,
    val installment: Double = 0.0,
    val period: Int = 0,
    @SerializedName("interest_rate")    val interestRate: Double = 0.0,
    @SerializedName("available_limit")  val availableLimit: Double = 0.0
)

// ─── Loan Limit (from LoanLimitGraduation) ────────────────────────────────────
data class LoanLimitResponse(
    @SerializedName("member_no")        val memberNo: String,
    @SerializedName("available_limit")  val availableLimit: Double,
    @SerializedName("last_updated")     val lastUpdated: String?,
    val message: String?
)

// ─── Transactions — DRF ViewSet returns paginated {count, results:[...]} ──────
data class PaginatedTransactions<T>(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T> = emptyList()
)

data class SavingsTransaction(
    val id: Int,
    @SerializedName("member_no")     val memberNo: String,
    @SerializedName("saving_type")   val savingType: String,
    @SerializedName("tr_date")       val trDate: String,
    @SerializedName("tr_ref")        val trRef: String,
    @SerializedName("ext_ref")       val extRef: String?,
    @SerializedName("tr_desc")       val trDesc: String,
    @SerializedName("debit_amount")  val debitAmount: String,
    @SerializedName("credit_amount") val creditAmount: String,
    @SerializedName("created_by")    val createdBy: String?,
    @SerializedName("created_at")    val createdAt: String
)

data class LoanTransaction(
    val id: Int,
    @SerializedName("member_no")     val memberNo: String,
    @SerializedName("loan_id")       val loanId: Int?,
    @SerializedName("loan_no")       val loanNo: String?,
    @SerializedName("loan_type")     val loanType: String,
    @SerializedName("tr_date")       val trDate: String,
    @SerializedName("tr_ref")        val trRef: String,
    @SerializedName("ext_ref")       val extRef: String?,
    @SerializedName("tr_desc")       val trDesc: String,
    @SerializedName("debit_amount")  val debitAmount: String,
    @SerializedName("credit_amount") val creditAmount: String,
    @SerializedName("created_by")    val createdBy: String?,
    @SerializedName("created_at")    val createdAt: String
)

data class LoanHistory(
    val id: Int,
    @SerializedName("loan_no")        val loanNo: String,
    @SerializedName("member_no")      val memberNo: String,
    @SerializedName("loan_date")      val loanDate: String,
    val principal: String,
    val installment: String,
    @SerializedName("loan_type")      val loanType: String,
    @SerializedName("loan_period")    val loanPeriod: Int,
    @SerializedName("interest_rate")  val interestRate: String,
    @SerializedName("net_disbursed")  val netDisbursed: String,
    @SerializedName("processing_fee") val processingFee: String,
    @SerializedName("insurance_fee")  val insuranceFee: String,
    @SerializedName("other_charges")  val otherCharges: String,
    @SerializedName("created_by")     val createdBy: String?,
    @SerializedName("created_at")     val createdAt: String
)

// ─── Balance ──────────────────────────────────────────────────────────────────
data class BalanceSummary(
    @SerializedName("savings_balance")      val savingsBalance: Double,
    @SerializedName("loan_balance")         val loanBalance: Double,
    @SerializedName("total_balance")        val totalBalance: Double,
    @SerializedName("savings_transactions") val savingsTransactions: List<SavingsTransaction>,
    @SerializedName("loan_transactions")    val loanTransactions: List<LoanTransaction>
)

// ─── Actions ──────────────────────────────────────────────────────────────────
data class PaymentRequest(
    @SerializedName("member_no")   val memberNo: String,
    @SerializedName("saving_type") val savingType: String,
    val amount: String,
    val description: String
)
data class WithdrawRequest(
    @SerializedName("member_no")   val memberNo: String,
    @SerializedName("saving_type") val savingType: String,
    val amount: String,
    val description: String,
    @SerializedName("otp_code")    val otpCode: String = "",
    @SerializedName("device_id")   val deviceId: String = ""
)

// Withdrawal: response covers BOTH the "OTP sent" step and the final disbursement.
// Step 1 → otpSent = true, preview populated.
// Step 2 → otpSent = false (or absent), message + reference + conversationId populated.
data class WithdrawalActionResponse(
    val message: String = "",
    @SerializedName("otp_sent")        val otpSent: Boolean = false,
    val preview: WithdrawalQuote? = null,
    val reference: String? = null,
    val amount: Double? = null,
    @SerializedName("total_charges")   val totalCharges: Double? = null,
    @SerializedName("gross_debit")     val grossDebit: Double? = null,
    @SerializedName("conversation_id") val conversationId: String? = null
)

// Unified deposit — works for both savings and loan repayment
data class DepositRequest(
    @SerializedName("member_no")    val memberNo: String,
    @SerializedName("account_type") val accountType: String,
    val amount: String,
    val description: String
)
data class DepositResponse(
    val message: String,
    val type: String   // "Deposit" or "Loan Repayment"
)

data class LoanApplicationRequest(
    @SerializedName("member_no") val memberNo: String,
    val amount: String,
    @SerializedName("loan_type") val loanType: String,
    val period: Int,
    val purpose: String,
    @SerializedName("otp_code")  val otpCode: String = "",
    @SerializedName("device_id") val deviceId: String = ""
)
data class ActionResponse(val message: String)
data class MessageResponse(val message: String)

// ─── Full Statement (grouped by account) ──────────────────────────────────────
data class StatementLine(
    val date: String,
    val ref: String,
    val description: String,
    val debit: Double?,
    val credit: Double?,
    val balance: Double
)

data class AccountStatement(
    @SerializedName("company_info")            val companyInfo: CompanyInfo? = null,
    @SerializedName("account_type")            val accountType: String,
    @SerializedName("account_name")            val accountName: String,
    @SerializedName("period_from")             val periodFrom: String,
    @SerializedName("period_to")               val periodTo: String,
    @SerializedName("balance_brought_forward") val balanceBroughtForward: Double,
    val lines: List<StatementLine>,
    @SerializedName("closing_balance")         val closingBalance: Double
)

data class FullStatementResponse(
    @SerializedName("company_info")   val companyInfo: CompanyInfo? = null,
    @SerializedName("member_no")      val memberNo: String,
    @SerializedName("full_name")      val fullName: String,
    @SerializedName("statement_date") val statementDate: String,
    @SerializedName("period_from")    val periodFrom: String,
    @SerializedName("period_to")      val periodTo: String,
    val accounts: List<AccountStatement>
)

// ─── Guarantors & Guarantees ─────────────────────────────────────────────────
data class GuarantorEntry(
    val id: Int,
    @SerializedName("loan_no")         val loanNo: String,
    @SerializedName("loan_date")       val loanDate: String,
    @SerializedName("loan_principal")  val loanPrincipal: String,
    @SerializedName("loan_type")       val loanType: String,
    val amount: String,
    @SerializedName("borrower_name")       val borrowerName: String,
    @SerializedName("borrower_member_no")  val borrowerMemberNo: String,
    @SerializedName("guarantor_name")      val guarantorName: String,
    @SerializedName("guarantor_member_no") val guarantorMemberNo: String
)

// ─── Branding (CompanyInfo) ──────────────────────────────────────────────────
data class CompanyInfo(
    @SerializedName("company_name")     val companyName: String = "",
    @SerializedName("company_address")  val companyAddress: String = "",
    @SerializedName("company_contact")  val companyContact: String = "",
    @SerializedName("company_location") val companyLocation: String = "",
    @SerializedName("company_footer")   val companyFooter: String = "Company MIS",
    @SerializedName("company_logo_url") val companyLogoUrl: String = ""
)

// ─── Promotions (dashboard carousel) ─────────────────────────────────────────
data class Promotion(
    val id: Int,
    val title: String,
    val subtitle: String = "",
    val body: String = "",
    @SerializedName("image_url")           val imageUrl: String = "",
    @SerializedName("promo_type")          val promoType: String = "ad",
    @SerializedName("promo_type_display")  val promoTypeDisplay: String = "",
    @SerializedName("cta_label")           val ctaLabel: String = "",
    @SerializedName("cta_url")             val ctaUrl: String = ""
)

// ─── Share Capital ───────────────────────────────────────────────────────────
data class ShareCapitalResponse(
    @SerializedName("has_share_capital") val hasShareCapital: Boolean,
    @SerializedName("account_code")      val accountCode: String = "",
    @SerializedName("account_name")      val accountName: String = "Share Capital",
    @SerializedName("account_type")      val accountType: String = "",
    val balance: Double = 0.0,
    @SerializedName("transaction_count") val transactionCount: Int = 0,
    val message: String? = null
)

// ─── Password Reset (3-step flow) ────────────────────────────────────────────
data class PasswordResetRequestResponse(
    val message: String,
    @SerializedName("member_no")    val memberNo: String? = null,
    @SerializedName("masked_email") val maskedEmail: String = "",
    @SerializedName("masked_phone") val maskedPhone: String = ""
)

data class PasswordResetVerifyResponse(
    val message: String,
    @SerializedName("reset_token") val resetToken: String
)

// ─── Dividend Slip ───────────────────────────────────────────────────────────
// ─── Dividend Slips (list + per-slip PDF download) ──────────────────────────
data class DividendSlipsListResponse(
    @SerializedName("has_dividends")  val hasDividends: Boolean = false,
    @SerializedName("member_no")      val memberNo: String? = null,
    @SerializedName("member_name")    val memberName: String? = null,
    val count: Int = 0,
    val slips: List<DividendSlipRow> = emptyList()
)

data class DividendSlipRow(
    val id: Int,
    @SerializedName("batch_no")              val batchNo: String,
    @SerializedName("saving_type")           val savingType: String,
    @SerializedName("saving_type_display")   val savingTypeDisplay: String,
    @SerializedName("cut_off_date")          val cutOffDate: String,
    val year: Int,
    @SerializedName("is_posted")             val isPosted: Boolean = false,
    @SerializedName("gross_interest")        val grossInterest: Double,
    @SerializedName("withholding_tax")       val withholdingTax: Double,
    @SerializedName("processing_fee")        val processingFee: Double,
    @SerializedName("net_payout")            val netPayout: Double
)

// ─── OTP-only auth responses ─────────────────────────────────────────────────
data class OtpSentResponse(
    val message: String,
    @SerializedName("masked_email") val maskedEmail: String = "",
    @SerializedName("masked_phone") val maskedPhone: String = "",
    val code: String? = null
)

// ─── Withdrawal Quote (charge preview before B2C) ───────────────────────────
data class WithdrawalQuote(
    @SerializedName("account_name")          val accountName: String = "",
    val balance: Double = 0.0,
    val amount: Double = 0.0,
    @SerializedName("band_fee")              val bandFee: Double = 0.0,
    @SerializedName("extra_charges")         val extraCharges: List<ExtraCharge> = emptyList(),
    @SerializedName("total_charges")         val totalCharges: Double = 0.0,
    @SerializedName("net_received")          val netReceived: Double = 0.0,
    @SerializedName("gross_debit")           val grossDebit: Double = 0.0,
    @SerializedName("insufficient_balance")  val insufficientBalance: Boolean = false,
    val message: String = ""
)

data class ExtraCharge(
    val name: String = "",
    val code: String = "",
    val amount: Double = 0.0
)

// ─── PDF save result (shared Downloads folder) ──────────────────────────────
data class PdfSaveResult(
    val uri: android.net.Uri,
    val displayPath: String,
    val verified: Boolean,
    val previewText: String? = null,
    val bytesWritten: Long = 0L,
    val usedMediaStore: Boolean = true,
) {
    fun delete(context: android.content.Context) {
        runCatching {
            if (usedMediaStore) context.contentResolver.delete(uri, null, null)
            else java.io.File(displayPath).delete()
        }
    }
}
