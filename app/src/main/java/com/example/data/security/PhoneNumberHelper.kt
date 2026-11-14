package com.example.data.security

data class CountryCodeOption(
    val isoCode: String,
    val name: String,
    val dialCode: String
)

object PhoneNumberHelper {

    val supportedCountries = listOf(
        CountryCodeOption("US", "United States", "+1"),
        CountryCodeOption("GB", "United Kingdom", "+44"),
        CountryCodeOption("PK", "Pakistan", "+92"),
        CountryCodeOption("IN", "India", "+91"),
        CountryCodeOption("AE", "United Arab Emirates", "+971"),
        CountryCodeOption("SA", "Saudi Arabia", "+966"),
        CountryCodeOption("CA", "Canada", "+1"),
        CountryCodeOption("AU", "Australia", "+61"),
        CountryCodeOption("DE", "Germany", "+49"),
        CountryCodeOption("NG", "Nigeria", "+234"),
        CountryCodeOption("PH", "Philippines", "+63"),
        CountryCodeOption("SG", "Singapore", "+65")
    )

    fun normalizeToE164(rawNumber: String, defaultDialCode: String): Result<String> {
        val trimmed = rawNumber.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a phone number to check."))
        }

        val cleanDigits = trimmed.filter { it.isDigit() || it == '+' }
        val normalized = when {
            cleanDigits.startsWith("+") -> "+" + cleanDigits.drop(1).filter(Char::isDigit)
            cleanDigits.startsWith("00") -> "+" + cleanDigits.drop(2).filter(Char::isDigit)
            cleanDigits.startsWith("0") -> defaultDialCode + cleanDigits.drop(1).filter(Char::isDigit)
            else -> defaultDialCode + cleanDigits.filter(Char::isDigit)
        }

        val digitsOnly = normalized.drop(1)
        if (digitsOnly.length !in 7..15) {
            return Result.failure(
                IllegalArgumentException("Enter a valid international phone number (7 to 15 digits).")
            )
        }
        return Result.success(normalized)
    }

    fun maskNumberForPrivacy(e164: String): String {
        if (e164.length <= 6) return e164
        val prefix = e164.take(4)
        val suffix = e164.takeLast(3)
        return "$prefix••••$suffix"
    }
}
