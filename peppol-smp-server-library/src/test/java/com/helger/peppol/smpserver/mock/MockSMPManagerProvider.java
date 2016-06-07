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
package com.helger.peppol.smpserver.mock;

import java.util.List;

import com.helger.commons.collection.ext.ICommonsCollection;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.ISMPManagerProvider;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardEntity;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * This {@link ISMPManagerProvider} implementation returns non-<code>null</code>
 * managers that all do nothing. This is only needed to access the identifier
 * factory.
 *
 * @author Philip Helger
 */
public class MockSMPManagerProvider implements ISMPManagerProvider
{
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    return new ISMPTransportProfileManager ()
    {
      public EChange updateSMPTransportProfile (final String sSMPTransportProfileID, final String sName)
      {
        throw new UnsupportedOperationException ();
      }

      public EChange removeSMPTransportProfile (final String sSMPTransportProfileID)
      {
        throw new UnsupportedOperationException ();
      }

      public ISMPTransportProfile getSMPTransportProfileOfID (final String sID)
      {
        throw new UnsupportedOperationException ();
      }

      public ICommonsCollection <? extends ISMPTransportProfile> getAllSMPTransportProfiles ()
      {
        throw new UnsupportedOperationException ();
      }

      public ISMPTransportProfile createSMPTransportProfile (final String sID, final String sName)
      {
        throw new UnsupportedOperationException ();
      }

      public boolean containsSMPTransportProfileWithID (final String sID)
      {
        return false;
      }
    };
  }

  public ISMPUserManager createUserMgr ()
  {
    return new ISMPUserManager ()
    {
      public Object verifyOwnership (final IParticipantIdentifier aServiceGroupID,
                                     final ISMPUser aCurrentUser) throws SMPNotFoundException, SMPUnauthorizedException
      {
        throw new UnsupportedOperationException ();
      }

      public ISMPUser validateUserCredentials (final BasicAuthClientCredentials aCredentials) throws Throwable
      {
        throw new UnsupportedOperationException ();
      }

      public void updateUser (final String sUserName, final String sPassword)
      {}

      public boolean isSpecialUserManagementNeeded ()
      {
        return false;
      }

      public ISMPUser getUserOfID (final String sUserID)
      {
        throw new UnsupportedOperationException ();
      }

      public int getUserCount ()
      {
        return 0;
      }

      public ICommonsCollection <? extends ISMPUser> getAllUsers ()
      {
        throw new UnsupportedOperationException ();
      }

      public void deleteUser (final String sUserName)
      {}

      public void createUser (final String sUserName, final String sPassword)
      {}
    };
  }

  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return new ISMPServiceGroupManager ()
    {
      public EChange updateSMPServiceGroup (final String sSMPServiceGroupID,
                                            final String sOwnerID,
                                            final String sExtension)
      {
        throw new UnsupportedOperationException ();
      }

      public ISMPServiceGroup getSMPServiceGroupOfID (final IParticipantIdentifier aParticipantIdentifier)
      {
        throw new UnsupportedOperationException ();
      }

      public int getSMPServiceGroupCountOfOwner (final String sOwnerID)
      {
        return 0;
      }

      public int getSMPServiceGroupCount ()
      {
        return 0;
      }

      public ICommonsCollection <? extends ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (final String sOwnerID)
      {
        throw new UnsupportedOperationException ();
      }

      public ICommonsCollection <? extends ISMPServiceGroup> getAllSMPServiceGroups ()
      {
        throw new UnsupportedOperationException ();
      }

      public EChange deleteSMPServiceGroup (final IParticipantIdentifier aParticipantIdentifier)
      {
        throw new UnsupportedOperationException ();
      }

      public ISMPServiceGroup createSMPServiceGroup (final String sOwnerID,
                                                     final IParticipantIdentifier aParticipantIdentifier,
                                                     final String sExtension)
      {
        throw new UnsupportedOperationException ();
      }

      public boolean containsSMPServiceGroupWithID (final IParticipantIdentifier aParticipantIdentifier)
      {
        return false;
      }
    };
  }

  public ISMPRedirectManager createRedirectMgr ()
  {
    return new ISMPRedirectManager ()
    {
      public ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (final ISMPServiceGroup aServiceGroup,
                                                                       final IDocumentTypeIdentifier aDocTypeID)
      {
        throw new UnsupportedOperationException ();
      }

      public int getSMPRedirectCount ()
      {
        return 0;
      }

      public ICommonsCollection <? extends ISMPRedirect> getAllSMPRedirectsOfServiceGroup (final ISMPServiceGroup aServiceGroup)
      {
        throw new UnsupportedOperationException ();
      }

      public ICommonsCollection <? extends ISMPRedirect> getAllSMPRedirects ()
      {
        throw new UnsupportedOperationException ();
      }

      public EChange deleteSMPRedirect (final ISMPRedirect aSMPRedirect)
      {
        throw new UnsupportedOperationException ();
      }

      public EChange deleteAllSMPRedirectsOfServiceGroup (final ISMPServiceGroup aServiceGroup)
      {
        throw new UnsupportedOperationException ();
      }

      public ISMPRedirect createOrUpdateSMPRedirect (final ISMPServiceGroup aServiceGroup,
                                                     final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                     final String sTargetHref,
                                                     final String sSubjectUniqueIdentifier,
                                                     final String sExtension)
      {
        throw new UnsupportedOperationException ();
      }
    };
  }

  public ISMPServiceInformationManager createServiceInformationMgr ()
  {
    return new ISMPServiceInformationManager ()
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
    };
  }

  public ISMPBusinessCardManager createBusinessCardMgr ()
  {
    return new ISMPBusinessCardManager ()
    {
      public ISMPBusinessCard getSMPBusinessCardOfServiceGroup (final ISMPServiceGroup aServiceGroup)
      {
        throw new UnsupportedOperationException ();
      }

      public ISMPBusinessCard getSMPBusinessCardOfID (final String sID)
      {
        throw new UnsupportedOperationException ();
      }

      public int getSMPBusinessCardCount ()
      {
        return 0;
      }

      public ICommonsCollection <? extends ISMPBusinessCard> getAllSMPBusinessCards ()
      {
        throw new UnsupportedOperationException ();
      }

      public EChange deleteSMPBusinessCard (final ISMPBusinessCard aSMPBusinessCard)
      {
        throw new UnsupportedOperationException ();
      }

      public ISMPBusinessCard createOrUpdateSMPBusinessCard (final ISMPServiceGroup aServiceGroup,
                                                             final List <SMPBusinessCardEntity> aEntities)
      {
        throw new UnsupportedOperationException ();
      }
    };
  }
}
