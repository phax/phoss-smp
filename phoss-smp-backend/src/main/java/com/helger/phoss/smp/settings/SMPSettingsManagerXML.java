/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.settings;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.dao.DAOException;
import com.helger.photon.io.dao.AbstractPhotonSimpleDAO;
import com.helger.settings.ISettings;
import com.helger.settings.Settings;
import com.helger.settings.exchange.xml.SettingsMicroDocumentConverter;
import com.helger.settings.factory.ISettingsFactory;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.MicroDocument;

/**
 * This class manages and persists the SMP settings.<br>
 * Use <code>SMPMetaManager.getSettingsMgr()</code> to get the singleton instance.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class SMPSettingsManagerXML extends AbstractPhotonSimpleDAO implements ISMPSettingsManager
{
  private final SMPSettings m_aSMPSettings = SMPSettings.createInitializedFromConfiguration ();
  private final CallbackList <ISMPSettingsCallback> m_aCallbacks = new CallbackList <> ();

  public SMPSettingsManagerXML (@Nullable final String sFilename) throws DAOException
  {
    super (sFilename);
    initialRead ();
  }

  @Override
  @NonNull
  protected EChange onRead (@NonNull final IMicroDocument aDoc)
  {
    final SettingsMicroDocumentConverter <Settings> aConverter = new SettingsMicroDocumentConverter <> (ISettingsFactory.newInstance ());
    final ISettings aSettings = aConverter.convertToNative (aDoc.getDocumentElement ());
    m_aSMPSettings.internalSetFromSettings (aSettings);
    return EChange.UNCHANGED;
  }

  @Override
  @NonNull
  protected IMicroDocument createWriteData ()
  {
    final IMicroDocument ret = new MicroDocument ();
    final SettingsMicroDocumentConverter <Settings> aConverter = new SettingsMicroDocumentConverter <> (ISettingsFactory.newInstance ());
    ret.addChild (aConverter.convertToMicroElement (m_aSMPSettings.internalGetAsMutableSettings (), null, "root"));
    return ret;
  }

  @NonNull
  @ReturnsMutableObject
  public final CallbackList <ISMPSettingsCallback> callbacks ()
  {
    return m_aCallbacks;
  }

  @NonNull
  public ISMPSettings getSettings ()
  {
    return m_aSMPSettings;
  }

  @NonNull
  public EChange updateSettings (final boolean bRESTWritableAPIDisabled,
                                 final boolean bDirectoryIntegrationEnabled,
                                 final boolean bDirectoryIntegrationRequired,
                                 final boolean bDirectoryIntegrationAutoUpdate,
                                 @Nullable final String sDirectoryHostName,
                                 final boolean bSMLActive,
                                 final boolean bSMLRequired,
                                 @Nullable final String sSMLInfoID)
  {
    EChange eChange = EChange.UNCHANGED;
    m_aRWLock.writeLock ().lock ();
    try
    {
      eChange = eChange.or (m_aSMPSettings.setRESTWritableAPIDisabled (bRESTWritableAPIDisabled));
      eChange = eChange.or (m_aSMPSettings.setDirectoryIntegrationRequired (bDirectoryIntegrationRequired));
      eChange = eChange.or (m_aSMPSettings.setDirectoryIntegrationEnabled (bDirectoryIntegrationEnabled));
      eChange = eChange.or (m_aSMPSettings.setDirectoryIntegrationAutoUpdate (bDirectoryIntegrationAutoUpdate));
      eChange = eChange.or (m_aSMPSettings.setDirectoryHostName (sDirectoryHostName));
      eChange = eChange.or (m_aSMPSettings.setSMLRequired (bSMLRequired));
      eChange = eChange.or (m_aSMPSettings.setSMLEnabled (bSMLActive));
      eChange = eChange.or (m_aSMPSettings.setSMLInfoID (sSMLInfoID));
      if (eChange.isChanged ())
        markAsChanged ();
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    // Invoke callbacks
    if (eChange.isChanged ())
      m_aCallbacks.forEach (x -> x.onSMPSettingsChanged (m_aSMPSettings));

    return eChange;
  }
}
