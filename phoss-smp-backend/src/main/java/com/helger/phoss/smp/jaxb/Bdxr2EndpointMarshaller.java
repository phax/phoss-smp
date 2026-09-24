package com.helger.phoss.smp.jaxb;

import javax.xml.namespace.QName;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.smpclient.bdxr2.marshal.AbstractBDXR2Marshaller;
import com.helger.xsds.bdxr.smp2.CBDXRSMP2;
import com.helger.xsds.bdxr.smp2.ac.EndpointType;

import jakarta.xml.bind.JAXBElement;

/**
 * Specific marshaller for OASIS BDXR SMP 2.0 Endpoint types.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public class Bdxr2EndpointMarshaller extends AbstractBDXR2Marshaller <EndpointType>
{
  public static final QName ENDPOINT_QNAME = new QName ("http://docs.oasis-open.org/bdxr/ns/SMP/2/AggregateComponents",
                                                        "Endpoint");

  @NonNull
  public static JAXBElement <EndpointType> createEndpoint (@Nullable final EndpointType value)
  {
    return new JAXBElement <> (ENDPOINT_QNAME, EndpointType.class, null, value);
  }

  public Bdxr2EndpointMarshaller ()
  {
    // In contrast to Peppol SMP and OASIS BDXR SMP 1.0, the Aggregate Components XSD declares a
    // global "Endpoint" element, so XML Schema validation stays enabled
    super (EndpointType.class, CBDXRSMP2.getAllXSDIncludes (), Bdxr2EndpointMarshaller::createEndpoint);
  }
}
