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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.compare.IComparator;
import com.helger.commons.id.IHasID;
import com.helger.commons.state.EChange;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.smp.domain.extension.ISMPHasExtension;

/**
 * This interface represents the main information in a service metadata, if no
 * redirect is present. It consists of a document type identifier (
 * {@link IDocumentTypeIdentifier}) and a list of processes (
 * {@link ISMPProcess}).
 *
 * @author Philip Helger
 */
public interface ISMPServiceInformation extends ISMPHasExtension, IHasID <String>
{
  /**
   * @return The participant ID of the service group to which this service
   *         information belongs. Never <code>null</code>.
   */
  @Nonnull
  IParticipantIdentifier getServiceGroupParticipantIdentifier ();

  /**
   * @return The ID of the service group to which this service information
   *         belongs. Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getServiceGroupID ();

  /**
   * @return The document type identifier of this service information. Never
   *         <code>null</code>.
   */
  @Nonnull
  IDocumentTypeIdentifier getDocumentTypeIdentifier ();

  /**
   * @return The number of contained process information. Always &ge; 0.
   */
  @Nonnegative
  int getProcessCount ();

  /**
   * Get the process with the specified ID
   *
   * @param aProcessID
   *        The process ID to search. May be <code>null</code>.
   * @return <code>null</code> if no such process exists
   */
  @Nullable
  ISMPProcess getProcessOfID (@Nullable IProcessIdentifier aProcessID);

  /**
   * @return A copy of the list of all processes associated with this service
   *         information.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPProcess> getAllProcesses ();

  /**
   * @return The overall endpoint count for all processes in this object. Always
   *         &ge; 0.
   */
  @Nonnegative
  int getTotalEndpointCount ();

  /**
   * Check if the passed transport profile is used or not.
   *
   * @param sTransportProfileID
   *        The transport profile ID to be checked. May be <code>null</code>.
   * @return <code>true</code> if at least one endpoint uses the provided
   *         transport profile ID, <code>false</code> if not.
   */
  boolean containsAnyEndpointWithTransportProfile (@Nullable String sTransportProfileID);

  /**
   * Add the passed process.
   *
   * @param aProcess
   *        The process to be added. May not be <code>null</code>.
   * @throws IllegalArgumentException
   *         If a process with the same process ID is already registered.
   */
  void addProcess (@Nonnull SMPProcess aProcess);

  /**
   * Delete the provided process from this object.
   *
   * @param aProcessID
   *        The process ID to be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if deletion was successfully,
   *         {@link EChange#UNCHANGED} otherwise.
   * @since 5.0.0
   */
  @Nonnull
  EChange deleteProcess (@Nullable IProcessIdentifier aProcessID);

  /**
   * @return This service information object as a Peppol SMP JAXB object for the
   *         REST interface. May be <code>null</code> if invalid XML would be
   *         created.
   */
  @Nullable
  com.helger.xsds.peppol.smp1.ServiceMetadataType getAsJAXBObjectPeppol ();

  /**
   * @return This service information object as a BDXR SMP v1 JAXB object for
   *         the REST interface. May be <code>null</code> if invalid XML would
   *         be created.
   */
  @Nullable
  com.helger.xsds.bdxr.smp1.ServiceMetadataType getAsJAXBObjectBDXR1 ();

  /**
   * @return This service information object as a BDXR SMP v2 JAXB object for
   *         the REST interface. May be <code>null</code> if invalid XML would
   *         be created.
   */
  @Nullable
  com.helger.xsds.bdxr.smp2.ServiceMetadataType getAsJAXBObjectBDXR2 ();

  @Nonnull
  static IComparator <ISMPServiceInformation> comparator ()
  {
    return (aElement1, aElement2) -> {
      int ret = aElement1.getServiceGroupID ().compareTo (aElement2.getServiceGroupID ());
      if (ret == 0)
        ret = aElement1.getDocumentTypeIdentifier ().compareTo (aElement2.getDocumentTypeIdentifier ());
      return ret;
    };
  }
}
