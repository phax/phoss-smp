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

import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.base.type.ObjectType;
import com.helger.cache.regex.RegExHelper;
import com.helger.datetime.helper.PDTFactory;
import com.helger.peppol.sml.CSMLDefault;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Default implementation of {@link ISMPParticipantMigration}
 *
 * @author Philip Helger
 * @since 5.4.0
 */
@NotThreadSafe
public class SMPParticipantMigration implements ISMPParticipantMigration
{
  public static final ObjectType OT = new ObjectType ("SmpParticipantMigration");
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPParticipantMigration.class);

  private final String m_sID;
  private final EParticipantMigrationDirection m_eDirection;
  private EParticipantMigrationState m_eState;
  private final IParticipantIdentifier m_aParticipantID;
  private final LocalDateTime m_aInitiationDateTime;
  private final String m_sMigrationKey;

  /**
   * Check if the provided migration key matches the SML requirements.
   *
   * @param sMigrationKey
   *        The migration key to check. May be <code>null</code>.
   * @return <code>true</code> if the key is valid, <code>false</code> if not.
   */
  public static boolean isValidMigrationKey (@Nullable final String sMigrationKey)
  {
    return StringHelper.isNotEmpty (sMigrationKey) &&
           RegExHelper.stringMatchesPattern (CSMLDefault.MIGRATION_CODE_PATTERN, sMigrationKey);
  }

  public SMPParticipantMigration (@NonNull @Nonempty final String sID,
                                  @NonNull final EParticipantMigrationDirection eDirection,
                                  @NonNull final EParticipantMigrationState eState,
                                  @NonNull final IParticipantIdentifier aParticipantID,
                                  @NonNull final LocalDateTime aInitiationDateTime,
                                  @NonNull @Nonempty final String sMigrationKey)
  {
    ValueEnforcer.notEmpty (sID, "ID");
    ValueEnforcer.notNull (eDirection, "Direction");
    ValueEnforcer.notNull (eState, "State");
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aInitiationDateTime, "InitiationDateTime");
    ValueEnforcer.notEmpty (sMigrationKey, "MigrationKey");

    if (!isValidMigrationKey (sMigrationKey))
      LOGGER.warn ("The migration key '" + sMigrationKey + "' is invalid");

    m_sID = sID;
    m_eDirection = eDirection;
    m_eState = eState;
    m_aParticipantID = aParticipantID;
    m_aInitiationDateTime = aInitiationDateTime;
    m_sMigrationKey = sMigrationKey;
  }

  @NonNull
  @Nonempty
  public final String getID ()
  {
    return m_sID;
  }

  @NonNull
  public final EParticipantMigrationDirection getDirection ()
  {
    return m_eDirection;
  }

  @NonNull
  public final EParticipantMigrationState getState ()
  {
    return m_eState;
  }

  @NonNull
  public EChange setState (@NonNull final EParticipantMigrationState eState)
  {
    ValueEnforcer.notNull (eState, "State");
    if (eState.equals (m_eState))
      return EChange.UNCHANGED;
    m_eState = eState;
    return EChange.CHANGED;
  }

  @NonNull
  public final IParticipantIdentifier getParticipantIdentifier ()
  {
    return m_aParticipantID;
  }

  @NonNull
  public final LocalDateTime getInitiationDateTime ()
  {
    return m_aInitiationDateTime;
  }

  @NonNull
  @Nonempty
  public final String getMigrationKey ()
  {
    return m_sMigrationKey;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPParticipantMigration rhs = (SMPParticipantMigration) o;
    return m_sID.equals (rhs.m_sID);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sID).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ID", m_sID)
                                       .append ("Direction", m_eDirection)
                                       .append ("State", m_eState)
                                       .append ("ParticipantID", m_aParticipantID)
                                       .append ("InitiationDateTime", m_aInitiationDateTime)
                                       .append ("MigrationKey", m_sMigrationKey)
                                       .getToString ();
  }

  @NonNull
  public static SMPParticipantMigration createOutbound (@NonNull final IParticipantIdentifier aParticipantID,
                                                        @NonNull @Nonempty final String sMigrationKey)
  {
    // Outbound starts "in progress"
    return new SMPParticipantMigration (GlobalIDFactory.getNewPersistentStringID (),
                                        EParticipantMigrationDirection.OUTBOUND,
                                        EParticipantMigrationState.IN_PROGRESS,
                                        aParticipantID,
                                        PDTFactory.getCurrentLocalDateTime (),
                                        sMigrationKey);
  }

  @NonNull
  public static SMPParticipantMigration createInbound (@NonNull final IParticipantIdentifier aParticipantID,
                                                       @NonNull @Nonempty final String sMigrationKey)
  {
    // Inbound is directly "migrated"
    return new SMPParticipantMigration (GlobalIDFactory.getNewPersistentStringID (),
                                        EParticipantMigrationDirection.INBOUND,
                                        EParticipantMigrationState.MIGRATED,
                                        aParticipantID,
                                        PDTFactory.getCurrentLocalDateTime (),
                                        sMigrationKey);
  }
}
