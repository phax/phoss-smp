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
package com.helger.peppol.smpserver.domain.redirect;

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.smp.ServiceMetadataType;
import com.helger.peppol.smpserver.domain.ISMPHasExtension;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * This interface represents a single SMP redirect for a certain document type
 * identifier ({@link IPeppolDocumentTypeIdentifier}).
 *
 * @author Philip Helger
 */
public interface ISMPRedirect extends IHasID <String>, Serializable, ISMPHasExtension
{
  /**
   * @return The service group which this redirect should handle.
   */
  @Nonnull
  ISMPServiceGroup getServiceGroup ();

  /**
   * @return The ID of the service group to which this redirect belongs. Never
   *         <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getServiceGroupID ();

  /**
   * @return The document type identifier of this redirect. Never
   *         <code>null</code>.
   */
  @Nonnull
  IPeppolDocumentTypeIdentifier getDocumentTypeIdentifier ();

  /**
   * @return The destination href of the new SMP. Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getTargetHref ();

  /**
   * @return The subject unique identifier of the target SMPs certificate used
   *         to sign its resources.
   */
  @Nonnull
  @Nonempty
  String getSubjectUniqueIdentifier ();

  @Nonnull
  ServiceMetadataType getAsJAXBObject ();
}
