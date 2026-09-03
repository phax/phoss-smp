/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.pmigration;

import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.compare.ESortOrder;
import com.helger.base.lang.EnumHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.domain.ISMPTableColumn;

/**
 * The sortable and searchable columns of an {@link ISMPParticipantMigration}.<br>
 * Note: no MongoDB field names are provided, because the MongoDB backend stores the participant
 * identifier as a sub document and not as a single string, so a native filter would not behave
 * identical to the other backends. The MongoDB backend therefore uses the in-memory implementation
 * of {@link com.helger.phoss.smp.domain.SMPTableColumnHelper}, which is unproblematic because the
 * number of participant migrations is bound by the number of migrations ever performed.
 *
 * @author Philip Helger
 * @since 8.2.1
 */
public enum ESMPParticipantMigrationColumn implements ISMPTableColumn <ISMPParticipantMigration>
{
  /** The participant identifier being migrated */
  PARTICIPANT_ID ("participantid",
                  new String [] { "pid" },
                  true,
                  true,
                  ESortOrder.ASCENDING,
                  x -> x.getParticipantIdentifier ().getURIEncoded ()),
  /** The date and time the migration was initiated */
  INITIATION_DATE_TIME ("initiationdt",
                        new String [] { "initdt" },
                        true,
                        false,
                        null,
                        x -> x.getInitiationDateTime () == null ? null : x.getInitiationDateTime ().toString ()),
  /** The migration key exchanged with the SML */
  MIGRATION_KEY ("migrationkey", new String [] { "migkey" }, true, true,
                  null, ISMPParticipantMigration::getMigrationKey);

  private final String m_sID;
  private final String [] m_aSQLColumnNames;
  private final boolean m_bSortable;
  private final boolean m_bSearchable;
  private final ESortOrder m_eDefaultSortOrder;
  private final Function <ISMPParticipantMigration, String> m_aValueProvider;

  ESMPParticipantMigrationColumn (@NonNull @Nonempty final String sID,
                                  final String @Nullable [] aSQLColumnNames,
                                  final boolean bSortable,
                                  final boolean bSearchable,
                                  @Nullable final ESortOrder eDefaultSortOrder,
                                  @NonNull final Function <ISMPParticipantMigration, String> aValueProvider)
  {
    m_sID = sID;
    m_aSQLColumnNames = aSQLColumnNames;
    m_bSortable = bSortable;
    m_bSearchable = bSearchable;
    m_eDefaultSortOrder = eDefaultSortOrder;
    m_aValueProvider = aValueProvider;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  @ReturnsMutableCopy
  public ICommonsList <String> getAllSQLColumnNames ()
  {
    return m_aSQLColumnNames == null ? null : new CommonsArrayList <> (m_aSQLColumnNames);
  }

  @Nullable
  @ReturnsMutableCopy
  public ICommonsList <String> getAllMongoFieldNames ()
  {
    // See the class comment - the MongoDB backend pages in memory
    return null;
  }

  public boolean isSortable ()
  {
    return m_bSortable;
  }

  public boolean isSearchable ()
  {
    return m_bSearchable;
  }

  @Nullable
  public ESortOrder getDefaultSortOrder ()
  {
    return m_eDefaultSortOrder;
  }

  @NonNull
  public Function <ISMPParticipantMigration, String> getValueProvider ()
  {
    return m_aValueProvider;
  }

  @Nullable
  public static ESMPParticipantMigrationColumn getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESMPParticipantMigrationColumn.class, sID);
  }
}
