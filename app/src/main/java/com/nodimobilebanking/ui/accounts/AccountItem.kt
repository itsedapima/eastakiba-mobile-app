package com.nodimobilebanking.ui.accounts

data class AccountItem(
    val accountCode: String,    // account_type or loan_no (used for statement lookup)
    val accountName: String,    // "Fixed Deposit", "Normal Loan"
    val subText: String,        // "fixed_deposit(S01-1256)" or "LN0000026(L01-1256)"
    val balance: Double,
    val isLoan: Boolean,
    val loanNo: String? = null
)
