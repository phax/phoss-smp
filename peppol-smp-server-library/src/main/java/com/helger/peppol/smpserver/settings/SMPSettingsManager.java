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
package com.helger.peppol.smpserver.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.callback.CallbackList;
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
  private final CallbackList <ISMPSettingsCallback> m_aCallbacks = new CallbackList<> ();

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
  @ReturnsMutableObject ("by design")
  public final CallbackList <ISMPSettingsCallback> getCallbacks ()
  {
    return m_aCallbacks;
  }

  @Nonnull
  public ISMPSettings getSettings ()
  {
    return m_aSMPS;
  }

  @Nonnull
  public EChange updateSettings (final boolean bRESTWritableAPIDisabled,
                                 final boolean bPEPPOLDirectoryIntegrationEnabled,
                                 final boolean bPEPPOLDirectoryIntegrationAutoUpdate,
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
      eChange = eChange.or (m_aSMPS.setPEPPOLDirectoryIntegrationAutoUpdate (bPEPPOLDirectoryIntegrationAutoUpdate));
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

    // Invoke callbacks
    if (eChange.isChanged ())
      m_aCallbacks.forEach (x -> x.onSMPSettingsChanged (m_aSMPS));

    return eChange;
  }
}
