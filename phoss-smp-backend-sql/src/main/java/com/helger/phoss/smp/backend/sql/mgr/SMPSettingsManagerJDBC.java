/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.sql.mgr;

import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringParser;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.backend.sql.SMPDBExecutor;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.settings.ISMPSettingsCallback;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.phoss.smp.settings.SMPSettings;
import com.helger.typeconvert.collection.IStringMap;
import com.helger.typeconvert.collection.StringMap;
import com.helger.web.scope.singleton.AbstractRequestWebSingleton;

public class SMPSettingsManagerJDBC extends AbstractJDBCEnabledManager implements ISMPSettingsManager
{
  private static final String SMP_REST_WRITABLE_API_DISABLED = "smp-rest-writable-api-disabled";
  private static final String DIRECTORY_INTEGRATION_REQUIRED = "directory-required";
  private static final String DIRECTORY_INTEGRATION_ENABLED = "directory-enabled";
  private static final String DIRECTORY_INTEGRATION_AUTO_UPDATE = "directory-auto-update";
  private static final String DIRECTORY_HOSTNAME = "directory-hostname";
  private static final String SML_REQUIRED = "sml-required";
  private static final String SML_ENABLED = "sml-enabled";
  private static final String SML_INFO_ID = "smlinfo-id";

  private final String m_sTableName;
  private final CallbackList <ISMPSettingsCallback> m_aCallbacks = new CallbackList <> ();

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be <code>null</code>.
   * @param sTableNamePrefix
   *        The table name prefix to be used. May not be <code>null</code>.
   */
  public SMPSettingsManagerJDBC (@NonNull final Supplier <? extends DBExecutor> aDBExecSupplier,
                                 @NonNull final String sTableNamePrefix)
  {
    super (aDBExecSupplier);
    ValueEnforcer.notNull (sTableNamePrefix, "TableNamePrefix");
    m_sTableName = sTableNamePrefix + "smp_settings";
  }

  @NonNull
  @ReturnsMutableObject
  public final CallbackList <ISMPSettingsCallback> callbacks ()
  {
    return m_aCallbacks;
  }

