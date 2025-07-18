package it.pagopa.ecommerce.payment.requests.repositories

class PaymentTransferInfo
constructor(
  val paFiscalCode: String,
  val digitalStamp: Boolean,
  val transferAmount: Long,
  val transferCategory: String?,
) {}
