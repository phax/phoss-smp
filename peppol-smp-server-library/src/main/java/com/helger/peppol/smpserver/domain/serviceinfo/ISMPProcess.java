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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.compare.IComparator;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.extension.ISMPHasExtension;

/**
 * This interface represents a single process information within a service
 * information. A process consists of a process identifier (
 * {@link IProcessIdentifier}) and a list of endpoints ( {@link ISMPEndpoint}).
 * It is contained in an {@link ISMPServiceInformation}.
 *
 * @author Philip Helger
 */
public interface ISMPProcess extends Serializable, ISMPHasExtension
{
  /**
   * @return The process identifier of this process. Never <code>null</code>.
   */
  @Nonnull
  IProcessIdentifier getProcessIdentifier ();

  /**
   * @return A copy of the list of all endpoints associated with this process.
   *         Never <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPEndpoint> getAllEndpoints ();

  /**
   * @return The number of contained endpoint information. Always &ge; 0.
   */
  @Nonnegative
  int getEndpointCount ();

  /**
   * Find the endpoint with the passed transport profile.
   *
   * @param aTransportProfile
   *        The transport profile to search. May be <code>null</code>.
   * @return <code>null</code> if the passed transport profile is
   *         <code>null</code> or if no such endpoint exists.
   */
  @Nullable
  ISMPEndpoint getEndpointOfTransportProfile (@Nullable ISMPTransportProfile aTransportProfile);

  /**
   * Find the endpoint with the passed transport profile ID.
   *
   * @param sTransportProfileID
   *        The transport profile ID to search. May be <code>null</code>.
   * @return <code>null</code> if the passed transport profile is
   *         <code>null</code> or empty or if no such endpoint exists.
   */
  @Nullable
  ISMPEndpoint getEndpointOfTransportProfile (@Nullable String sTransportProfileID);

  /**
   * Add a new endpoint.
   *
   * @param aEndpoint
   *        The endpoint to be added. May not be <code>null</code>.
   * @throws IllegalArgumentException
   *         If another endpoint with the same transport profile already exists-
   */
  void addEndpoint (@Nonnull SMPEndpoint aEndpoint);

  /**
   * Add a new endpoint overwriting any eventually present endpoint with the
   * same transport profile.
   *
   * @param aEndpoint
   *        The endpoint to be added. May not be <code>null</code>.
   */
  void setEndpoint (@Nonnull SMPEndpoint aEndpoint);

  @Nonnull
  EChange deleteEndpoint (@Nullable String sTransportProfile);

  /**
   * @return This service information object as a PEPPOL SMP JAXB object for the
   *         REST interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.peppol.smp.ProcessType getAsJAXBObjectPeppol ();

  /**
   * @return This service information object as a BDXR SMP JAXB object for the
   *         REST interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.peppol.bdxr.ProcessType getAsJAXBObjectBDXR ();

  @Nonnull
  static IComparator <ISMPProcess> comparator ()
  {
    return (aElement1, aElement2) -> aElement1.getProcessIdentifier ().compareTo (aElement2.getProcessIdentifier ());
  }
}
