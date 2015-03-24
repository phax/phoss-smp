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

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.busdox.servicemetadata.publishing._1.ServiceGroupType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataType;

import com.helger.commons.annotations.ReturnsMutableCopy;
import com.helger.peppol.identifier.DocumentIdentifierType;
import com.helger.peppol.identifier.ParticipantIdentifierType;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * This interface is used by the REST interface for accessing the underlying SMP
 * data. One should implement this interface if a new data source is needed.
 * 
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public interface IDataManager
{
  /**
   * Gets the service group ids owned by the given credentials.
   * 
   * @param aCredentials
   *        The credentials to get service groups id for.
   * @return A collection of service group id's.
   * @throws Throwable
   */
  @Nonnull
  @ReturnsMutableCopy
  Collection <ParticipantIdentifierType> getServiceGroupList (@Nonnull BasicAuthClientCredentials aCredentials) throws Throwable;

  /**
   * This method returns a ServiceGroup given its id.
   * 
   * @param aServiceGroupID
   *        The service group id.
   * @return The service group corresponding to the id.
   * @throws Throwable
   */
  @Nullable
  ServiceGroupType getServiceGroup (@Nonnull ParticipantIdentifierType aServiceGroupID) throws Throwable;

  /**
   * Persists the service group in the underlying data layer. This operation
   * requires credentials.
   * 
   * @param aServiceGroup
   *        The service group to save.
   * @param aCredentials
   *        The credentials to use.
   * @throws Throwable
   */
  void saveServiceGroup (@Nonnull ServiceGroupType aServiceGroup, @Nonnull BasicAuthClientCredentials aCredentials) throws Throwable;

  /**
   * Deletes the service group having the specified id.
   * 
   * @param aServiceGroupID
   *        The ID of the service group to delete.
   * @param aCredentials
   *        The credentials to use.
   * @throws Throwable
   */
  void deleteServiceGroup (@Nonnull ParticipantIdentifierType aServiceGroupID,
                           @Nonnull BasicAuthClientCredentials aCredentials) throws Throwable;

  /**
   * Gets a list of the document id's of the given service group.
   * 
   * @param aServiceGroupID
   *        The id of the service group.
   * @return The corresponding document id's.
   * @throws Throwable
   */
  @Nonnull
  @ReturnsMutableCopy
  List <DocumentIdentifierType> getDocumentTypes (@Nonnull ParticipantIdentifierType aServiceGroupID) throws Throwable;

  /**
   * Gets the list of service metadata objects corresponding to a given service
   * group id.
   * 
   * @param aServiceGroupID
   *        The service group id.
   * @return A list of service metadata objects.
   * @throws Throwable
   */
  @Nonnull
  @ReturnsMutableCopy
  Collection <ServiceMetadataType> getServices (@Nonnull ParticipantIdentifierType aServiceGroupID) throws Throwable;

  /**
   * Gets the service metadata corresponding to the service group id and
   * document id.
   * 
   * @param aServiceGroupID
   *        The service group id of the service metadata.
   * @param aDocType
   *        The document id of the service metadata.
   * @return The corresponding service metadata.
   * @throws Throwable
   */
  @Nullable
  ServiceMetadataType getService (@Nonnull ParticipantIdentifierType aServiceGroupID,
                                  @Nonnull DocumentIdentifierType aDocType) throws Throwable;

  /**
   * Saves the given service metadata in the underlying data layer.
   * 
   * @param aServiceMetadata
   *        The service metadata to save.
   * @param aCredentials
   *        The credentials to use.
   * @throws Throwable
   */
  void saveService (@Nonnull ServiceMetadataType aServiceMetadata, @Nonnull BasicAuthClientCredentials aCredentials) throws Throwable;

  /**
   * Deletes a service metadata object given by its service group id and
   * document id.
   * 
   * @param aServiceGroupID
   *        The service group id of the service metadata.
   * @param aDocType
   *        The document id of the service metadata.
   * @param aCredentials
   *        The credentials to use.
   * @throws Throwable
   */
  void deleteService (@Nonnull ParticipantIdentifierType aServiceGroupID,
                      @Nonnull DocumentIdentifierType aDocType,
                      @Nonnull BasicAuthClientCredentials aCredentials) throws Throwable;

  /**
   * Checks whether the ServiceMetadata should be found elsewhere.
   * 
   * @param aServiceGroupID
   *        The service group id of the service metadata. May not be
   *        <code>null</code>.
   * @param aDocTypeID
   *        The document id of the service metadata. May not be
   *        <code>null</code>.
   * @return The URI to be redirected to. null if no redirection should take
   *         place.
   * @throws Throwable
   */
  @Nullable
  ServiceMetadataType getRedirection (@Nonnull ParticipantIdentifierType aServiceGroupID,
                                      @Nonnull DocumentIdentifierType aDocTypeID) throws Throwable;
}
