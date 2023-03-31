package it.pagopa.ecommerce.payment.requests.repositories

import it.pagopa.ecommerce.payment.requests.domain.IdempotencyKey
import it.pagopa.ecommerce.payment.requests.domain.RptId
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.redis.core.RedisHash
import org.springframework.lang.NonNull
import org.springframework.lang.Nullable

@RedisHash(value = "keys", timeToLive = 10 * 60)
class PaymentRequestInfo
@PersistenceCreator
constructor(
  @Id val id: RptId,
  val paFiscalCode: String?,
  val paName: String?,
  val description: String?,
  val amount: Int?,
  val dueDate: String?,
  val paymentToken: String?,
  val idempotencyKey: IdempotencyKey?,
  val transferList: List<PaymentTransferInfo>,
) {
}
