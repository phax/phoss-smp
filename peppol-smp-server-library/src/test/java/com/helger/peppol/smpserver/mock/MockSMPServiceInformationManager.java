/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.mock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;

/**
 * Mock implementation of {@link ISMPServiceInformationManager}.
 *
 * @author Philip Helger
 */
final class MockSMPServiceInformationManager implements ISMPServiceInformationManager
{
  @Nonnull
  public ESuccess mergeSMPServiceInformation (final ISMPServiceInformation aServiceInformation)
  {
    return ESuccess.SUCCESS;
  }

  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (final ISMPServiceGroup aServiceGroup,
                                                                                       final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    throw new UnsupportedOperationException ();
  }

  public int getSMPServiceInformationCount ()
  {
    return 0;
  }

  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformationOfServiceGroup (final ISMPServiceGroup aServiceGroup)
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation ()
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsList <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (final ISMPServiceGroup aServiceGroup)
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

  @Nonnull
  public EChange deleteSMPProcess (@Nullable final ISMPServiceInformation aSMPServiceInformation,
                                   @Nullable final ISMPProcess aProcess)
  {
    throw new UnsupportedOperationException ();
  }
}
