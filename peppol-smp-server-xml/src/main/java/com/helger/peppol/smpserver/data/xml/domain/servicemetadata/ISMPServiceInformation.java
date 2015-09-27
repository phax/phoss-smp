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
package com.helger.peppol.smpserver.data.xml.domain.servicemetadata;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.id.IHasID;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.smp.ServiceMetadataType;
import com.helger.peppol.smpserver.data.xml.domain.ISMPHasExtension;
import com.helger.peppol.smpserver.data.xml.domain.servicegroup.ISMPServiceGroup;

/**
 * This interface represents the main information in a service metadata, if no
 * redirect is present. It consists of a document type identifier (
 * {@link IPeppolDocumentTypeIdentifier}) and a list of processes (
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
  IPeppolDocumentTypeIdentifier getDocumentTypeIdentifier ();

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
  ISMPProcess getProcessOfID (@Nullable IPeppolProcessIdentifier aProcessID);

  /**
   * @return A copy of the list of all processes associated with this service
   *         information.
   */
  @Nonnull
  @ReturnsMutableCopy
  List <? extends ISMPProcess> getAllProcesses ();

  /**
   * @return The overall endpoint count for all processes in this object. Always
   *         &ge; 0.
   */
  @Nonnegative
  int getTotalEndpointCount ();

  @Nonnull
  ServiceMetadataType getAsJAXBObject ();
}