  public static void setSettingsValueInDB (@NonNull final DBExecutor aExecutor,
                                           @NonNull @Nonempty final String sKey,
                                           @Nullable final String sValue)
  {
    ValueEnforcer.notNull (aExecutor, "Executor");
    ValueEnforcer.notEmpty (sKey, "Key");

    // update
    final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE " +
                                                            SMPDBExecutor.TABLE_NAME_PREFIX +
                                                            "smp_settings SET value=? WHERE id=?",
                                                            new ConstantPreparedStatementDataProvider (DBValueHelper.getTrimmedToLength (sValue,
                                                                                                                                         ISMPSettingsManager.MAX_LEN_VALUE),
                                                                                                       DBValueHelper.getTrimmedToLength (sKey,
                                                                                                                                         CSMPServer.MAX_LEN_ID)));
    if (nUpdated == 0)
    {
      // Create
      final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                                              SMPDBExecutor.TABLE_NAME_PREFIX +
                                                              "smp_settings (id, value) VALUES (?, ?)",
                                                              new ConstantPreparedStatementDataProvider (DBValueHelper.getTrimmedToLength (sKey,
                                                                                                                                           CSMPServer.MAX_LEN_ID),
                                                                                                         DBValueHelper.getTrimmedToLength (sValue,
                                                                                                                                           ISMPSettingsManager.MAX_LEN_VALUE)));
      if (nCreated != 1)
        throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
    }
  }

  @NonNull
  public ESuccess setSettingsValuesInDB (@NonNull @Nonempty final Map <String, String> aEntries)
  {
    ValueEnforcer.notEmpty (aEntries, "Entries");

    final DBExecutor aExecutor = newExecutor ();
    return aExecutor.performInTransaction ( () -> {
      for (final Map.Entry <String, String> aEntry : aEntries.entrySet ())
      {
        final String sKey = aEntry.getKey ();
        final String sValue = aEntry.getValue ();

        setSettingsValueInDB (aExecutor, sKey, sValue);
      }
    });
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsMap <String, String> getAllSettingsValuesFromDB ()
  {
    final ICommonsMap <String, String> ret = new CommonsHashMap <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT id, value FROM " + m_sTableName);
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
        ret.put (aRow.getAsString (0), aRow.getAsString (1));
    return ret;
  }

  @Nullable
  public static String getSettingsValueFromDB (@NonNull final DBExecutor aExecutor, @Nullable final String sKey)
  {
    if (StringHelper.isEmpty (sKey))
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    aExecutor.querySingle ("SELECT value FROM " + SMPDBExecutor.TABLE_NAME_PREFIX + "smp_settings WHERE id=?",
                           new ConstantPreparedStatementDataProvider (sKey),
                           aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    return aDBResult.get ().getAsString (0);
  }

  @Nullable
  public String getSettingsValue (@Nullable final String sKey)
  {
    if (StringHelper.isEmpty (sKey))
      return null;

    return getSettingsValueFromDB (newExecutor (), sKey);
  }

  public static class SettingsSingleton extends AbstractRequestWebSingleton
  {
    private static final Logger LOGGER = LoggerFactory.getLogger (SMPSettingsManagerJDBC.SettingsSingleton.class);
    private ISMPSettings m_aSMPSettings;

    @Deprecated (forRemoval = false)
    @UsedViaReflection
    public SettingsSingleton ()
    {}

    @NonNull
    public static SettingsSingleton getInstance ()
    {
      return getRequestSingleton (SettingsSingleton.class);
    }

    @NonNull
    private static ISMPSettings _createSettingsFromDB (@NonNull final SMPSettingsManagerJDBC aMgr)
    {
      // Queries DB
      final ICommonsMap <String, String> aValues = aMgr.getAllSettingsValuesFromDB ();

      final SMPSettings ret = SMPSettings.createInitializedFromConfiguration ();
      ret.setRESTWritableAPIDisabled (StringParser.parseBool (aValues.get (SMP_REST_WRITABLE_API_DISABLED),
                                                              ret.isRESTWritableAPIDisabled ()));
      ret.setDirectoryIntegrationEnabled (StringParser.parseBool (aValues.get (DIRECTORY_INTEGRATION_ENABLED),
                                                                  ret.isDirectoryIntegrationEnabled ()));
      ret.setDirectoryIntegrationRequired (StringParser.parseBool (aValues.get (DIRECTORY_INTEGRATION_REQUIRED),
                                                                   ret.isDirectoryIntegrationRequired ()));
      ret.setDirectoryIntegrationAutoUpdate (StringParser.parseBool (aValues.get (DIRECTORY_INTEGRATION_AUTO_UPDATE),
                                                                     ret.isDirectoryIntegrationAutoUpdate ()));
      String sDirectoryHostName = aValues.get (DIRECTORY_HOSTNAME);
      if (StringHelper.isEmpty (sDirectoryHostName))
        sDirectoryHostName = ret.getDirectoryHostName ();
      ret.setDirectoryHostName (sDirectoryHostName);
      ret.setSMLEnabled (StringParser.parseBool (aValues.get (SML_ENABLED), ret.isSMLEnabled ()));
      ret.setSMLRequired (StringParser.parseBool (aValues.get (SML_REQUIRED), ret.isSMLRequired ()));
      ret.setSMLInfoID (aValues.get (SML_INFO_ID));
      return ret;
    }

    @NonNull
    public ISMPSettings getSettings (@NonNull final SMPSettingsManagerJDBC aMgr)
    {
      ISMPSettings ret = m_aSMPSettings;
      if (ret == null)
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Loading SMP settings from DB");
        ret = m_aSMPSettings = _createSettingsFromDB (aMgr);
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Reusing SMP settings of request");
      }
      return ret;
    }
  }

  @NonNull
  public ISMPSettings getSettings ()
  {
    // Construct to read the settings only once per request
    return SettingsSingleton.getInstance ().getSettings (this);
  }

  @NonNull
  public EChange updateSettings (final boolean bRESTWritableAPIDisabled,
                                 final boolean bDirectoryIntegrationEnabled,
                                 final boolean bDirectoryIntegrationRequired,
                                 final boolean bDirectoryIntegrationAutoUpdate,
                                 @Nullable final String sDirectoryHostName,
                                 final boolean bSMLEnabled,
                                 final boolean bSMLRequired,
                                 @Nullable final String sSMLInfoID)
  {
    final IStringMap aMap = new StringMap ();
    aMap.putIn (SMP_REST_WRITABLE_API_DISABLED, bRESTWritableAPIDisabled);
    aMap.putIn (DIRECTORY_INTEGRATION_ENABLED, bDirectoryIntegrationEnabled);
    aMap.putIn (DIRECTORY_INTEGRATION_REQUIRED, bDirectoryIntegrationRequired);
    aMap.putIn (DIRECTORY_INTEGRATION_AUTO_UPDATE, bDirectoryIntegrationAutoUpdate);
    aMap.putIn (DIRECTORY_HOSTNAME, sDirectoryHostName);
    aMap.putIn (SML_ENABLED, bSMLEnabled);
    aMap.putIn (SML_REQUIRED, bSMLRequired);
    aMap.putIn (SML_INFO_ID, sSMLInfoID);

    // Save
    if (setSettingsValuesInDB (aMap).isFailure ())
      return EChange.UNCHANGED;
    return EChange.CHANGED;
  }
}
