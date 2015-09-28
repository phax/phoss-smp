package com.helger.peppol.smpserver.data.xml.domain;

import java.util.Collection;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.photon.basic.security.user.IUser;

public interface ISMPServiceGroupManager
{
  @Nonnull
  ISMPServiceGroup createSMPServiceGroup (IUser aOwner,
                                          IParticipantIdentifier aParticipantIdentifier,
                                          @Nullable String sExtension);

  @Nonnull
  EChange updateSMPServiceGroup (@Nullable String sSMPServiceGroupID, String sOwnerID, @Nullable String sExtension);

  @Nonnull
  EChange deleteSMPServiceGroup (@Nullable IParticipantIdentifier aParticipantIdentifier);

  @Nonnull
  EChange deleteSMPServiceGroup (@Nullable ISMPServiceGroup aSMPServiceGroup);

  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPServiceGroup> getAllSMPServiceGroups ();

  @Nullable
  ISMPServiceGroup getSMPServiceGroupOfID (@Nullable IParticipantIdentifier aParticipantIdentifier);

  @Nullable
  ISMPServiceGroup getSMPServiceGroupOfID (@Nullable String sID);

  boolean containsSMPServiceGroupWithID (@Nullable String sID);

  @Nonnegative
  int getSMPServiceGroupCount ();
}
