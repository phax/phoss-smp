package com.helger.peppol.smpserver.mock;

import com.helger.commons.collection.ext.ICommonsCollection;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;

/**
 * Mock implementation of {@link ISMPServiceInformationManager}.
 * 
 * @author Philip Helger
 */
final class MockSMPServiceInformationManager implements ISMPServiceInformationManager
{
  public void mergeSMPServiceInformation (final ISMPServiceInformation aServiceInformation)
  {}

  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (final ISMPServiceGroup aServiceGroup,
                                                                                       final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    throw new UnsupportedOperationException ();
  }

  public int getSMPServiceInformationCount ()
  {
    return 0;
  }

  public ICommonsCollection <? extends ISMPServiceInformation> getAllSMPServiceInformationsOfServiceGroup (final ISMPServiceGroup aServiceGroup)
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsCollection <? extends ISMPServiceInformation> getAllSMPServiceInformation ()
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsCollection <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (final ISMPServiceGroup aServiceGroup)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPServiceInformation findServiceInformation (final ISMPServiceGroup aServiceGroup,
                                                        final IDocumentTypeIdentifier aDocTypeID,
                                                        final IProcessIdentifier aProcessID,
                                                        final ISMPTransportProfile aTransportProfile)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteSMPServiceInformation (final ISMPServiceInformation aSMPServiceInformation)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteAllSMPServiceInformationOfServiceGroup (final ISMPServiceGroup aServiceGroup)
  {
    throw new UnsupportedOperationException ();
  }
}
