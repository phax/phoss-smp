/*
 * Copyright (C) 2015-2021 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.state.EChange;
import com.helger.dao.DAOException;
import com.helger.photon.app.dao.AbstractPhotonSimpleDAO;
import com.helger.settings.ISettings;
import com.helger.settings.Settings;
import com.helger.settings.exchange.xml.SettingsMicroDocumentConverter;
import com.helger.settings.factory.ISettingsFactory;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.MicroDocument;

/**
 * This class manages and persists the SMP settings.<br>
 * Use <code>SMPMetaManager.getSettingsMgr()</code> to get the singleton
 * instance.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class SMPSettingsManagerXML extends AbstractPhotonSimpleDAO implements ISMPSettingsManager
{
  private final SMPSettings m_aSettings = new SMPSettings ();
  private final CallbackList <ISMPSettingsCallback> m_aCallbacks = new CallbackList <> ();

  public SMPSettingsManagerXML (@Nullable final String sFilename) throws DAOException
  {
    super (sFilename);
    initialRead ();
  }

  @Override
  @Nonnull
  protected EChange onRead (@Nonnull final IMicroDocument aDoc)
  {
    final SettingsMicroDocumentConverter <Settings> aConverter = new SettingsMicroDocumentConverter <> (ISettingsFactory.newInstance ());
    final ISettings aSettings = aConverter.convertToNative (aDoc.getDocumentElement ());
    m_aSettings.initFromSettings (aSettings);
    return EChange.UNCHANGED;
  }

  @Override
  @Nonnull
  protected IMicroDocument createWriteData ()
  {
    final IMicroDocument ret = new MicroDocument ();
    final SettingsMicroDocumentConverter <Settings> aConverter = new SettingsMicroDocumentConverter <> (ISettingsFactory.newInstance ());
    ret.appendChild (aConverter.convertToMicroElement (m_aSettings.getAsSettings (), null, "root"));
    return ret;
  }

  @Nonnull
  @ReturnsMutableObject
  public final CallbackList <ISMPSettingsCallback> callbacks ()
  {
    return m_aCallbacks;
  }

  @Nonnull
  public ISMPSettings getSettings ()
  {
    return m_aSettings;
  }

  @Nonnull
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
      eChange = eChange.or (m_aSettings.setRESTWritableAPIDisabled (bRESTWritableAPIDisabled));
      eChange = eChange.or (m_aSettings.setDirectoryIntegrationRequired (bDirectoryIntegrationRequired));
      eChange = eChange.or (m_aSettings.setDirectoryIntegrationEnabled (bDirectoryIntegrationEnabled));
      eChange = eChange.or (m_aSettings.setDirectoryIntegrationAutoUpdate (bDirectoryIntegrationAutoUpdate));
      eChange = eChange.or (m_aSettings.setDirectoryHostName (sDirectoryHostName));
      eChange = eChange.or (m_aSettings.setSMLRequired (bSMLRequired));
      eChange = eChange.or (m_aSettings.setSMLEnabled (bSMLActive));
      eChange = eChange.or (m_aSettings.setSMLInfoID (sSMLInfoID));
      if (eChange.isChanged ())
        markAsChanged ();
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    // Invoke callbacks
    if (eChange.isChanged ())
      m_aCallbacks.forEach (x -> x.onSMPSettingsChanged (m_aSettings));

    return eChange;
  }
}
