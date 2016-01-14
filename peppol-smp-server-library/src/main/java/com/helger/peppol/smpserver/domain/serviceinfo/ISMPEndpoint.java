/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.domain.serviceinfo;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.helger.commons.annotation.Nonempty;
import com.helger.peppol.smpserver.domain.ISMPHasExtension;

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
  LocalDate getServiceActivationDate ();

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
  LocalDate getServiceExpirationDate ();

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

  @Nonnull
  com.helger.peppol.smp.EndpointType getAsJAXBObjectPeppol ();

  @Nonnull
  com.helger.peppol.bdxr.EndpointType getAsJAXBObjectBDXR ();
}
