/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.domain.serviceinfo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.compare.IComparator;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.smpserver.domain.extension.ISMPHasExtension;

/**
 * Represents a single SMP endpoint that is contained in a single
 * {@link ISMPProcess}.
 *
 * @author Philip Helger
 */
public interface ISMPEndpoint extends Serializable, ISMPHasExtension
{
  /**
   * @return the type of BUSDOX transport that is being used between access
   *         points, e.g. the BUSDOX START profile ("busdox-transport-start").
   *         The list of valid transport protocols is found in
   *         ICT-Transport-Policy_for_using_Identifiers.
   * @see com.helger.peppol.smp.ISMPTransportProfile
   */
  @Nonnull
  @Nonempty
  String getTransportProfile ();

  /**
   * @return The address of an endpoint, as an WS-Addressing Endpoint Reference
   *         (EPR). This is just a URL.
   */
  @Nullable
  String getEndpointReference ();

  /**
   * @return <code>true</code> if this endpoint has an endpoint reference URL,
   *         <code>false</code> otherwise.
   * @see #getEndpointReference()
   */
  default boolean hasEndpointReference ()
  {
    return StringHelper.hasText (getEndpointReference ());
  }

  /**
   * @return Set to ‘true’ if the recipient requires business-level signatures
   *         for the message, meaning a signature applied to the business
   *         message before the message is put on the transport. This is
   *         independent of the transport-level signatures that a specific
   *         transport profile, such as the START profile, might mandate. This
   *         flag does not indicate which type of business-level signature might
   *         be required. Setting or consuming business-level signatures would
   *         typically be the responsibility of the final senders and receivers
   *         of messages, rather than a set of APs.
   */
  boolean isRequireBusinessLevelSignature ();

  /**
   * @return Indicates the minimum authentication level that recipient requires.
   *         The specific semantics of this field is defined in a specific
   *         instance of the BUSDOX infrastructure. It could for example reflect
   *         the value of the "urn:eu:busdox:attribute:assurance-level" SAML
   *         attribute defined in the START specification.
   */
  @Nullable
  String getMinimumAuthenticationLevel ();

  /**
   * @return <code>true</code> if this endpoint has a minimum authentication
   *         level, <code>false</code> otherwise.
   * @see #getMinimumAuthenticationLevel()
   */
  default boolean hasMinimumAuthenticationLevel ()
  {
    return StringHelper.hasText (getMinimumAuthenticationLevel ());
  }

  /**
   * @return Activation date and time of the service. Senders should ignore
   *         services that are not yet activated.
   */
  @Nullable
  LocalDateTime getServiceActivationDateTime ();

  /**
   * @return Activation date of the service. Senders should ignore services that
   *         are not yet activated.
   */
  @Nullable
  default LocalDate getServiceActivationDate ()
  {
    final LocalDateTime aServiceActivationDT = getServiceActivationDateTime ();
    return aServiceActivationDT != null ? aServiceActivationDT.toLocalDate () : null;
  }

  /**
   * @return <code>true</code> if this endpoint has a service activation
   *         datetime, <code>false</code> otherwise.
   * @see #getServiceActivationDate()
   */
  default boolean hasServiceActivationDate ()
  {
    return getServiceActivationDate () != null;
  }

  /**
   * @return Expiration date and time of the service. Senders should ignore
   *         services that are expired.
   */
  @Nullable
  LocalDateTime getServiceExpirationDateTime ();

  /**
   * @return Expiration date of the service. Senders should ignore services that
   *         are expired.
   */
  @Nullable
  default LocalDate getServiceExpirationDate ()
  {
    final LocalDateTime aServiceExpirationDT = getServiceExpirationDateTime ();
    return aServiceExpirationDT != null ? aServiceExpirationDT.toLocalDate () : null;
  }

  /**
   * @return <code>true</code> if this endpoint has a service expiration
   *         datetime, <code>false</code> otherwise.
   * @see #getServiceExpirationDateTime()
   */
  default boolean hasServiceExpirationDateTime ()
  {
    return getServiceExpirationDateTime () != null;
  }

  /**
   * @return the complete signing certificate of the recipient AP, as a PEM base
   *         64 encoded X509 DER formatted value.
   */
  @Nonnull
  @Nonempty
  String getCertificate ();

  /**
   * @return A human readable description of the service
   */
  @Nonnull
  @Nonempty
  String getServiceDescription ();

  /**
   * @return a link to human readable contact information. This might also be an
   *         email address.
   */
  @Nonnull
  @Nonempty
  String getTechnicalContactUrl ();

  /**
   * @return A URL to human readable documentation of the service format. This
   *         could for example be a web site containing links to XML Schemas,
   *         WSDLs, Schematrons and other relevant resources.
   */
  @Nullable
  String getTechnicalInformationUrl ();

  default boolean hasTechnicalInformationUrl ()
  {
    return StringHelper.hasText (getTechnicalInformationUrl ());
  }

  /**
   * @return This service information object as a PEPPOL SMP JAXB object for the
   *         REST interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.peppol.smp.EndpointType getAsJAXBObjectPeppol ();

  /**
   * @return This service information object as a BDXR SMP JAXB object for the
   *         REST interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.peppol.bdxr.EndpointType getAsJAXBObjectBDXR ();

  @Nonnull
  static IComparator <ISMPEndpoint> comparator ()
  {
    return (aElement1, aElement2) -> {
      int ret = aElement1.getTransportProfile ().compareTo (aElement2.getTransportProfile ());
      if (ret == 0)
        ret = aElement1.getEndpointReference ().compareTo (aElement2.getEndpointReference ());
      return ret;
    };
  }
}
