/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
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

import org.bson.Document;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.settings.ISMPSettingsCallback;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.phoss.smp.settings.SMPSettings;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

  private final SMPSettings m_aSMPSettings = SMPSettings.createInitializedFromConfiguration ();
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
    ValueEnforcer.notNull (aDoc, "Doc");
    ValueEnforcer.notNull (aTarget, "Target");

    aTarget.setRESTWritableAPIDisabled (aDoc.getBoolean (BSON_SMP_REST_WRITABLE_API_DISABLED,
                                                         aTarget.isRESTWritableAPIDisabled ()));
    aTarget.setDirectoryIntegrationEnabled (aDoc.getBoolean (BSON_DIRECTORY_INTEGRATION_ENABLED,
                                                             aTarget.isDirectoryIntegrationEnabled ()));
    aTarget.setDirectoryIntegrationRequired (aDoc.getBoolean (BSON_DIRECTORY_INTEGRATION_REQUIRED,
                                                              aTarget.isDirectoryIntegrationRequired ()));
    aTarget.setDirectoryIntegrationAutoUpdate (aDoc.getBoolean (BSON_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                                                aTarget.isDirectoryIntegrationAutoUpdate ()));

    String sDirectoryHostName = aDoc.getString (BSON_DIRECTORY_HOSTNAME);
    if (StringHelper.isEmpty (sDirectoryHostName))
      sDirectoryHostName = aTarget.getDirectoryHostName ();
    aTarget.setDirectoryHostName (sDirectoryHostName);

    aTarget.setSMLEnabled (aDoc.getBoolean (BSON_SML_ENABLED, aTarget.isSMLEnabled ()));
    aTarget.setSMLRequired (aDoc.getBoolean (BSON_SML_REQUIRED, aTarget.isSMLRequired ()));
    aTarget.setSMLInfoID (aDoc.getString (BSON_SML_INFO_ID));
  }

  public SMPSettingsManagerMongoDB ()
  {
    super ("smp-settings");
    final Document aDoc = getCollection ().find (new Document (BSON_ID, ID_SETTINGS)).first ();
    if (aDoc != null)
      toDomain (aDoc, m_aSMPSettings);
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
    return m_aSMPSettings;
  }

  @Nonnull
  public EChange updateSettings (final boolean bRESTWritableAPIDisabled,
                                 final boolean bDirectoryIntegrationEnabled,
                                 final boolean bDirectoryIntegrationRequired,
                                 final boolean bDirectoryIntegrationAutoUpdate,
                                 @Nullable final String sDirectoryHostName,
                                 final boolean bSMLEnabled,
                                 final boolean bSMLRequired,
                                 @Nullable final String sSMLInfoID)
  {
    EChange eChange = EChange.UNCHANGED;
    eChange = eChange.or (m_aSMPSettings.setRESTWritableAPIDisabled (bRESTWritableAPIDisabled));
    eChange = eChange.or (m_aSMPSettings.setDirectoryIntegrationEnabled (bDirectoryIntegrationEnabled));
    eChange = eChange.or (m_aSMPSettings.setDirectoryIntegrationRequired (bDirectoryIntegrationRequired));
    eChange = eChange.or (m_aSMPSettings.setDirectoryIntegrationAutoUpdate (bDirectoryIntegrationAutoUpdate));
    eChange = eChange.or (m_aSMPSettings.setDirectoryHostName (sDirectoryHostName));
    eChange = eChange.or (m_aSMPSettings.setSMLEnabled (bSMLEnabled));
    eChange = eChange.or (m_aSMPSettings.setSMLRequired (bSMLRequired));
    eChange = eChange.or (m_aSMPSettings.setSMLInfoID (sSMLInfoID));
    if (eChange.isChanged ())
    {
      // Write to DB
      final Document aDoc = toBson (m_aSMPSettings);
      if (m_aInsertDocument.getAndSet (false))
      {
        if (!getCollection ().insertOne (aDoc).wasAcknowledged ())
          throw new IllegalStateException ("Failed to insert into MongoDB Collection");
      }
      else
      {
        if (!getCollection ().replaceOne (new Document (BSON_ID, ID_SETTINGS), aDoc).wasAcknowledged ())
          throw new IllegalStateException ("Failed to replace in MongoDB Collection");
      }

      // Invoke callbacks
      m_aCallbacks.forEach (x -> x.onSMPSettingsChanged (m_aSMPSettings));
    }

    return eChange;
  }
}
