package com.nodimobilebanking.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import com.nodimobilebanking.data.models.AccountStatement
import com.nodimobilebanking.data.models.MemberProfile
import com.nodimobilebanking.data.models.FullStatementResponse
import com.nodimobilebanking.data.models.LoanTransaction
import com.nodimobilebanking.data.models.SavingsTransaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generates PDF statements matching the core banking format:
 * - Company header
 * - Member info
 * - Grouped by account type (Fixed Deposit, Savings, Share Capital, Loan LNXXXXXX...)
 * - Running balance per section
 */
object PdfStatementGenerator {

    /**
     * Copy a generated PDF to the public Downloads folder so the user can find it.
     * Works on Android 10+ (MediaStore) and older (direct file access).
     */
    fun copyToDownloads(context: Context, sourceFile: File): File {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — use MediaStore
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { out ->
                    sourceFile.inputStream().use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(it, values, null, null)
            }
            return sourceFile
        } else {
            // Older Android — copy directly
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)
            return destFile
        }
    }

    private val TITLE   = Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, BaseColor(44, 62, 80))
    private val HDR     = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor(44, 62, 80))
    private val SEC     = Font(Font.FontFamily.HELVETICA, 11f, Font.BOLDITALIC, BaseColor.BLACK)
    private val TH      = Font(Font.FontFamily.HELVETICA, 9f, Font.BOLD, BaseColor.BLACK)
    private val TD      = Font(Font.FontFamily.HELVETICA, 8.5f, Font.NORMAL, BaseColor.BLACK)
    private val LBL     = Font(Font.FontFamily.HELVETICA, 9f, Font.BOLD)
    private val VAL     = Font(Font.FontFamily.HELVETICA, 9f, Font.NORMAL)
    private val FOOTER  = Font(Font.FontFamily.HELVETICA, 8f, Font.ITALIC, BaseColor.GRAY)

    private val HEADER_BG = BaseColor(248, 249, 249)
    private val LINE_CLR  = BaseColor.LIGHT_GRAY

    /**
     * Generate consolidated statement from FullStatementResponse (API data).
     */
    fun generateConsolidatedStatement(
        context: Context,
        member: MemberProfile,
        statement: FullStatementResponse,
        companyName: String = "SACCO MIS",
        companyAddress: String = "",
        companyContact: String = "",
        companyLocation: String = ""
    ): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "Full_Statement_${member.memberNo}_$ts.pdf"
        )

        val doc = Document(PageSize.A4, 40f, 40f, 30f, 40f)
        PdfWriter.getInstance(doc, FileOutputStream(file))
        doc.open()

        // ── HEADER ──
        doc.add(Paragraph(companyName.uppercase(), TITLE).apply { alignment = Element.ALIGN_CENTER })
        val subParts = listOfNotNull(
            companyAddress.takeIf { it.isNotBlank() },
            companyContact.takeIf { it.isNotBlank() },
            companyLocation.takeIf { it.isNotBlank() }
        )
        if (subParts.isNotEmpty()) {
            doc.add(Paragraph(subParts.joinToString("  |  "), FOOTER).apply {
                alignment = Element.ALIGN_CENTER; spacingBefore = 2f
            })
        }
        doc.add(Paragraph("CONSOLIDATED MEMBER STATEMENT", HDR).apply { alignment = Element.ALIGN_CENTER; spacingBefore = 4f })
        doc.add(addLine())
        doc.add(Chunk.NEWLINE)

        // ── MEMBER INFO ──
        val info = PdfPTable(4).apply { widthPercentage = 100f; setWidths(floatArrayOf(1.2f, 2.5f, 1f, 2f)) }
        info.addCell(lbl("Member:")); info.addCell(value(member.fullName.uppercase()))
        info.addCell(lbl("ID No:")); info.addCell(value(member.nationalId))
        info.addCell(lbl("Member No:")); info.addCell(value("${member.memberNo}"))
        info.addCell(lbl("Mobile:")); info.addCell(value(member.mobile))
        info.addCell(lbl("Generated:")); info.addCell(value(
            SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
        ))
        info.addCell(lbl("")); info.addCell(value(""))
        doc.add(info)
        doc.add(Chunk.NEWLINE)

        // ── ACCOUNT SECTIONS ──
        if (statement.accounts.isEmpty()) {
            doc.add(Paragraph("No transactions found for the selected period.", VAL))
        } else {
            for (account in statement.accounts) {
                addAccountSection(doc, account)
            }
        }

        // ── FOOTER ──
        doc.add(Chunk.NEWLINE)
        doc.add(addLine())
        doc.add(Paragraph("End of consolidated report.", FOOTER).apply { spacingBefore = 5f })

        doc.close()
        return file
    }

    /**
     * Generate statement for a single account (from Accounts click).
     */
    fun generateAccountStatement(
        context: Context,
        member: MemberProfile,
        account: AccountStatement,
        companyName: String = "SACCO MIS",
        companyAddress: String = "",
        companyContact: String = "",
        companyLocation: String = ""
    ): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "Statement_${member.memberNo}_${account.accountType}_$ts.pdf"
        )

        val doc = Document(PageSize.A4, 40f, 40f, 30f, 40f)
        PdfWriter.getInstance(doc, FileOutputStream(file))
        doc.open()

        doc.add(Paragraph(companyName.uppercase(), TITLE).apply { alignment = Element.ALIGN_CENTER })
        val subParts = listOfNotNull(
            companyAddress.takeIf { it.isNotBlank() },
            companyContact.takeIf { it.isNotBlank() },
            companyLocation.takeIf { it.isNotBlank() }
        )
        if (subParts.isNotEmpty()) {
            doc.add(Paragraph(subParts.joinToString("  |  "), FOOTER).apply {
                alignment = Element.ALIGN_CENTER; spacingBefore = 2f
            })
        }
        doc.add(Paragraph("ACCOUNT STATEMENT", HDR).apply { alignment = Element.ALIGN_CENTER; spacingBefore = 4f })
        doc.add(addLine())
        doc.add(Chunk.NEWLINE)

        val info = PdfPTable(4).apply { widthPercentage = 100f; setWidths(floatArrayOf(1.2f, 2.5f, 1f, 2f)) }
        info.addCell(lbl("Member:")); info.addCell(value(member.fullName.uppercase()))
        info.addCell(lbl("Account:")); info.addCell(value(account.accountName))
        info.addCell(lbl("Member No:")); info.addCell(value("${member.memberNo}"))
        info.addCell(lbl("Period:")); info.addCell(value("${account.periodFrom} to ${account.periodTo}"))
        doc.add(info)
        doc.add(Chunk.NEWLINE)

        addAccountSection(doc, account)

        doc.add(Chunk.NEWLINE)
        doc.add(addLine())
        doc.add(Paragraph("End of statement.", FOOTER).apply { spacingBefore = 5f })

        doc.close()
        return file
    }

    /**
     * Legacy method — generates from raw transaction lists (balance summary).
     */
    fun generateStatement(
        context: Context,
        member: MemberProfile,
        savingsTransactions: List<SavingsTransaction>,
        loanTransactions: List<LoanTransaction>,
        savingsBalance: Double,
        loanBalance: Double,
        companyName: String = "SACCO MIS",
        companyAddress: String = "",
        companyContact: String = "",
        companyLocation: String = ""
    ): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "Statement_${member.memberNo}_$ts.pdf"
        )

        val doc = Document(PageSize.A4, 40f, 40f, 30f, 40f)
        PdfWriter.getInstance(doc, FileOutputStream(file))
        doc.open()

        doc.add(Paragraph(companyName.uppercase(), TITLE).apply { alignment = Element.ALIGN_CENTER })
        val subParts = listOfNotNull(
            companyAddress.takeIf { it.isNotBlank() },
            companyContact.takeIf { it.isNotBlank() },
            companyLocation.takeIf { it.isNotBlank() }
        )
        if (subParts.isNotEmpty()) {
            doc.add(Paragraph(subParts.joinToString("  |  "), FOOTER).apply {
                alignment = Element.ALIGN_CENTER; spacingBefore = 2f
            })
        }
        doc.add(Paragraph("CONSOLIDATED MEMBER STATEMENT", HDR).apply { alignment = Element.ALIGN_CENTER; spacingBefore = 4f })
        doc.add(addLine())
        doc.add(Chunk.NEWLINE)

        val info = PdfPTable(4).apply { widthPercentage = 100f; setWidths(floatArrayOf(1.2f, 2.5f, 1f, 2f)) }
        info.addCell(lbl("Member:")); info.addCell(value(member.fullName.uppercase()))
        info.addCell(lbl("Member No:")); info.addCell(value("${member.memberNo}"))
        info.addCell(lbl("Mobile:")); info.addCell(value(member.mobile))
        info.addCell(lbl("Generated:")); info.addCell(value(
            SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
        ))
        doc.add(info)
        doc.add(Chunk.NEWLINE)

        // Group savings by type
        val savByType = savingsTransactions.groupBy { it.savingType }
        for ((sType, txns) in savByType) {
            val name = sType.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            doc.add(sectionTitle(name))
            val table = transactionTable()
            var bal = 0.0
            for (t in txns.sortedBy { it.trDate }) {
                val cr = t.creditAmount.toDoubleOrNull() ?: 0.0
                val dr = t.debitAmount.toDoubleOrNull() ?: 0.0
                bal += (cr - dr)
                table.addCell(td(t.trDate.formatDate()))
                table.addCell(td(t.trDesc.take(40)))
                table.addCell(tdRight(if (dr > 0) dr.formatAmount() else "-"))
                table.addCell(tdRight(if (cr > 0) cr.formatAmount() else "-"))
                table.addCell(tdRight(bal.formatAmount()))
            }
            doc.add(table)
            doc.add(Chunk.NEWLINE)
        }

        // Group loans by loan_no
        val loanByNo = loanTransactions.groupBy { it.loanNo ?: it.loanType }
        for ((lNo, txns) in loanByNo) {
            val label = if (lNo.startsWith("LN") || lNo.startsWith("MOBI")) "Loan $lNo" else lNo.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            doc.add(sectionTitle(label))
            val table = transactionTable()
            var bal = 0.0
            for (t in txns.sortedBy { it.trDate }) {
                val dr = t.debitAmount.toDoubleOrNull() ?: 0.0
                val cr = t.creditAmount.toDoubleOrNull() ?: 0.0
                bal += (dr - cr)
                table.addCell(td(t.trDate.formatDate()))
                table.addCell(td(t.trDesc.take(40)))
                table.addCell(tdRight(if (dr > 0) dr.formatAmount() else "-"))
                table.addCell(tdRight(if (cr > 0) cr.formatAmount() else "-"))
                table.addCell(tdRight(bal.formatAmount()))
            }
            doc.add(table)
            doc.add(Chunk.NEWLINE)
        }

        doc.add(addLine())
        doc.add(Paragraph("End of consolidated report.", FOOTER))
        doc.close()
        return file
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun addAccountSection(doc: Document, account: AccountStatement) {
        doc.add(sectionTitle(account.accountName))

        val table = transactionTable()

        // Balance Brought Forward row
        if (account.balanceBroughtForward != 0.0) {
            table.addCell(td(account.periodFrom))
            table.addCell(td("Balance Brought Forward"))
            table.addCell(tdRight("-"))
            table.addCell(tdRight("-"))
            table.addCell(tdRight(account.balanceBroughtForward.formatAmount()))
        }

        var totalDebit = 0.0
        var totalCredit = 0.0
        for (line in account.lines) {
            table.addCell(td(line.date))
            table.addCell(td(line.description.take(40)))
            val dr = line.debit ?: 0.0
            val cr = line.credit ?: 0.0
            if (dr > 0) totalDebit += dr
            if (cr > 0) totalCredit += cr
            table.addCell(tdRight(if (dr > 0) dr.formatAmount() else "-"))
            table.addCell(tdRight(if (cr > 0) cr.formatAmount() else "-"))
            table.addCell(tdRight(line.balance.formatAmount()))
        }

        // Closing Balance row
        val closingBg = BaseColor(0xF0, 0xF4, 0xF8)
        table.addCell(PdfPCell(Phrase("", TD)).apply { borderColor = LINE_CLR; backgroundColor = closingBg })
        table.addCell(PdfPCell(Phrase("Closing Balance", TH)).apply { borderColor = LINE_CLR; backgroundColor = closingBg; paddingTop = 4f; paddingBottom = 4f; paddingLeft = 3f })
        table.addCell(PdfPCell(Phrase(totalDebit.formatAmount(), TH)).apply { borderColor = LINE_CLR; backgroundColor = closingBg; horizontalAlignment = Element.ALIGN_RIGHT; paddingTop = 4f; paddingBottom = 4f; paddingRight = 3f })
        table.addCell(PdfPCell(Phrase(totalCredit.formatAmount(), TH)).apply { borderColor = LINE_CLR; backgroundColor = closingBg; horizontalAlignment = Element.ALIGN_RIGHT; paddingTop = 4f; paddingBottom = 4f; paddingRight = 3f })
        table.addCell(PdfPCell(Phrase(account.closingBalance.formatAmount(), TH)).apply { borderColor = LINE_CLR; backgroundColor = closingBg; horizontalAlignment = Element.ALIGN_RIGHT; paddingTop = 4f; paddingBottom = 4f; paddingRight = 3f })

        doc.add(table)
        doc.add(Chunk.NEWLINE)
    }

    private fun sectionTitle(title: String): Paragraph {
        return Paragraph(title, SEC).apply {
            spacingBefore = 10f; spacingAfter = 4f
        }
    }

    private fun transactionTable(): PdfPTable {
        val t = PdfPTable(5).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(1.2f, 2.8f, 1.1f, 1.1f, 1.5f))
        }
        arrayOf("Date", "Description", "Debit", "Credit", "Running Balance").forEach { h ->
            t.addCell(PdfPCell(Phrase(h, TH)).apply {
                backgroundColor = HEADER_BG
                paddingTop = 4f; paddingBottom = 4f; paddingLeft = 3f
                borderColor = LINE_CLR
            })
        }
        return t
    }

    private fun addLine(): PdfPTable {
        val t = PdfPTable(1).apply { widthPercentage = 100f; spacingBefore = 3f; spacingAfter = 3f }
        t.addCell(PdfPCell().apply { fixedHeight = 1f; backgroundColor = BaseColor(52, 73, 94); border = Rectangle.NO_BORDER })
        return t
    }

    private fun lbl(text: String) = PdfPCell(Phrase(text, LBL)).apply {
        paddingTop = 3f; paddingBottom = 3f; border = Rectangle.NO_BORDER
    }

    private fun value(text: String) = PdfPCell(Phrase(text, VAL)).apply {
        paddingTop = 3f; paddingBottom = 3f; border = Rectangle.NO_BORDER
    }

    private fun td(text: String) = PdfPCell(Phrase(text, TD)).apply {
        paddingTop = 3f; paddingBottom = 3f; paddingLeft = 3f; borderColor = LINE_CLR
    }

    private fun tdRight(text: String) = PdfPCell(Phrase(text, TD)).apply {
        paddingTop = 3f; paddingBottom = 3f; paddingRight = 3f
        horizontalAlignment = Element.ALIGN_RIGHT; borderColor = LINE_CLR
    }
}
