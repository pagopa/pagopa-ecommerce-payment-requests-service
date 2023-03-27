package it.pagopa.ecommerce.payment.requests.utils.soap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

final class JaxbContextContainer {

    private static final String PACKAGE_NODE = "it.pagopa.ecommerce.generated.transactions.model";

    private static final JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(PACKAGE_NODE);
        } catch (JAXBException e) {
            throw new IllegalStateException("Exception initializing JaxbContext", e);
        }
    }

    public Marshaller createMarshaller() throws JAXBException {
        return jaxbContext.createMarshaller();
    }

    public Unmarshaller createUnmarshaller() throws JAXBException {
        return jaxbContext.createUnmarshaller();
    }
}
