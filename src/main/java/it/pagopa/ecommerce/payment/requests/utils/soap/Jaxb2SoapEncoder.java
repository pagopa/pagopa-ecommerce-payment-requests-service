package it.pagopa.ecommerce.payment.requests.utils.soap;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.w3c.dom.Document;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Jaxb2SoapEncoder implements Encoder<Object> {

    private final JaxbContextContainer jaxbContexts = new JaxbContextContainer();

    @Override
    public boolean canEncode(
                             ResolvableType elementType,
                             MimeType mimeType
    ) {
        Class<?> outputClass = elementType.toClass();
        return (outputClass.isAnnotationPresent(XmlRootElement.class) ||
                outputClass.isAnnotationPresent(XmlType.class));
    }

    @Override
    public Flux<DataBuffer> encode(
                                   Publisher<?> inputStream,
                                   DataBufferFactory bufferFactory,
                                   ResolvableType elementType,
                                   MimeType mimeType,
                                   Map<String, Object> hints
    ) {
        return Flux.from(inputStream)
                .take(1)
                .concatMap(value -> encode(value, bufferFactory))
                .doOnDiscard(PooledDataBuffer.class, PooledDataBuffer::release);
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return Arrays.asList(MimeTypeUtils.TEXT_XML);
    }

    private Flux<DataBuffer> encode(
                                    Object value,
                                    DataBufferFactory bufferFactory
    ) {
        return Mono.fromCallable(() -> {

            boolean release = true;
            DataBuffer buffer = bufferFactory.allocateBuffer(1024);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            SoapEnvelope soapEnvelope = (SoapEnvelope) value;
            OutputStream outputStream = buffer.asOutputStream();
            Document doc = dbf.newDocumentBuilder().newDocument();
            SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();

            try {

                /**
                 * Marshal: body content into a xml Document.
                 */
                getMarshaller().marshal(soapEnvelope.getBody(), doc);

                /**
                 * Clean: SOAP-ENV (SOAP1.2) into soap (SOAP1.1)
                 */
                soapMessage.getSOAPPart().getEnvelope().removeNamespaceDeclaration("SOAP-ENV");
                soapMessage.getSOAPPart().getEnvelope().setPrefix("soap");
                soapMessage.getSOAPHeader().setPrefix("soap");
                soapMessage.getSOAPBody().setPrefix("soap");

                /**
                 * Marshal ending
                 */
                SOAPBody soapBody = soapMessage.getSOAPBody();
                soapBody.addDocument(doc);
                soapMessage.saveChanges();
                soapMessage.writeTo(outputStream);
                outputStream.flush();

                release = false;
                return buffer;
            } catch (MarshalException ex) {
                throw new EncodingException(
                        "Could not marshal " + value.getClass() + " to XML",
                        ex
                );
            } catch (JAXBException ex) {
                throw new CodecException("Invalid JAXB configuration", ex);
            } finally {
                if (release) {
                    DataBufferUtils.release(buffer);
                }
            }
        }).flux();
    }

    private Marshaller getMarshaller() throws JAXBException {
        Marshaller marshaller = jaxbContexts.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
        return marshaller;
    }
}
