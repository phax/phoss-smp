package com.helger.peppol.smpserver.domain.serviceinfo;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

public interface ISMPServiceInformationManager
{
  @Nonnull
  ISMPServiceInformation updateSMPServiceInformation (@Nullable String sServiceInfoID);

  @Nonnull
  ISMPServiceInformation createSMPServiceInformation (@Nonnull ISMPServiceGroup aServiceGroup,
                                                      @Nonnull IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                      @Nonnull List <SMPProcess> aProcesses,
                                                      @Nullable String sExtension);

  @Nullable
  ISMPServiceInformation findServiceInformation (@Nullable String sServiceGroupID,
                                                 @Nullable IPeppolDocumentTypeIdentifier aDocTypeID,
                                                 @Nullable IPeppolProcessIdentifier aProcessID,
                                                 @Nullable ISMPTransportProfile eTransportProfile);

  @Nonnull
  ISMPServiceInformation createSMPServiceInformation (@Nonnull SMPServiceInformation aServiceInformation);

  @Nonnull
  EChange deleteSMPServiceInformation (@Nullable ISMPServiceInformation aSMPServiceInformation);

  @Nonnull
  EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable ISMPServiceGroup aServiceGroup);

  @Nonnull
  EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable String sServiceGroupID);

  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPServiceInformation> getAllSMPServiceInformations ();

  @Nonnegative
  int getSMPServiceInformationCount ();

  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPServiceInformation> getAllSMPServiceInformationsOfServiceGroup (@Nullable ISMPServiceGroup aServiceGroup);

  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPServiceInformation> getAllSMPServiceInformationsOfServiceGroup (@Nullable String sServiceGroupID);

  @Nullable
  ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable String sServiceGroupID,
                                                                                @Nullable IDocumentTypeIdentifier aDocumentTypeIdentifier);

  @Nullable
  ISMPServiceInformation getSMPServiceInformationOfID (@Nullable String sID);

  boolean containsSMPServiceInformationWithID (@Nullable String sID);
}
