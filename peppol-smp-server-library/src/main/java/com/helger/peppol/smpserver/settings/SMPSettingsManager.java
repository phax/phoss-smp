/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.smpserver.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.state.EChange;
import com.helger.photon.basic.app.dao.impl.AbstractSimpleDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.settings.ISettings;
import com.helger.settings.exchange.xml.SettingsMicroDocumentConverter;
import com.helger.settings.factory.ISettingsFactory;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.MicroDocument;

/**
 * This class manages and persists the SMP settings.
 *
 * @author Philip Helger
 */
@ThreadSafe
@WorkInProgress
public class SMPSettingsManager extends AbstractSimpleDAO implements ISMPSettingsManager
{
  private final SMPSettings m_aSMPS = new SMPSettings ();

  public SMPSettingsManager (@Nullable final String sFilename) throws DAOException
  {
    super (sFilename);
    initialRead ();
  }

  @Override
  @Nonnull
  protected EChange onRead (@Nonnull final IMicroDocument aDoc)
  {
    final SettingsMicroDocumentConverter aConverter = new SettingsMicroDocumentConverter (ISettingsFactory.newInstance ());
    final ISettings aSettings = aConverter.convertToNative (aDoc.getDocumentElement ());
    m_aSMPS.setFromSettings (aSettings);
    return EChange.UNCHANGED;
  }

  @Override
  @Nonnull
  protected IMicroDocument createWriteData ()
  {
    final IMicroDocument ret = new MicroDocument ();
    final SettingsMicroDocumentConverter aConverter = new SettingsMicroDocumentConverter (ISettingsFactory.newInstance ());
    ret.appendChild (aConverter.convertToMicroElement (m_aSMPS.getAsSettings (), null, "root"));
    return ret;
  }

  @Nonnull
  public ISMPSettings getSettings ()
  {
    return m_aSMPS;
  }

  @Nonnull
  public EChange updateSettings (final boolean bRESTWritableAPIDisabled,
                                 final boolean bPEPPOLDirectoryIntegrationEnabled,
                                 @Nullable final String sPEPPOLDirectoryHostName,
                                 final boolean bWriteToSML,
                                 @Nullable final String sSMLURL)
  {
    EChange eChange = EChange.UNCHANGED;
    m_aRWLock.writeLock ().lock ();
    try
    {
      eChange = eChange.or (m_aSMPS.setRESTWritableAPIDisabled (bRESTWritableAPIDisabled));
      eChange = eChange.or (m_aSMPS.setPEPPOLDirectoryIntegrationEnabled (bPEPPOLDirectoryIntegrationEnabled));
      eChange = eChange.or (m_aSMPS.setPEPPOLDirectoryHostName (sPEPPOLDirectoryHostName));
      eChange = eChange.or (m_aSMPS.setWriteToSML (bWriteToSML));
      eChange = eChange.or (m_aSMPS.setSMLURL (sSMLURL));
      if (eChange.isChanged ())
        markAsChanged ();
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    return eChange;
  }
}
