package com.helger.peppol.smpserver.data.xml.domain;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;

public interface ISMPServiceInformationManager
{
  ISMPServiceInformation updateSMPServiceInformation (@Nullable String sServiceInfoID);

  ISMPServiceInformation createSMPServiceInformation (ISMPServiceGroup aServiceGroup,
                                                      IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                      List <SMPProcess> aProcesses,
                                                      @Nullable String sExtension);

  ISMPServiceInformation findServiceInformation (@Nullable String sServiceGroupID,
                                                 @Nullable IPeppolDocumentTypeIdentifier aDocTypeID,
                                                 @Nullable IPeppolProcessIdentifier aProcessID,
                                                 @Nullable ESMPTransportProfile eTransportProfile);

  @Nonnull
  ISMPServiceInformation createSMPServiceInformation (SMPServiceInformation aServiceInformation);

  @Nonnull
  EChange deleteSMPServiceInformation (@Nullable ISMPServiceInformation aSMPServiceInformation);

  @Nonnull
  EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable ISMPServiceGroup aServiceGroup);

  @Nonnull
  EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable String sServiceGroupID);

  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPServiceInformation> getAllSMPServiceInformations ();

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
