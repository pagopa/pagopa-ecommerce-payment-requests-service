package it.pagopa.ecommerce.payment.requests.domain


import java.util.regex.Pattern


class RptId(val value: String) {
    companion object {
        private val rptIdRegex = Pattern.compile("([a-zA-Z\\d]{29})")
    }

    //RtpId = CF(0,10)+NotID(11,28)
    //NotID = AuxDigit(0)+ApplicationCode(1,2)+CodiceIUV(3,18) || AuxDigit(0)+CodiceIUV(1,18)
    init {
        require(
            rptIdRegex.matcher(value).matches()
        ) { "Ill-formed RPT id: " + value + ". Doesn't match format: " + rptIdRegex.pattern() }
    }

    val fiscalCode: String
        get() = value.substring(0, 11)

    val noticeId: String
        get() = value.substring(11)

    val auxDigit: String
        get() = noticeId.substring(0, 1)

    val applicationCode: String?
        get() = if ("0" == auxDigit) noticeId.substring(1, 3) else null

    val IUV: String
        get() = noticeId.substring(if (applicationCode != null) 3 else 1, 18)

    override fun toString(): String {
        return "RptId(value='$value')"
    }
}
