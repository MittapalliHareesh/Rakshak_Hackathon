package com.androidblunders.rakshak.reporting

/**
 * Data class representing extracted financial transaction details from an SMS.
 */
data class TransactionDetails(
    val amount: String? = null,
    val accountInfo: String? = null,
    val referenceNumber: String? = null,
    val dateTime: String? = null
)

/**
 * Utility to parse raw SMS text and extract key financial indicators using Regex.
 */
object TransactionDetailsExtractor {

    private val AMOUNT_REGEX = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d{0,2})""")
    private val ACCOUNT_REGEX = Regex("""(?i)(?:a/c|acct|account)(?:\s*no\.?)?\s*[*xX]*(\d{3,6})""")
    private val UPI_REGEX = Regex("""[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64}""")
    private val REF_NUM_REGEX = Regex("""(?i)(?:ref|txn|transaction|utr)(?:\s*no\.?|\s*id)?\s*[:\-]?\s*([a-zA-Z0-9]{6,16})""")
    
    // Broad regex to determine if a message contains transaction/debit intent
    private val TRANSACTION_KEYWORDS = Regex("""(?i)(debited|credited|deducted|transaction|payment|paid|sent|received)""")

    /**
     * Checks if the given SMS text likely describes a financial transaction.
     */
    fun isTransactionSms(messageBody: String): Boolean {
        return TRANSACTION_KEYWORDS.containsMatchIn(messageBody) || 
               AMOUNT_REGEX.containsMatchIn(messageBody)
    }

    /**
     * Extracts structured transaction details from the given SMS text.
     */
    fun extractDetails(messageBody: String): TransactionDetails {
        val amount = AMOUNT_REGEX.find(messageBody)?.groupValues?.get(1)
        val account = ACCOUNT_REGEX.find(messageBody)?.groupValues?.get(1)
            ?: UPI_REGEX.find(messageBody)?.value
        val refNum = REF_NUM_REGEX.find(messageBody)?.groupValues?.get(1)

        return TransactionDetails(
            amount = amount,
            accountInfo = account,
            referenceNumber = refNum,
            dateTime = null // Optional: parsing exact dates from SMS can be complex and error-prone, leaving null for user to fill
        )
    }
}
