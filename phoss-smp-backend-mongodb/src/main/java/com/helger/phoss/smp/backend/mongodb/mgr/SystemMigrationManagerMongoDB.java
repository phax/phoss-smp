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
package com.helger.phoss.smp.backend.mongodb.mgr;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.function.Supplier;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.SuccessWithValue;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.mgrs.sysmigration.ISystemMigrationManager;
import com.helger.photon.mgrs.sysmigration.SystemMigrationHelper;
import com.helger.photon.mgrs.sysmigration.SystemMigrationResult;
import com.helger.typeconvert.impl.TypeConverter;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;

/**
 * MongoDB based implementation of {@link ISystemMigrationManager}.
 *
 * @author Philip Helger
 * @since 8.0.16
 */
public class SystemMigrationManagerMongoDB extends AbstractManagerMongoDB implements ISystemMigrationManager
{
  private static final String BSON_MIGRATION_ID = "migration_id";
  private static final String BSON_EXECUTION_DT = "execution_dt";
  private static final String BSON_SUCCESS = "success";
  private static final String BSON_ERROR_MSG = "error_msg";

  public SystemMigrationManagerMongoDB ()
  {
    super ("smp-sys-migration");
    getCollection ().createIndex (Indexes.ascending (BSON_MIGRATION_ID));
  }

  @NonNull
  private static Document _toBson (@NonNull final SystemMigrationResult aResult)
  {
    return new Document ().append (BSON_MIGRATION_ID, aResult.getID ())
                          .append (BSON_EXECUTION_DT,
                                   TypeConverter.convert (aResult.getExecutionDateTime (), Date.class))
                          .append (BSON_SUCCESS, Boolean.valueOf (aResult.isSuccess ()))
                          .append (BSON_ERROR_MSG, aResult.getErrorMessage ());
  }

  @NonNull
  private static SystemMigrationResult _toDomain (@NonNull final Document aDoc)
  {
    final String sMigrationID = aDoc.getString (BSON_MIGRATION_ID);
    final Date aDate = aDoc.getDate (BSON_EXECUTION_DT);
    final LocalDateTime aExecutionDT = aDate != null ? TypeConverter.convert (aDate, LocalDateTime.class)
                                                     : LocalDateTime.now (ZoneOffset.UTC);
    final boolean bSuccess = aDoc.getBoolean (BSON_SUCCESS, false);
    final String sErrorMsg = aDoc.getString (BSON_ERROR_MSG);
    return new SystemMigrationResult (sMigrationID, aExecutionDT, bSuccess, sErrorMsg);
  }

  public void addMigrationResult (@NonNull final SystemMigrationResult aMigrationResult)
  {
    if (!getCollection ().insertOne (_toBson (aMigrationResult)).wasAcknowledged ())
      throw new IllegalStateException ("Failed to insert migration result for '" + aMigrationResult.getID () + "'");

    AuditHelper.onAuditCreateSuccess (SystemMigrationResult.OT,
                                      aMigrationResult.getID (),
                                      Boolean.valueOf (aMigrationResult.isSuccess ()),
                                      aMigrationResult.getErrorMessage ());
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <SystemMigrationResult> getAllMigrationResults (@Nullable final String sMigrationID)
  {
    final ICommonsList <SystemMigrationResult> ret = new CommonsArrayList <> ();
    if (sMigrationID == null)
      return ret;
    for (final Document aDoc : getCollection ().find (Filters.eq (BSON_MIGRATION_ID, sMigrationID)))
      ret.add (_toDomain (aDoc));
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <SystemMigrationResult> getAllMigrationResultsFlattened ()
  {
    final ICommonsList <SystemMigrationResult> ret = new CommonsArrayList <> ();
    for (final Document aDoc : getCollection ().find ())
      ret.add (_toDomain (aDoc));
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <SystemMigrationResult> getAllFailedMigrationResults (@Nullable final String sMigrationID)
  {
    final ICommonsList <SystemMigrationResult> ret = new CommonsArrayList <> ();
    if (sMigrationID == null)
      return ret;
    for (final Document aDoc : getCollection ().find (Filters.and (Filters.eq (BSON_MIGRATION_ID, sMigrationID),
                                                                   Filters.eq (BSON_SUCCESS, Boolean.FALSE))))
      ret.add (_toDomain (aDoc));
    return ret;
  }

  public boolean wasMigrationExecutedSuccessfully (@Nullable final String sMigrationID)
  {
    if (sMigrationID == null)
      return false;
    return getCollection ().countDocuments (Filters.and (Filters.eq (BSON_MIGRATION_ID, sMigrationID),
                                                         Filters.eq (BSON_SUCCESS, Boolean.TRUE))) > 0;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllMigrationIDs ()
  {
    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    for (final String sID : getCollection ().distinct (BSON_MIGRATION_ID, String.class))
      ret.add (sID);
    return ret;
  }

  public void performMigrationIfNecessary (@NonNull @Nonempty final String sMigrationID,
                                           @NonNull final Runnable aMigrationAction)
  {
    SystemMigrationHelper.performMigrationIfNecessary (this, sMigrationID, aMigrationAction);
  }

  public void performMigrationIfNecessary (@NonNull @Nonempty final String sMigrationID,
                                           @NonNull final Supplier <SuccessWithValue <String>> aMigrationAction)
  {
    SystemMigrationHelper.performMigrationIfNecessary (this, sMigrationID, aMigrationAction);
  }
}
