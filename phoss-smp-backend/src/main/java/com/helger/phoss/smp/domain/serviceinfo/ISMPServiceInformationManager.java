/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.serviceinfo;

import java.util.function.Consumer;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.smp.ChangeSMPV8;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Manager for {@link ISMPServiceInformation} objects. Service information
 * objects require a service group to be present first.
 *
 * @author Philip Helger
 */
public interface ISMPServiceInformationManager
{
  /**
   * @return A non-<code>null</code> mutable list of callbacks.
   */
  @Nonnull
  @ReturnsMutableObject
  CallbackList <ISMPServiceInformationCallback> serviceInformationCallbacks ();

  /**
   * Create or update an SMP service information object. An existing service
   * information object is searched by service group, document type ID, process
   * ID and transport profile.
   *
   * @param aServiceInformation
   *        The service information object to handle. May not be
   *        <code>null</code>.
   * @return {@link ESuccess}
   */
  @Nonnull
  ESuccess mergeSMPServiceInformation (@Nonnull ISMPServiceInformation aServiceInformation);

  /**
   * Find the service information matching the passed quadruple of parameters.
   * If one of the parameters is <code>null</code> no match should be found and
   * <code>null</code> should be returned. This is a sanity method to find the
   * service information for a certain endpoint and is a specialization of
   * {@link #getSMPServiceInformationOfServiceGroupAndDocumentType(IParticipantIdentifier, IDocumentTypeIdentifier)}
   * .
   *
   * @param aParticipantID
   *        The service group ID to be searched. May be <code>null</code>.
   * @param aDocTypeID
   *        The document type ID to search. May be <code>null</code>.
   * @param aProcessID
   *        The process ID to search. May be <code>null</code>.
   * @param sTransportProfileID
   *        The transport profile ID to search. May be <code>null</code>.
   * @return <code>null</code> if any of the parameters is <code>null</code> or
   *         if no such service information exists.
   * @see #getSMPServiceInformationOfServiceGroupAndDocumentType(IParticipantIdentifier,
   *      IDocumentTypeIdentifier)
   */
  @ChangeSMPV8 ("Rename to findSMPServiceInformation")
  @Nullable
  ISMPServiceInformation findServiceInformation (@Nullable IParticipantIdentifier aParticipantID,
                                                 @Nullable IDocumentTypeIdentifier aDocTypeID,
                                                 @Nullable IProcessIdentifier aProcessID,
                                                 @Nullable String sTransportProfileID);

  /**
   * Delete the provided service information object.
   *
   * @param aSMPServiceInformation
   *        The service information object to be deleted. May be
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
   * @param aParticipantID
   *        The service group ID for which all service information objects
   *        should be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} only if the passed service group is not
   *         <code>null</code> and if at least one service information object
   *         was deleted. {@link EChange#UNCHANGED} must be returned otherwise.
   */
  @Nonnull
  EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable IParticipantIdentifier aParticipantID);

  /**
   * Delete a single process with all endpoints from this service information.
   *
   * @param aSMPServiceInformation
   *        The service information object where the process should be deleted.
   *        May be <code>null</code>.
   * @param aProcess
   *        The process within the service information to be deleted. May be
   *        <code>null</code>.
   * @return {@link EChange#CHANGED} if the the process was successfully
   *         deleted, {@link EChange#UNCHANGED} otherwise.
   * @since 5.0.0
   */
  @Nonnull
  EChange deleteSMPProcess (@Nullable ISMPServiceInformation aSMPServiceInformation, @Nullable ISMPProcess aProcess);

  /**
   * @return All service information objects in arbitrary order. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation ();

  /**
   * Iterate each Service Information element and invoke the provided consumer
   * for it.
   *
   * @param aConsumer
   *        The consumer to invoke. May not be <code>null</code>.
   * @since 7.1.5
   */
  void forEachSMPServiceInformation (@Nonnull Consumer <? super ISMPServiceInformation> aConsumer);

  /**
   * @return The count of all service information objects. Always &ge; 0.
   */
  @Nonnegative
  long getSMPServiceInformationCount ();

  /**
   * Get all service information objects that belong to the provided service
   * group.
   *
   * @param aParticipantID
   *        The service group ID of interest. May be <code>null</code>.
   * @return Never <code>null</code> but maybe empty list of all matching
   *         service information objects in arbitrary order. An empty result
   *         means that either a non-existing service group was passed <b>or</b>
   *         that no service information objects exist for the provided service
   *         group.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPServiceInformation> getAllSMPServiceInformationOfServiceGroup (@Nullable IParticipantIdentifier aParticipantID);

  /**
   * Get all SMP document types that are registered for the provided service
   * group. This is a sanity method to handle the REST service group request
   * (e.g. <code>/iso6523-actorid-upis::0088/example</code>) in an efficient
   * way.
   *
   * @param aParticipantID
   *        The service group ID of interest. May be <code>null</code>.
   * @return Never <code>null</code> but may empty collection of document type
   *         identifiers in arbitrary order. An empty result means that either a
   *         non-existing service group was passed <b>or</b> that no service
   *         information objects exist for the provided service group.
   * @see #getAllSMPDocumentTypesOfServiceGroup(IParticipantIdentifier)
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (@Nullable IParticipantIdentifier aParticipantID);

  /**
   * Get the service information for the passed tuple of service group and
   * document type identifier.
   *
   * @param aParticipantIdentifier
   *        The service group ID of interest. May be <code>null</code>.
   * @param aDocumentTypeIdentifier
   *        The document type identifier to search. May be <code>null</code>.
   * @return <code>null</code> if any parameter is <code>null</code> or if the
   *         service group is not found or if the document type is not found in
   *         the service group.
   */
  @Nullable
  ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable IParticipantIdentifier aParticipantIdentifier,
                                                                                @Nullable IDocumentTypeIdentifier aDocumentTypeIdentifier);

  /**
   * Check if the passed transport profile is used or not.
   *
   * @param sTransportProfileID
   *        The transport profile ID to be checked. May be <code>null</code>.
   * @return <code>true</code> if at least one endpoint uses the provided
   *         transport profile ID, <code>false</code> if not.
   */
  boolean containsAnyEndpointWithTransportProfile (@Nullable String sTransportProfileID);
}
