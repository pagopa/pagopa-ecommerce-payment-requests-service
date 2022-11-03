package it.pagopa.ecommerce.payment.requests.repositories

import org.springframework.data.repository.CrudRepository
import java.util.*

interface CartInfoRepository : CrudRepository<CartInfo, UUID>
