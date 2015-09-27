/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.smpserver.domain.servicemetadata;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ProcessType;
import com.helger.peppol.smpserver.domain.ISMPHasExtension;

/**
 * This interface represents a single process information within a service
 * information. A process consists of a process identifier (
 * {@link IPeppolProcessIdentifier}) and a list of endpoints (
 * {@link ISMPEndpoint}). It is contained in an {@link ISMPServiceInformation}.
 *
 * @author Philip Helger
 */
public interface ISMPProcess extends Serializable, ISMPHasExtension
{
  /**
   * @return The process identifier of this process. Never <code>null</code>.
   */
  @Nonnull
  IPeppolProcessIdentifier getProcessIdentifier ();

  /**
   * @return The number of contained endpoint information. Always &ge; 0.
   */
  @Nonnegative
  int getEndpointCount ();

  @Nullable
  ISMPEndpoint getEndpointOfTransportProfile (@Nullable ESMPTransportProfile eTransportProfile);

  @Nullable
  ISMPEndpoint getEndpointOfTransportProfile (@Nullable String sTransportProfile);

  /**
   * @return A copy of the list of all endpoints associated with this process.
   */
  @Nonnull
  @ReturnsMutableCopy
  List <? extends ISMPEndpoint> getAllEndpoints ();

  @Nonnull
  ProcessType getAsJAXBObject ();
}
