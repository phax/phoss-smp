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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.compare.IComparator;
import com.helger.commons.id.IHasID;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.smpserver.domain.extension.ISMPHasExtension;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * This interface represents the main information in a service metadata, if no
 * redirect is present. It consists of a document type identifier (
 * {@link IDocumentTypeIdentifier}) and a list of processes (
 * {@link ISMPProcess}).
 *
 * @author Philip Helger
 */
public interface ISMPServiceInformation extends Serializable, ISMPHasExtension, IHasID <String>
{
  /**
   * @return The service group to which this service information belongs. Never
   *         <code>null</code>.
   */
  @Nonnull
  ISMPServiceGroup getServiceGroup ();

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
   * @param aProcess
   *        The process to be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if deletion was successfully,
   *         {@link EChange#UNCHANGED} otherwise.
   * @since 5.0.0
   */
  @Nonnull
  EChange deleteProcess (@Nullable ISMPProcess aProcess);

  /**
   * @return This service information object as a PEPPOL SMP JAXB object for the
   *         REST interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.peppol.smp.ServiceMetadataType getAsJAXBObjectPeppol ();

  /**
   * @return This service information object as a BDXR SMP JAXB object for the
   *         REST interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.peppol.bdxr.ServiceMetadataType getAsJAXBObjectBDXR ();

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
