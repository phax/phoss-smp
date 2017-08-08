/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.domain.transportprofile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.dao.DAOException;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smp.SMPTransportProfile;
import com.helger.peppol.smpserver.domain.redirect.SMPRedirect;
import com.helger.photon.basic.app.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.basic.audit.AuditHelper;

public final class SMPTransportProfileManager extends
                                              AbstractPhotonMapBasedWALDAO <ISMPTransportProfile, SMPTransportProfile>
                                              implements
                                              ISMPTransportProfileManager
{
  public SMPTransportProfileManager (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPTransportProfile.class, sFilename);
  }

  @Override
  @Nonnull
  protected EChange onInit ()
  {
    // Add the default transport profiles
    for (final ESMPTransportProfile eTransportProfile : ESMPTransportProfile.values ())
      internalCreateItem (new SMPTransportProfile (eTransportProfile));
    return EChange.CHANGED;
  }

  @Nullable
  public ISMPTransportProfile createSMPTransportProfile (@Nonnull @Nonempty final String sID,
                                                         @Nonnull @Nonempty final String sName)
  {
    // Double ID needs to be taken care of
    if (containsWithID (sID))
      return null;

    final SMPTransportProfile aSMPTransportProfile = new SMPTransportProfile (sID, sName);

    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aSMPTransportProfile);
    });
    AuditHelper.onAuditCreateSuccess (SMPTransportProfile.OT, sID, sName);
    return aSMPTransportProfile;
  }

  @Nonnull
  public EChange updateSMPTransportProfile (@Nullable final String sSMPTransportProfileID,
                                            @Nonnull @Nonempty final String sName)
  {
    final SMPTransportProfile aSMPTransportProfile = getOfID (sSMPTransportProfileID);
    if (aSMPTransportProfile == null)
    {
      AuditHelper.onAuditModifyFailure (SMPTransportProfile.OT, sSMPTransportProfileID, "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (aSMPTransportProfile.setName (sName));
      if (eChange.isUnchanged ())
        return EChange.UNCHANGED;

      internalUpdateItem (aSMPTransportProfile);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (SMPTransportProfile.OT, "all", sSMPTransportProfileID, sName);
    return EChange.CHANGED;
  }

  @Nullable
  public EChange removeSMPTransportProfile (@Nullable final String sSMPTransportProfileID)
  {
    if (StringHelper.hasNoText (sSMPTransportProfileID))
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPTransportProfile aSMPTransportProfile = internalDeleteItem (sSMPTransportProfileID);
      if (aSMPTransportProfile == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPRedirect.OT, "no-such-id", sSMPTransportProfileID);
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (SMPRedirect.OT, sSMPTransportProfileID);
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPTransportProfile> getAllSMPTransportProfiles ()
  {
    return getAll ();
  }

  @Nullable
  public ISMPTransportProfile getSMPTransportProfileOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  public boolean containsSMPTransportProfileWithID (@Nullable final String sID)
  {
    return containsWithID (sID);
  }
}
