package com.nodimobilebanking.ui.accounts

data class AccountItem(
    val accountCode: String,    // GL account_code (e.g. S02) or loan_no (e.g. LN000001)
    val accountName: String,    // "Fixed Deposit", "Normal Loan"
    val subText: String,        // "fixed_deposit(S01-1256)" or "LN0000026(L01-1256)"
    val balance: Double,
    val isLoan: Boolean,
    val loanNo: String? = null
)
