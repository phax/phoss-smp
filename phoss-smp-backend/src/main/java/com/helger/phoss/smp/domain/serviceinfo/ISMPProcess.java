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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.compare.IComparator;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.smp.domain.extension.ISMPHasExtension;

/**
 * This interface represents a single process information within a service information. A process
 * consists of a process identifier ( {@link IProcessIdentifier}) and a list of endpoints (
 * {@link ISMPEndpoint}). It is contained in an {@link ISMPServiceInformation}.
 *
 * @author Philip Helger
 */
public interface ISMPProcess extends ISMPHasExtension
{
  /**
   * @return The process identifier of this process. Never <code>null</code>.
   */
  @NonNull
  IProcessIdentifier getProcessIdentifier ();

  /**
   * @return A copy of the list of all endpoints associated with this process. Never
   *         <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ISMPEndpoint> getAllEndpoints ();

  /**
   * @return The number of contained endpoint information. Always &ge; 0.
   */
  @Nonnegative
  int getEndpointCount ();

  /**
   * Find the endpoint with the passed transport profile ID.
   *
   * @param sTransportProfileID
   *        The transport profile ID to search. May be <code>null</code>.
   * @return <code>null</code> if the passed transport profile is <code>null</code> or empty or if
   *         no such endpoint exists.
   */
  @Nullable
  ISMPEndpoint getEndpointOfTransportProfile (@Nullable String sTransportProfileID);

  /**
   * Check if the passed transport profile is used or not.
   *
   * @param sTransportProfileID
   *        The transport profile ID to be checked. May be <code>null</code>.
   * @return <code>true</code> if at least one endpoint uses the provided transport profile ID,
   *         <code>false</code> if not.
   */
  boolean containsAnyEndpointWithTransportProfile (@Nullable String sTransportProfileID);

  /**
   * Add a new endpoint.
   *
   * @param aEndpoint
   *        The endpoint to be added. May not be <code>null</code>.
   * @throws IllegalArgumentException
   *         If another endpoint with the same transport profile already exists-
   */
  void addEndpoint (@NonNull SMPEndpoint aEndpoint);

  /**
   * Add a new endpoint overwriting any eventually present endpoint with the same transport profile.
   *
   * @param aEndpoint
   *        The endpoint to be added. May not be <code>null</code>.
   */
  void setEndpoint (@NonNull SMPEndpoint aEndpoint);

  @NonNull
  EChange deleteEndpoint (@Nullable String sTransportProfile);

  /**
   * @return This service information object as a Peppol SMP JAXB object for the REST interface. May
   *         be <code>null</code> if no endpoint is contained.
   */
  com.helger.xsds.peppol.smp1.@Nullable ProcessType getAsJAXBObjectPeppol ();

  /**
   * @return This service information object as a BDXR SMP v1 JAXB object for the REST interface.
   *         May be <code>null</code> if no endpoint is contained.
   */
  com.helger.xsds.bdxr.smp1.@Nullable ProcessType getAsJAXBObjectBDXR1 ();

  /**
   * @return This service information object as a BDXR SMP v2 JAXB object for the REST interface.
   *         May be <code>null</code> if no endpoint is contained.
   */
  com.helger.xsds.bdxr.smp2.ac.@Nullable ProcessMetadataType getAsJAXBObjectBDXR2 ();

  @NonNull
  static IComparator <ISMPProcess> comparator ()
  {
    return (aElement1, aElement2) -> aElement1.getProcessIdentifier ().compareTo (aElement2.getProcessIdentifier ());
  }
}
