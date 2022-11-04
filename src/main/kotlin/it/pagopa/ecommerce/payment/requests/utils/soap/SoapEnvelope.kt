package it.pagopa.ecommerce.payment.requests.utils.soap

import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement
data class SoapEnvelope(val header: String, val body: Any)