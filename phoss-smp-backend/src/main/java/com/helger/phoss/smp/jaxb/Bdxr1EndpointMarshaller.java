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

import com.helger.smpclient.bdxr1.marshal.AbstractBDXR1Marshaller;
import com.helger.xsds.bdxr.smp1.EndpointType;

import jakarta.xml.bind.JAXBElement;

/**
 * Specific marshaller for OASIS BDXR SMP 1.0 Endpoint types.
 *
 * @author Philip Helger
 * @since 8.5.0
 */
public class Bdxr1EndpointMarshaller extends AbstractBDXR1Marshaller <EndpointType>
{
  public static final QName ENDPOINT_QNAME = new QName ("http://docs.oasis-open.org/bdxr/ns/SMP/2016/05", "Endpoint");

  @NonNull
  public static JAXBElement <EndpointType> createEndpoint (@Nullable final EndpointType value)
  {
    return new JAXBElement <> (ENDPOINT_QNAME, EndpointType.class, null, value);
  }

  public Bdxr1EndpointMarshaller ()
  {
    super (EndpointType.class, Bdxr1EndpointMarshaller::createEndpoint);
    // The OASIS BDXR SMP 1.0 XSD declares no global "Endpoint" element, so an Endpoint can neither
    // be read nor written with XML Schema validation enabled
    setUseSchema (false);
  }
}
