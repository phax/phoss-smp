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
package com.helger.phoss.smp.domain.businesscard;

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
import com.helger.base.string.StringHelper;

/**
 * The sortable and searchable columns of an {@link ISMPBusinessCard}.
 *
 * @author Philip Helger
 * @since 8.2.1
 */
public enum ESMPBusinessCardColumn implements ISMPTableColumn <ISMPBusinessCard>
{
  /** The participant identifier of the owning service group */
  SERVICE_GROUP ("servicegroup",
                 new String [] { "pid" },
                 new String [] { "sgid" },
                 true,
                 true,
                 ESortOrder.ASCENDING,
                 ISMPBusinessCard::getID),
  /**
   * The names of all contained business entities. A Business Card has 0-n entities with 0-n names
   * each, so this column can be searched but not sorted by.
   */
  NAME ("name",
        new String [] { "name" },
        new String [] { "entities.names.name" },
        false,
        true,
        null,
        ESMPBusinessCardColumn::_getAllEntityNames);

  private final String m_sID;
  private final String [] m_aSQLColumnNames;
  private final String [] m_aMongoFieldNames;
  private final boolean m_bSortable;
  private final boolean m_bSearchable;
  private final ESortOrder m_eDefaultSortOrder;
  private final Function <ISMPBusinessCard, String> m_aValueProvider;

  ESMPBusinessCardColumn (@NonNull @Nonempty final String sID,
                         final String @Nullable [] aSQLColumnNames,
                         final String @Nullable [] aMongoFieldNames,
                         final boolean bSortable,
                         final boolean bSearchable,
                         @Nullable final ESortOrder eDefaultSortOrder,
                         @NonNull final Function <ISMPBusinessCard, String> aValueProvider)
  {
    m_sID = sID;
    m_aSQLColumnNames = aSQLColumnNames;
    m_aMongoFieldNames = aMongoFieldNames;
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
    return m_aMongoFieldNames == null ? null : new CommonsArrayList <> (m_aMongoFieldNames);
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
  public Function <ISMPBusinessCard, String> getValueProvider ()
  {
    return m_aValueProvider;
  }

  /**
   * Get all names of all entities of the provided Business Card as one string. All of them are
   * searched in the SQL and the MongoDB backend as well, so the in-memory search must cover them
   * all too.
   *
   * @param aBusinessCard
   *        The Business Card to get the names of. May not be <code>null</code>.
   * @return Never <code>null</code> but maybe empty.
   */
  @NonNull
  private static String _getAllEntityNames (@NonNull final ISMPBusinessCard aBusinessCard)
  {
    final StringBuilder aSB = new StringBuilder ();
    for (final SMPBusinessCardEntity aEntity : aBusinessCard.getAllEntities ())
      for (final SMPBusinessCardName aName : aEntity.names ())
        if (StringHelper.isNotEmpty (aName.getName ()))
        {
          if (aSB.length () > 0)
            aSB.append (' ');
          aSB.append (aName.getName ());
        }
    return aSB.toString ();
  }


  @Nullable
  public static ESMPBusinessCardColumn getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESMPBusinessCardColumn.class, sID);
  }
}
