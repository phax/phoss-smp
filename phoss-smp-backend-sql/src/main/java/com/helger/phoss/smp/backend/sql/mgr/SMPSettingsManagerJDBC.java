/*
 * Copyright (C) 2019-2021 Philip Helger and contributors
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.attr.IStringMap;
import com.helger.commons.collection.attr.StringMap;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.StringParser;
import com.helger.commons.wrapper.Wrapper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.settings.ISMPSettingsCallback;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.phoss.smp.settings.SMPSettings;

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

  private final CallbackList <ISMPSettingsCallback> m_aCallbacks = new CallbackList <> ();

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be
   *        <code>null</code>.
   */
  public SMPSettingsManagerJDBC (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier)
  {
    super (aDBExecSupplier);
  }

  @Nonnull
  @ReturnsMutableObject
  public final CallbackList <ISMPSettingsCallback> callbacks ()
  {
    return m_aCallbacks;
  }

  static void setSettingsValue (@Nonnull final DBExecutor aExecutor, @Nonnull @Nonempty final String sKey, @Nullable final String sValue)
  {
    ValueEnforcer.notNull (aExecutor, "Executor");
    ValueEnforcer.notEmpty (sKey, "Key");

    // update
    final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_settings SET value=? WHERE id=?",
                                                            new ConstantPreparedStatementDataProvider (getTrimmedToLength (sValue, 500),
                                                                                                       getTrimmedToLength (sKey, 45)));
    if (nUpdated == 0)
    {
      // Create
      final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_settings (id, value) VALUES (?, ?)",
                                                              new ConstantPreparedStatementDataProvider (getTrimmedToLength (sKey, 45),
                                                                                                         getTrimmedToLength (sValue, 500)));
      if (nCreated != 1)
        throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
    }
  }

  @Nonnull
  public ESuccess setSettingsValues (@Nonnull @Nonempty final Map <String, String> aEntries)
  {
    ValueEnforcer.notEmpty (aEntries, "Entries");

    final DBExecutor aExecutor = newExecutor ();
    return aExecutor.performInTransaction ( () -> {
      for (final Map.Entry <String, String> aEntry : aEntries.entrySet ())
      {
        final String sKey = aEntry.getKey ();
        final String sValue = aEntry.getValue ();

        setSettingsValue (aExecutor, sKey, sValue);
      }
    });
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsMap <String, String> getAllSettingsValues ()
  {
    final ICommonsMap <String, String> ret = new CommonsHashMap <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT id, value FROM smp_settings");
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
        ret.put (aRow.getAsString (0), aRow.getAsString (1));
    return ret;
  }

  @Nullable
  public static String getSettingsValue (@Nonnull final DBExecutor aExecutor, @Nullable final String sKey)
  {
    if (StringHelper.hasNoText (sKey))
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    aExecutor.querySingle ("SELECT value FROM smp_settings WHERE id=?", new ConstantPreparedStatementDataProvider (sKey), aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    return aDBResult.get ().getAsString (0);
  }

  @Nullable
  public String getSettingsValue (@Nullable final String sKey)
  {
    if (StringHelper.hasNoText (sKey))
      return null;

    return getSettingsValue (newExecutor (), sKey);
  }

  @Nonnull
  public ISMPSettings getSettings ()
  {
    final ICommonsMap <String, String> aValues = getAllSettingsValues ();
    final SMPSettings ret = new SMPSettings (false);
    ret.setRESTWritableAPIDisabled (StringParser.parseBool (aValues.get (SMP_REST_WRITABLE_API_DISABLED),
                                                            SMPServerConfiguration.DEFAULT_SMP_REST_WRITABLE_API_DISABLED));
    ret.setDirectoryIntegrationEnabled (StringParser.parseBool (aValues.get (DIRECTORY_INTEGRATION_ENABLED),
                                                                SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_INTEGRATION_ENABLED));
    ret.setDirectoryIntegrationRequired (StringParser.parseBool (aValues.get (DIRECTORY_INTEGRATION_REQUIRED),
                                                                 SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_INTEGRATION_REQUIRED));
    ret.setDirectoryIntegrationAutoUpdate (StringParser.parseBool (aValues.get (DIRECTORY_INTEGRATION_AUTO_UPDATE),
                                                                   SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE));
    ret.setDirectoryHostName (aValues.get (DIRECTORY_HOSTNAME));
    ret.setSMLEnabled (StringParser.parseBool (aValues.get (SML_ENABLED), SMPServerConfiguration.DEFAULT_SML_ENABLED));
    ret.setSMLRequired (StringParser.parseBool (aValues.get (SML_REQUIRED), SMPServerConfiguration.DEFAULT_SML_REQUIRED));
    ret.setSMLInfoID (aValues.get (SML_INFO_ID));
    return ret;
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
    final IStringMap aMap = new StringMap ();
    aMap.putIn (SMP_REST_WRITABLE_API_DISABLED, bRESTWritableAPIDisabled);
    aMap.putIn (DIRECTORY_INTEGRATION_ENABLED, bDirectoryIntegrationEnabled);
    aMap.putIn (DIRECTORY_INTEGRATION_REQUIRED, bDirectoryIntegrationRequired);
    aMap.putIn (DIRECTORY_INTEGRATION_AUTO_UPDATE, bDirectoryIntegrationAutoUpdate);
    aMap.putIn (DIRECTORY_HOSTNAME, sDirectoryHostName);
    aMap.putIn (SML_ENABLED, bSMLEnabled);
    aMap.putIn (SML_REQUIRED, bSMLRequired);
    aMap.putIn (SML_INFO_ID, sSMLInfoID);
    if (setSettingsValues (aMap).isFailure ())
      return EChange.UNCHANGED;
    return EChange.CHANGED;
  }
}
