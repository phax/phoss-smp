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
package com.helger.phoss.smp.domain.servicegroup;

import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;

/**
 * Defines the predefined server side filters that can be applied when querying Service Groups. The
 * filters are resolved by the backend, so that no more Service Groups than necessary need to be
 * read.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
public enum ESMPServiceGroupFilter implements IHasID <String>
{
  /** No filtering at all - all Service Groups match. */
  ALL ("all"),
  /** Only Service Groups that have no Business Card assigned. */
  NO_BUSINESS_CARD ("nobc"),
  /**
   * Only Service Groups that have no outbound Participant Migration that prevents a new migration.
   */
  NO_BLOCKING_MIGRATION ("nomig");

  private final String m_sID;

  ESMPServiceGroupFilter (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * @return <code>true</code> if this filter matches all Service Groups.
   */
  public boolean isAll ()
  {
    return this == ALL;
  }

  /**
   * Get the in-memory representation of this filter. This is only meant to be used by backends that
   * cannot express the filter as part of their query.
   *
   * @return Never <code>null</code>.
   */
  @NonNull
  public Predicate <ISMPServiceGroup> getFilterPredicate ()
  {
    switch (this)
    {
      case NO_BUSINESS_CARD:
      {
        final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
        if (aBusinessCardMgr == null)
        {
          // Business Cards are disabled - so none exists
          return x -> true;
        }
        return x -> !aBusinessCardMgr.containsSMPBusinessCardOfID (x.getParticipantIdentifier ());
      }
      case NO_BLOCKING_MIGRATION:
      {
        final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
        if (aParticipantMigrationMgr == null)
          return x -> true;

        // Determine all participants for which no new migration can be started, once per query
        final ICommonsSet <String> aBlockedPIDs = new CommonsHashSet <> ();
        for (final ISMPParticipantMigration aMigration : aParticipantMigrationMgr.getAllOutboundParticipantMigrations (null))
          if (aMigration.getState ().preventsNewMigration ())
            aBlockedPIDs.add (aMigration.getParticipantIdentifier ().getURIEncoded ());
        return x -> !aBlockedPIDs.contains (x.getParticipantIdentifier ().getURIEncoded ());
      }
      default:
        return x -> true;
    }
  }

  @Nullable
  public static ESMPServiceGroupFilter getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESMPServiceGroupFilter.class, sID);
  }

  @Nullable
  public static ESMPServiceGroupFilter getFromIDOrDefault (@Nullable final String sID,
                                                           @Nullable final ESMPServiceGroupFilter eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ESMPServiceGroupFilter.class, sID, eDefault);
  }
}
