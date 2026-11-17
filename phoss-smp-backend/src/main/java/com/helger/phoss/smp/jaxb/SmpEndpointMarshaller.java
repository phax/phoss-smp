/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.jaxb;

import javax.xml.namespace.QName;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.smpclient.peppol.marshal.AbstractSMPMarshaller;
import com.helger.xsds.peppol.smp1.EndpointType;

import jakarta.xml.bind.JAXBElement;

/**
 * Specific marshaller for Peppol SMP Endpoint types.
 * 
 * @author Philip Helger
 * @since 8.5.0
 */
public class SmpEndpointMarshaller extends AbstractSMPMarshaller <EndpointType>
{
  public static final QName ENDPOINT_QNAME = new QName ("http://busdox.org/serviceMetadata/publishing/1.0/",
                                                        "Endpoint");

  @NonNull
  public static JAXBElement <EndpointType> createEndpoint (@Nullable final EndpointType value)
  {
    return new JAXBElement <> (ENDPOINT_QNAME, EndpointType.class, null, value);
  }

  public SmpEndpointMarshaller ()
  {
    super (EndpointType.class, SmpEndpointMarshaller::createEndpoint);
    // The Peppol SMP XSD declares no global "Endpoint" element, so an Endpoint can neither be read
    // nor written with XML Schema validation enabled
    setUseSchema (false);
  }
}
