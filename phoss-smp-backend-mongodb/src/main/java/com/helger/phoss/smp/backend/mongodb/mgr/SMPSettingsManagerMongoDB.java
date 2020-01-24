/**
 * Copyright (C) 2019-2020 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.mgr;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.Document;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.state.EChange;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.settings.ISMPSettingsCallback;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.phoss.smp.settings.SMPSettings;

/**
 * Implementation of {@link ISMPSettingsManager} for MongoDB
 *
 * @author Philip Helger
 */
public class SMPSettingsManagerMongoDB extends AbstractManagerMongoDB implements ISMPSettingsManager
{
  private static final String BSON_ID = "id";
  // Legacy ID
  private static final String ID_SETTINGS = "singleton";
  private static final String BSON_SMP_REST_WRITABLE_API_DISABLED = "smp-rest-writable-api-disabled";
  private static final String BSON_DIRECTORY_INTEGRATION_REQUIRED = "directory-required";
  private static final String BSON_DIRECTORY_INTEGRATION_ENABLED = "directory-enabled";
  private static final String BSON_DIRECTORY_INTEGRATION_AUTO_UPDATE = "directory-auto-update";
  private static final String BSON_DIRECTORY_HOSTNAME = "directory-hostname";
  private static final String BSON_SML_REQUIRED = "sml-required";
  private static final String BSON_SML_ENABLED = "sml-enabled";
  private static final String BSON_SML_INFO_ID = "smlinfo-id";

  private final SMPSettings m_aSettings = new SMPSettings ();
  private final CallbackList <ISMPSettingsCallback> m_aCallbacks = new CallbackList <> ();
  private final AtomicBoolean m_aInsertDocument = new AtomicBoolean ();

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final ISMPSettings aValue)
  {
    return new Document ().append (BSON_ID, ID_SETTINGS)
                          .append (BSON_SMP_REST_WRITABLE_API_DISABLED,
                                   Boolean.valueOf (aValue.isRESTWritableAPIDisabled ()))
                          .append (BSON_DIRECTORY_INTEGRATION_REQUIRED,
                                   Boolean.valueOf (aValue.isDirectoryIntegrationRequired ()))
                          .append (BSON_DIRECTORY_INTEGRATION_ENABLED,
                                   Boolean.valueOf (aValue.isDirectoryIntegrationEnabled ()))
                          .append (BSON_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                   Boolean.valueOf (aValue.isDirectoryIntegrationAutoUpdate ()))
                          .append (BSON_DIRECTORY_HOSTNAME, aValue.getDirectoryHostName ())
                          .append (BSON_SML_REQUIRED, Boolean.valueOf (aValue.isSMLRequired ()))
                          .append (BSON_SML_ENABLED, Boolean.valueOf (aValue.isSMLEnabled ()))
                          .append (BSON_SML_INFO_ID, aValue.getSMLInfoID ());
  }

  public static void toDomain (@Nonnull final Document aDoc, @Nonnull final SMPSettings aTarget)
  {
    aTarget.setRESTWritableAPIDisabled (aDoc.getBoolean (BSON_SMP_REST_WRITABLE_API_DISABLED,
                                                         SMPServerConfiguration.DEFAULT_SMP_REST_WRITABLE_API_DISABLED));
    aTarget.setDirectoryIntegrationRequired (aDoc.getBoolean (BSON_DIRECTORY_INTEGRATION_REQUIRED,
                                                              SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_INTEGRATION_REQUIRED));
    aTarget.setDirectoryIntegrationEnabled (aDoc.getBoolean (BSON_DIRECTORY_INTEGRATION_ENABLED,
                                                             SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_INTEGRATION_ENABLED));
    aTarget.setDirectoryIntegrationAutoUpdate (aDoc.getBoolean (BSON_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                                                SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE));
    aTarget.setDirectoryHostName (aDoc.getString (BSON_DIRECTORY_HOSTNAME));
    aTarget.setSMLRequired (aDoc.getBoolean (BSON_SML_REQUIRED, SMPServerConfiguration.DEFAULT_SML_REQUIRED));
    aTarget.setSMLEnabled (aDoc.getBoolean (BSON_SML_ENABLED, SMPServerConfiguration.DEFAULT_SML_ENABLED));
    aTarget.setSMLInfoID (aDoc.getString (BSON_SML_INFO_ID));
  }

  public SMPSettingsManagerMongoDB ()
  {
    super ("smp-settings");
    final Document aDoc = getCollection ().find (new Document (BSON_ID, ID_SETTINGS)).first ();
    if (aDoc != null)
      toDomain (aDoc, m_aSettings);
    m_aInsertDocument.set (aDoc == null);
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
                                 @Nullable final ISMLInfo aSMLInfo)
  {
    EChange eChange = EChange.UNCHANGED;
    eChange = eChange.or (m_aSettings.setRESTWritableAPIDisabled (bRESTWritableAPIDisabled));
    eChange = eChange.or (m_aSettings.setDirectoryIntegrationEnabled (bDirectoryIntegrationEnabled));
    eChange = eChange.or (m_aSettings.setDirectoryIntegrationRequired (bDirectoryIntegrationRequired));
    eChange = eChange.or (m_aSettings.setDirectoryIntegrationAutoUpdate (bDirectoryIntegrationAutoUpdate));
    eChange = eChange.or (m_aSettings.setDirectoryHostName (sDirectoryHostName));
    eChange = eChange.or (m_aSettings.setSMLEnabled (bSMLActive));
    eChange = eChange.or (m_aSettings.setSMLRequired (bSMLRequired));
    eChange = eChange.or (m_aSettings.setSMLInfoID (aSMLInfo == null ? null : aSMLInfo.getID ()));
    if (eChange.isChanged ())
    {
      final Document aDoc = toBson (m_aSettings);
      if (m_aInsertDocument.getAndSet (false))
      {
        getCollection ().insertOne (aDoc);
      }
      else
      {
        getCollection ().replaceOne (new Document (BSON_ID, ID_SETTINGS), aDoc);
      }

      // Invoke callbacks
      m_aCallbacks.forEach (x -> x.onSMPSettingsChanged (m_aSettings));
    }

    return eChange;
  }
}
