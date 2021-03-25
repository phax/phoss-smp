/**
 * Copyright (C) 2015-2021 Philip Helger and contributors
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
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
    return StringHelper.hasText (sMigrationKey) && RegExHelper.stringMatchesPattern (CSMLDefault.MIGRATION_CODE_PATTERN, sMigrationKey);
  }

  protected SMPParticipantMigration (@Nonnull @Nonempty final String sID,
                                     @Nonnull final EParticipantMigrationDirection eDirection,
                                     @Nonnull final EParticipantMigrationState eState,
                                     @Nonnull final IParticipantIdentifier aParticipantID,
                                     @Nonnull final LocalDateTime aInitiationDateTime,
                                     @Nonnull @Nonempty final String sMigrationKey)
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

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public EParticipantMigrationDirection getDirection ()
  {
    return m_eDirection;
  }

  @Nonnull
  public EParticipantMigrationState getState ()
  {
    return m_eState;
  }

  @Nonnull
  public EChange setState (@Nonnull final EParticipantMigrationState eState)
  {
    ValueEnforcer.notNull (eState, "State");
    if (eState.equals (m_eState))
      return EChange.UNCHANGED;
    m_eState = eState;
    return EChange.CHANGED;
  }

  @Nonnull
  public IParticipantIdentifier getParticipantIdentifier ()
  {
    return m_aParticipantID;
  }

  @Nonnull
  public LocalDateTime getInitiationDateTime ()
  {
    return m_aInitiationDateTime;
  }

  @Nonnull
  @Nonempty
  public String getMigrationKey ()
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

  @Nonnull
  public static SMPParticipantMigration createOutbound (@Nonnull final IParticipantIdentifier aParticipantID,
                                                        @Nonnull @Nonempty final String sMigrationKey)
  {
    return new SMPParticipantMigration (GlobalIDFactory.getNewPersistentStringID (),
                                        EParticipantMigrationDirection.OUTBOUND,
                                        EParticipantMigrationState.IN_PROGRESS,
                                        aParticipantID,
                                        PDTFactory.getCurrentLocalDateTime (),
                                        sMigrationKey);
  }

  @Nonnull
  public static SMPParticipantMigration createInbound (@Nonnull final IParticipantIdentifier aParticipantID,
                                                       @Nonnull @Nonempty final String sMigrationKey)
  {
    return new SMPParticipantMigration (GlobalIDFactory.getNewPersistentStringID (),
                                        EParticipantMigrationDirection.INBOUND,
                                        EParticipantMigrationState.IN_PROGRESS,
                                        aParticipantID,
                                        PDTFactory.getCurrentLocalDateTime (),
                                        sMigrationKey);
  }
}
