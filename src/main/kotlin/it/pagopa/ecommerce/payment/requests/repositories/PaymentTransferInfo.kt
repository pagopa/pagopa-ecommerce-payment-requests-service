package it.pagopa.ecommerce.payment.requests.repositories

class PaymentTransferInfo
constructor(
    val paFiscalCode: String,
    val digitalStamp: Boolean,
    val transferAmount: Int,
    val transferCategory: String?,
)
{

}