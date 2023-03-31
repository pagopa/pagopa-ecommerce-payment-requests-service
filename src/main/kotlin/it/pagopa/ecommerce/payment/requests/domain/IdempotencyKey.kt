package it.pagopa.ecommerce.payment.requests.domain

import java.util.regex.Pattern
import org.springframework.data.annotation.PersistenceCreator

class IdempotencyKey {
  val rawValue: String

  constructor(pspFiscalCode: String, keyIdentifier: String) {
    validateComponents(pspFiscalCode, keyIdentifier)
    rawValue = pspFiscalCode + "_" + keyIdentifier
  }

  @PersistenceCreator
  constructor(rawValue: String) {
    val matches = rawValue.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    require(matches.size == 2) { "Key doesn't match format `\$pspFiscalCode_\$keyIdentifier`" }
    val pspFiscalCode = matches[0]
    val keyIdentifier = matches[1]
    validateComponents(pspFiscalCode, keyIdentifier)
    this.rawValue = rawValue
  }

  companion object {
    private val pspFiscalCodeRegex = Pattern.compile("\\d{11}")
    private val keyIdentifierRegex = Pattern.compile("[a-zA-Z\\d]{10}")

    private fun validateComponents(pspFiscalCode: String, keyIdentifier: String) {
      require(pspFiscalCodeRegex.matcher(pspFiscalCode).matches()) {
        "PSP fiscal code doesn't match regex: " + pspFiscalCodeRegex.pattern()
      }

      require(keyIdentifierRegex.matcher(keyIdentifier).matches()) {
        "Key identifier doesn't match regex: " + keyIdentifierRegex.pattern()
      }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as IdempotencyKey

    if (rawValue != other.rawValue) return false

    return true
  }

  override fun hashCode(): Int {
    return rawValue.hashCode()
  }

  override fun toString(): String {
    return "IdempotencyKey(key='$rawValue')"
  }
}
