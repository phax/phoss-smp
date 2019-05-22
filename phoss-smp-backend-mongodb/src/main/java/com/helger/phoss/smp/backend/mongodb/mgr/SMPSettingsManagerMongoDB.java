package com.helger.phoss.smp.backend.mongodb.mgr;

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

public class SMPSettingsManagerMongoDB extends AbstractManagerMongoDB implements ISMPSettingsManager
{
  private static final String BSON_ID = "id";
  private static final String ID_DUMMY = "singleton";
  private static final String KEY_SML_INFO_ID = "smlinfo.id";

  private final SMPSettings m_aSettings = new SMPSettings ();
  private final CallbackList <ISMPSettingsCallback> m_aCallbacks = new CallbackList <> ();

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final ISMPSettings aSettings)
  {
    return new Document ().append (BSON_ID, ID_DUMMY)
                          .append (SMPServerConfiguration.KEY_SMP_REST_WRITABLE_API_DISABLED,
                                   Boolean.valueOf (aSettings.isRESTWritableAPIDisabled ()))
                          .append (SMPServerConfiguration.KEY_SMP_DIRECTORY_INTEGRATION_REQUIRED,
                                   Boolean.valueOf (aSettings.isDirectoryIntegrationRequired ()))
                          .append (SMPServerConfiguration.KEY_SMP_DIRECTORY_INTEGRATION_ENABLED,
                                   Boolean.valueOf (aSettings.isDirectoryIntegrationEnabled ()))
                          .append (SMPServerConfiguration.KEY_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                   Boolean.valueOf (aSettings.isDirectoryIntegrationAutoUpdate ()))
                          .append (SMPServerConfiguration.KEY_SMP_DIRECTORY_HOSTNAME, aSettings.getDirectoryHostName ())
                          .append (SMPServerConfiguration.KEY_SML_REQUIRED,
                                   Boolean.valueOf (aSettings.isSMLRequired ()))
                          .append (SMPServerConfiguration.KEY_SML_ENABLED, Boolean.valueOf (aSettings.isSMLEnabled ()))
                          .append (KEY_SML_INFO_ID, aSettings.getSMLInfoID ());
  }

  public static void toDomain (@Nonnull final Document aDoc, @Nonnull final SMPSettings aTarget)
  {
    aTarget.setRESTWritableAPIDisabled (aDoc.getBoolean (SMPServerConfiguration.KEY_SMP_REST_WRITABLE_API_DISABLED,
                                                         SMPServerConfiguration.DEFAULT_SMP_REST_WRITABLE_API_DISABLED));
    aTarget.setDirectoryIntegrationRequired (aDoc.getBoolean (SMPServerConfiguration.KEY_SMP_DIRECTORY_INTEGRATION_REQUIRED,
                                                              SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_INTEGRATION_REQUIRED));
    aTarget.setDirectoryIntegrationEnabled (aDoc.getBoolean (SMPServerConfiguration.KEY_SMP_DIRECTORY_INTEGRATION_ENABLED,
                                                             SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_INTEGRATION_ENABLED));
    aTarget.setDirectoryIntegrationAutoUpdate (aDoc.getBoolean (SMPServerConfiguration.KEY_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                                                SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE));
    aTarget.setDirectoryHostName (aDoc.getString (SMPServerConfiguration.KEY_SMP_DIRECTORY_HOSTNAME));
    aTarget.setSMLRequired (aDoc.getBoolean (SMPServerConfiguration.KEY_SML_REQUIRED,
                                             SMPServerConfiguration.DEFAULT_SML_REQUIRED));
    aTarget.setSMLEnabled (aDoc.getBoolean (SMPServerConfiguration.KEY_SML_ENABLED,
                                            SMPServerConfiguration.DEFAULT_SML_ENABLED));
    aTarget.setSMLInfoID (aDoc.getString (KEY_SML_INFO_ID));
  }

  public SMPSettingsManagerMongoDB ()
  {
    super ("smp-settings");
    final Document aDoc = getCollection ().find (new Document (BSON_ID, ID_DUMMY)).first ();
    if (aDoc != null)
      toDomain (aDoc, m_aSettings);
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
      getCollection ().replaceOne (new Document (BSON_ID, ID_DUMMY), aDoc);

      // Invoke callbacks
      m_aCallbacks.forEach (x -> x.onSMPSettingsChanged (m_aSettings));
    }

    return eChange;
  }
}
