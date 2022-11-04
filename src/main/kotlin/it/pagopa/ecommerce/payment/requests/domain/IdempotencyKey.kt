package it.pagopa.ecommerce.payment.requests.domain


import org.springframework.data.annotation.PersistenceCreator
import java.util.regex.Pattern

class IdempotencyKey {
    private val key: String

    constructor(pspFiscalCode: String, keyIdentifier: String) {
        validateComponents(pspFiscalCode, keyIdentifier)
        key = pspFiscalCode + "_" + keyIdentifier
    }

    @PersistenceCreator
    constructor(key: String) {
        val matches = key.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        require(matches.size == 2) { "Key doesn't match format `\$pspFiscalCode_\$keyIdentifier`" }
        val pspFiscalCode = matches[0]
        val keyIdentifier = matches[1]
        validateComponents(pspFiscalCode, keyIdentifier)
        this.key = key
    }

    companion object {
        private val pspFiscalCodeRegex = Pattern.compile("\\d{11}")
        private val keyIdentifierRegex = Pattern.compile("[a-zA-Z\\d]{10}")

        private fun validateComponents(pspFiscalCode: String, keyIdentifier: String) {
            require(
                pspFiscalCodeRegex.matcher(pspFiscalCode).matches()
            ) { "PSP fiscal code doesn't match regex: " + pspFiscalCodeRegex.pattern() }

            require(
                keyIdentifierRegex.matcher(keyIdentifier).matches()
            ) { "Key identifier doesn't match regex: " + keyIdentifierRegex.pattern() }
        }
    }
}
