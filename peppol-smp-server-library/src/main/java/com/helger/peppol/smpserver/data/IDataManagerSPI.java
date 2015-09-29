/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIInterface;
import com.helger.commons.annotation.Nonempty;
import com.helger.peppol.identifier.DocumentIdentifierType;
import com.helger.peppol.identifier.ParticipantIdentifierType;
import com.helger.peppol.smp.ServiceInformationType;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * This interface is used by the REST interface for accessing the underlying SMP
 * data. One should implement this interface if a new data source is needed.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@IsSPIInterface
public interface IDataManagerSPI
{
  /**
   * Check if an SMP user matching the user name of the BasicAuth credentials
   * exists, and that the passwords match. So this method verifies that the
   * BasicAuth credentials are valid.
   *
   * @param aCredentials
   *        The credentials to be validated. May not be <code>null</code>.
   * @return The matching non-<code>null</code> {@link IDataUser}.
   * @throws Throwable
   *         If no user matching the passed user name is present or if the
   *         password in the credentials does not match the stored password
   *         (hash).
   */
  @Nonnull
  IDataUser validateUserCredentials (@Nonnull BasicAuthClientCredentials aCredentials) throws Throwable;

  /**
   * Create a pre-authenticated user (e.g. for the management GUI).
   *
   * @param sUserName
   *        The user name to use.
   * @return Never <code>null</code>.
   */
  @Nonnull
  IDataUser createPreAuthenticatedUser (@Nonnull @Nonempty String sUserName);

  /**
   * Saves the given service metadata in the underlying data layer.
   *
   * @param aServiceMetadata
   *        The service information to save.
   * @param aDataUser
   *        The current, verified user. Never <code>null</code>.
   * @throws Throwable
   *         In case something goes wrong.
   */
  void saveService (@Nonnull ServiceInformationType aServiceMetadata, @Nonnull IDataUser aDataUser) throws Throwable;

  /**
   * Deletes a service metadata object given by its service group id and
   * document id.
   *
   * @param aServiceGroupID
   *        The service group id of the service metadata.
   * @param aDocTypeID
   *        The document id of the service metadata.
   * @param aDataUser
   *        The current, verified user. Never <code>null</code>.
   * @throws Throwable
   *         In case something goes wrong.
   */
  void deleteService (@Nonnull ParticipantIdentifierType aServiceGroupID,
                      @Nonnull DocumentIdentifierType aDocTypeID,
                      @Nonnull IDataUser aDataUser) throws Throwable;
}
