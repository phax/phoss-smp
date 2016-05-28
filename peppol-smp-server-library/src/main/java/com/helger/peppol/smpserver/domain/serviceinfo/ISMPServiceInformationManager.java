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

import java.util.Collection;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.peppol.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.peppol.process.IPeppolProcessIdentifier;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * Manager for {@link ISMPServiceInformation} objects. Service information
 * objects require a service group to be present first.
 *
 * @author Philip Helger
 */
public interface ISMPServiceInformationManager
{
  /**
   * Create or update an SMP service information object. An existing service
   * information object is searched by service group, document type ID, process
   * ID and transport profile.
   *
   * @param aServiceInformation
   *        The service information object to handle. May not be
   *        <code>null</code>.
   */
  void mergeSMPServiceInformation (@Nonnull ISMPServiceInformation aServiceInformation);

  /**
   * Find the service information matching the passed quadruple of parameters.
   * If one of the parameters is <code>null</code> no match should be found and
   * <code>null</code> should be returned. This is a sanity method to find the
   * service information for a certain endpoint and is a specialization of
   * {@link #getSMPServiceInformationOfServiceGroupAndDocumentType(ISMPServiceGroup, IDocumentTypeIdentifier)}
   * .
   *
   * @param aServiceGroup
   *        The service group to be searched. May be <code>null</code>.
   * @param aDocTypeID
   *        The document type ID to search. May be <code>null</code>.
   * @param aProcessID
   *        The process ID to search. May be <code>null</code>.
   * @param aTransportProfile
   *        The transport profile to search. May be <code>null</code>.
   * @return <code>null</code> if any of the parameters is <code>null</code> or
   *         if no such service information exists.
   * @see #getSMPServiceInformationOfServiceGroupAndDocumentType(ISMPServiceGroup,
   *      IDocumentTypeIdentifier)
   */
  @Nullable
  ISMPServiceInformation findServiceInformation (@Nullable ISMPServiceGroup aServiceGroup,
                                                 @Nullable IPeppolDocumentTypeIdentifier aDocTypeID,
                                                 @Nullable IPeppolProcessIdentifier aProcessID,
                                                 @Nullable ISMPTransportProfile aTransportProfile);

  /**
   * Delete the provided service information object.
   *
   * @param aSMPServiceInformation
   *        The service information objects to be deleted. May be
   *        <code>null</code>.
   * @return {@link EChange#CHANGED} if the parameter is not <code>null</code>
   *         and was successfully deleted from the internal data structures.
   *         {@link EChange#UNCHANGED} must be returned otherwise.
   */
  @Nonnull
  EChange deleteSMPServiceInformation (@Nullable ISMPServiceInformation aSMPServiceInformation);

  /**
   * Delete all contained service information objects that belong to the passed
   * service group.
   *
   * @param aServiceGroup
   *        The service group for which all service information objects should
   *        be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} only if the passed service group is not
   *         <code>null</code> and if at least one service information object
   *         was deleted. {@link EChange#UNCHANGED} must be returned otherwise.
   */
  @Nonnull
  EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable ISMPServiceGroup aServiceGroup);

  /**
   * @return All service information objects in arbitrary order. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPServiceInformation> getAllSMPServiceInformation ();

  /**
   * @return The count of all service information objects. Always &ge; 0.
   */
  @Nonnegative
  int getSMPServiceInformationCount ();

  /**
   * Get all service information objects that belong to the provided service
   * group.
   *
   * @param aServiceGroup
   *        The service group of interest. May be <code>null</code>.
   * @return Never <code>null</code> but maybe empty list of all matching
   *         service information objects in arbitrary order. An empty result
   *         means that either a non-existing service group was passed <b>or</b>
   *         that no service information objects exist for the provided service
   *         group.
   */
  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPServiceInformation> getAllSMPServiceInformationsOfServiceGroup (@Nullable ISMPServiceGroup aServiceGroup);

  /**
   * Get all SMP document types that are registered for the provided service
   * group. This is a sanity method to handle the REST service group request
   * (e.g. <code>/iso6523-actorid-upis::0088/example</code>) in an efficient
   * way.
   *
   * @param aServiceGroup
   *        The service group of interest. May be <code>null</code>.
   * @return Never <code>null</code> but may empty collection of document type
   *         identifiers in arbitrary order. An empty result means that either a
   *         non-existing service group was passed <b>or</b> that no service
   *         information objects exist for the provided service group.
   * @see #getAllSMPDocumentTypesOfServiceGroup(ISMPServiceGroup)
   */
  @Nonnull
  @ReturnsMutableCopy
  Collection <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (@Nullable ISMPServiceGroup aServiceGroup);

  /**
   * Get the service information for the passed tuple of service group and
   * document type identifier.
   *
   * @param aServiceGroup
   *        The service group of interest. May be <code>null</code>.
   * @param aDocumentTypeIdentifier
   *        The document type identifier to search. May be <code>null</code>.
   * @return <code>null</code> if any parameter is <code>null</code> or if the
   *         service group is not found or if the document type is not found in
   *         the service group.
   */
  @Nullable
  ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable ISMPServiceGroup aServiceGroup,
                                                                                @Nullable IDocumentTypeIdentifier aDocumentTypeIdentifier);
}
