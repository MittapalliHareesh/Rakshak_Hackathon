package com.androidblunders.rakshak.reporting

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Helper class to draft an email to the Cyber Police using Android Intents.
 */
object CyberPoliceReporter {

    // Placeholder cyber police email address (can be made configurable later)
    private const val CYBER_POLICE_EMAIL = "report@cybercrime.gov.in"

    /**
     * Drafts an email to the cyber police with the extracted details from a suspicious SMS.
     * Launches the user's default email client.
     */
    fun draftEmail(
        context: Context,
        sender: String,
        messageBody: String,
        transactionDetails: TransactionDetails? = null
    ) {
        val subject = "Suspected Cyber Fraud / Spam SMS Report - Sender: $sender"
        
        val bodyBuilder = StringBuilder()
        bodyBuilder.append("Dear Cyber Crime Cell,\n\n")
        bodyBuilder.append("I would like to report a suspected fraudulent SMS received on my device.\n\n")
        bodyBuilder.append("--- SMS DETAILS ---\n")
        bodyBuilder.append("Sender: $sender\n")
        bodyBuilder.append("Message: $messageBody\n\n")
        
        if (transactionDetails != null) {
            bodyBuilder.append("--- EXTRACTED TRANSACTION DETAILS ---\n")
            transactionDetails.amount?.let { bodyBuilder.append("Amount Involved: Rs. $it\n") }
            transactionDetails.accountInfo?.let { bodyBuilder.append("Account/UPI ID Involved: $it\n") }
            transactionDetails.referenceNumber?.let { bodyBuilder.append("Reference Number: $it\n") }
            bodyBuilder.append("\n")
        }
        
        bodyBuilder.append("Please investigate this matter.\n\n")
        bodyBuilder.append("Sincerely,\n[Your Name]")

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // Only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf(CYBER_POLICE_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, bodyBuilder.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Try to start the activity. If no email client is installed, this could throw an exception,
        // so it's best to wrap it in a try-catch.
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // In a production app, we would show a Toast here letting the user know no email client was found.
        }
    }
}
