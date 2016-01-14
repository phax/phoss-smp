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
package com.helger.peppol.smpserver.domain.servicegroup;

import java.util.Collection;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.IParticipantIdentifier;

/**
 * BAse interface for a manager for {@link ISMPServiceGroup} objects.
 * 
 * @author Philip Helger
 */
public interface ISMPServiceGroupManager
{
  @Nonnull
  ISMPServiceGroup createSMPServiceGroup (@Nonnull @Nonempty String sOwnerID,
                                          @Nonnull IParticipantIdentifier aParticipantIdentifier,
                                          @Nullable String sExtension);

  @Nonnull
  EChange updateSMPServiceGroup (@Nullable String sSMPServiceGroupID,
                                 @Nonnull String sOwnerID,
                                 @Nullable String sExtension);

  @Nonnull
  EChange deleteSMPServiceGroup (@Nullable IParticipantIdentifier aParticipantIdentifier);

  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPServiceGroup> getAllSMPServiceGroups ();

  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (@Nonnull String sOwnerID);

  @Nonnegative
  int getSMPServiceGroupCountOfOwner (@Nonnull String sOwnerID);

  @Nullable
  ISMPServiceGroup getSMPServiceGroupOfID (@Nullable IParticipantIdentifier aParticipantIdentifier);

  boolean containsSMPServiceGroupWithID (@Nullable IParticipantIdentifier aParticipantIdentifier);

  @Nonnegative
  int getSMPServiceGroupCount ();
}
