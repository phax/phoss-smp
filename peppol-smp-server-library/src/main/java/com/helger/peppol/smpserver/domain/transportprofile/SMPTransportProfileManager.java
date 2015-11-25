/**
 * Copyright (C) 2012-2014 Philip Helger <philip[at]helger[dot]com>
 * All Rights Reserved
 *
 * This file is part of the Ecoware Online Shop.
 *
 * Proprietary and confidential.
 *
 * It can not be copied and/or distributed without the
 * express permission of Philip Helger.
 *
 * Unauthorized copying of this file, via any medium is
 * strictly prohibited.
 */
package com.helger.peppol.smpserver.domain.transportprofile;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroDocument;
import com.helger.commons.microdom.convert.MicroTypeConverter;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smp.SMPTransportProfile;
import com.helger.peppol.smpserver.domain.redirect.SMPRedirect;
import com.helger.photon.basic.app.dao.impl.AbstractWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.dao.impl.EDAOActionType;
import com.helger.photon.basic.audit.AuditHelper;

public final class SMPTransportProfileManager extends AbstractWALDAO <SMPTransportProfile>
{
  private static final String ELEMENT_ROOT = "transportprofiles";
  private static final String ELEMENT_ITEM = "transportprofile";

  private final Map <String, SMPTransportProfile> m_aMap = new HashMap <String, SMPTransportProfile> ();

  public SMPTransportProfileManager (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPTransportProfile.class, sFilename);
    initialRead ();
  }

  @Override
  protected void onRecoveryCreate (@Nonnull final SMPTransportProfile aSMPTransportProfile)
  {
    _addItem (aSMPTransportProfile);
  }

  @Override
  protected void onRecoveryUpdate (@Nonnull final SMPTransportProfile aSMPTransportProfile)
  {
    _addItem (aSMPTransportProfile);
  }

  @Override
  protected void onRecoveryDelete (@Nonnull final SMPTransportProfile aSMPTransportProfile)
  {
    m_aMap.remove (aSMPTransportProfile.getID ());
  }

  @Override
  @Nonnull
  protected EChange onInit ()
  {
    // Add the default transport profiles
    for (final ESMPTransportProfile eTransportProfile : ESMPTransportProfile.values ())
      _addItem (new SMPTransportProfile (eTransportProfile));
    return EChange.CHANGED;
  }

  @Override
  @Nonnull
  protected EChange onRead (@Nonnull final IMicroDocument aDoc)
  {
    for (final IMicroElement eSMPTransportProfile : aDoc.getDocumentElement ().getAllChildElements (ELEMENT_ITEM))
      _addItem (MicroTypeConverter.convertToNative (eSMPTransportProfile, SMPTransportProfile.class));
    return EChange.UNCHANGED;
  }

  @Override
  @Nonnull
  protected IMicroDocument createWriteData ()
  {
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.appendElement (ELEMENT_ROOT);
    for (final SMPTransportProfile aSMPTransportProfile : CollectionHelper.getSortedByKey (m_aMap).values ())
      eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aSMPTransportProfile, ELEMENT_ITEM));
    return aDoc;
  }

  private void _addItem (@Nonnull final SMPTransportProfile aSMPTransportProfile)
  {
    ValueEnforcer.notNull (aSMPTransportProfile, "SMPTransportProfile");

    final String sSMPTransportProfileID = aSMPTransportProfile.getID ();
    if (m_aMap.containsKey (sSMPTransportProfileID))
      throw new IllegalArgumentException ("SMPTransportProfile ID '" + sSMPTransportProfileID + "' is already in use!");
    m_aMap.put (sSMPTransportProfileID, aSMPTransportProfile);
  }

  @Nullable
  public ISMPTransportProfile createSMPTransportProfile (@Nonnull @Nonempty final String sID, @Nonnull @Nonempty final String sName)
  {
    final SMPTransportProfile aSMPTransportProfile = new SMPTransportProfile (sID, sName);

    m_aRWLock.writeLock ().lock ();
    try
    {
      // Double ID needs to be taken care of
      if (containsSMPTransportProfileWithID (sID))
        return null;

      _addItem (aSMPTransportProfile);
      markAsChanged (aSMPTransportProfile, EDAOActionType.CREATE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditCreateSuccess (SMPTransportProfile.OT, sID, sName);
    return aSMPTransportProfile;
  }

  @Nonnull
  public EChange updateSMPTransportProfile (@Nullable final String sSMPTransportProfileID, @Nonnull @Nonempty final String sName)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPTransportProfile aSMPTransportProfile = m_aMap.get (sSMPTransportProfileID);
      if (aSMPTransportProfile == null)
      {
        AuditHelper.onAuditModifyFailure (SMPTransportProfile.OT, sSMPTransportProfileID, "no-such-id");
        return EChange.UNCHANGED;
      }

      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (aSMPTransportProfile.setName (sName));
      if (eChange.isUnchanged ())
        return EChange.UNCHANGED;

      markAsChanged (aSMPTransportProfile, EDAOActionType.UPDATE);
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
      final SMPTransportProfile aSMPTransportProfile = m_aMap.remove (sSMPTransportProfileID);
      if (aSMPTransportProfile == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPRedirect.OT, "no-such-id", sSMPTransportProfileID);
        return EChange.UNCHANGED;
      }

      markAsChanged (aSMPTransportProfile, EDAOActionType.DELETE);
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
  public Collection <? extends ISMPTransportProfile> getAllSMPTransportProfiles ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return CollectionHelper.newList (m_aMap.values ());
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nullable
  public ISMPTransportProfile getSMPTransportProfileOfID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;

    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aMap.get (sID);
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  public boolean containsSMPTransportProfileWithID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return false;

    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aMap.containsKey (sID);
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }
}
