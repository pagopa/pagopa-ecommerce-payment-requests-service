package it.pagopa.ecommerce.payment.requests.utils.soap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.xml.bind.Marshaller;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class Jaxb2SoapEncoderTest {

    @InjectMocks
    private Jaxb2SoapEncoder jaxb2SoapEncoder = new Jaxb2SoapEncoder();

    @Test
    void shouldConstructTransactionAmount()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = Jaxb2SoapEncoder.class.getDeclaredMethod("getMarshaller");
        method.setAccessible(true);

        Marshaller marshaller = (Marshaller) method.invoke(jaxb2SoapEncoder);

        assertEquals(Boolean.TRUE, marshaller != null);
    }
}
