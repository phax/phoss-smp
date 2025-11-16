/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
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

import com.helger.annotation.Nonempty;
import com.helger.annotation.misc.ContainsSoftMigration;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;

/**
 * This class is internally used to convert {@link SMPParticipantMigration} from and to XML.
 *
 * @author Philip Helger
 * @since 5.4.0
 */
public final class SMPParticipantMigrationMicroTypeConverter implements IMicroTypeConverter <SMPParticipantMigration>
{
  private static final String ATTR_ID = "id";
  private static final String ATTR_DIRECTION = "direction";
  private static final String ATTR_STATE = "state";
  private static final String ELEMENT_PARTICIPANT_IDENTIFIER = "participant";
  private static final String ATTR_INITIATION_DATETIME = "initdt";
  private static final String ATTR_MIGRATION_KEY = "migkey";

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final SMPParticipantMigration aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_ID, aValue.getID ());
    aElement.setAttribute (ATTR_DIRECTION, aValue.getDirection ().getID ());
    aElement.setAttribute (ATTR_STATE, aValue.getState ().getID ());
    aElement.addChild (MicroTypeConverter.convertToMicroElement (aValue.getParticipantIdentifier (),
                                                                 sNamespaceURI,
                                                                 ELEMENT_PARTICIPANT_IDENTIFIER));
    aElement.setAttributeWithConversion (ATTR_INITIATION_DATETIME, aValue.getInitiationDateTime ());
    aElement.setAttribute (ATTR_MIGRATION_KEY, aValue.getMigrationKey ());
    return aElement;
  }

  @NonNull
  @ContainsSoftMigration
  public SMPParticipantMigration convertToNative (@NonNull final IMicroElement aElement)
  {
    final String sID = aElement.getAttributeValue (ATTR_ID);

    final String sDirection = aElement.getAttributeValue (ATTR_DIRECTION);
    final EParticipantMigrationDirection eDirection = EParticipantMigrationDirection.getFromIDOrNull (sDirection);
    if (eDirection == null)
      throw new IllegalStateException ("Failed to resolve Participant Migration Direction with ID '" +
                                       sDirection +
                                       "'");

    String sState = aElement.getAttributeValue (ATTR_STATE);
    if (sState == null)
    {
      // Migration
      sState = EParticipantMigrationState.IN_PROGRESS.getID ();
    }
    final EParticipantMigrationState eState = EParticipantMigrationState.getFromIDOrNull (sState);
    if (eState == null)
      throw new IllegalStateException ("Failed to resolve Participant Migration State with ID '" + sState + "'");

    final SimpleParticipantIdentifier aParticipantID = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_PARTICIPANT_IDENTIFIER),
                                                                                           SimpleParticipantIdentifier.class);

    final LocalDateTime aInitiationDT = aElement.getAttributeValueWithConversion (ATTR_INITIATION_DATETIME,
                                                                                  LocalDateTime.class);

    final String sMigrationKey = aElement.getAttributeValue (ATTR_MIGRATION_KEY);

    return new SMPParticipantMigration (sID, eDirection, eState, aParticipantID, aInitiationDT, sMigrationKey);
  }
}
