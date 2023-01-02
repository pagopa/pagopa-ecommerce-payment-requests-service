package it.pagopa.ecommerce.payment.requests.repositories

import java.util.*
import org.springframework.data.repository.CrudRepository

interface CartInfoRepository : CrudRepository<CartInfo, UUID>
