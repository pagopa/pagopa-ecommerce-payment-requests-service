package it.pagopa.ecommerce.payment.requests.utils.soap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@Data
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class SoapEnvelope {

    private String header;
    private Object body;
}
