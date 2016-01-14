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
package com.helger.peppol.smpserver.restapi;

import java.net.URI;

import javax.annotation.Nonnull;

import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IParticipantIdentifier;

/**
 * This interface must be implemented by all real SMP implementations, so that
 * the REST API can easily be used. It provides information only available in
 * the web application.
 *
 * @author Philip Helger
 */
public interface ISMPServerAPIDataProvider
{
  /**
   * @return The URI of the current request. May not be <code>null</code>.
   */
  @Nonnull
  URI getCurrentURI ();

  /**
   * Get the service group HREF for the passed service group ID. Since this
   * depends on the web address of the server it must be implemented in this
   * interface.
   *
   * @param aServiceGroupID
   *        The service group ID. Never <code>null</code>.
   * @return The HREF to show the service group.
   */
  @Nonnull
  String getServiceGroupHref (@Nonnull IParticipantIdentifier aServiceGroupID);

  /**
   * Get the service metadata HREF for the passed service group and document
   * type ID. Since this depends on the web address of the server it must be
   * implemented in this interface.
   *
   * @param aServiceGroupID
   *        The service group ID. Never <code>null</code>.
   * @param aDocTypeID
   *        The document type ID of the participant to query.
   * @return The HREF to the service metadata.
   */
  @Nonnull
  String getServiceMetadataReferenceHref (@Nonnull IParticipantIdentifier aServiceGroupID,
                                          @Nonnull IDocumentTypeIdentifier aDocTypeID);
}
