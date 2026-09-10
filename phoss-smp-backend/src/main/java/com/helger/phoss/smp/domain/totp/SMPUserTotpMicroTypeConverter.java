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
package com.helger.phoss.smp.domain.totp;

import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringParser;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

/**
 * This class is internally used to convert {@link SMPUserTotp} from and to XML.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
public final class SMPUserTotpMicroTypeConverter implements IMicroTypeConverter <SMPUserTotp>
{
  private static final MicroQName ATTR_USER_ID = new MicroQName ("userid");
  private static final MicroQName ATTR_SECRET = new MicroQName ("secret");
  private static final MicroQName ATTR_ENABLED = new MicroQName ("enabled");
  private static final MicroQName ATTR_REGISTRATION_DATETIME = new MicroQName ("regdt");
  private static final MicroQName ATTR_LAST_USED_TIME_SLOT = new MicroQName ("lastslot");
  private static final String ELEMENT_RECOVERY_CODE = "recoverycode";
  private static final MicroQName ATTR_RECOVERY_CODE_HASH = new MicroQName ("hash");

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final SMPUserTotp aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_USER_ID, aValue.getID ());
    aElement.setAttribute (ATTR_SECRET, aValue.getSecret ());
    aElement.setAttribute (ATTR_ENABLED, aValue.isEnabled ());
    aElement.setAttributeWithConversion (ATTR_REGISTRATION_DATETIME, aValue.getRegistrationDateTime ());
    if (aValue.hasLastUsedTimeSlot ())
      aElement.setAttribute (ATTR_LAST_USED_TIME_SLOT, aValue.getLastUsedTimeSlot ().longValue ());

    for (final String sRecoveryCodeHash : aValue.getAllRecoveryCodeHashes ())
      aElement.addElementNS (sNamespaceURI, ELEMENT_RECOVERY_CODE)
              .setAttribute (ATTR_RECOVERY_CODE_HASH, sRecoveryCodeHash);
    return aElement;
  }

  @NonNull
  public SMPUserTotp convertToNative (@NonNull final IMicroElement aElement)
  {
    final String sUserID = aElement.getAttributeValue (ATTR_USER_ID);
    final String sSecret = aElement.getAttributeValue (ATTR_SECRET);
    final boolean bEnabled = aElement.getAttributeValueAsBool (ATTR_ENABLED, false);
    final LocalDateTime aRegistrationDT = aElement.getAttributeValueWithConversion (ATTR_REGISTRATION_DATETIME,
                                                                                    LocalDateTime.class);
    final String sLastUsedTimeSlot = aElement.getAttributeValue (ATTR_LAST_USED_TIME_SLOT);
    final Long aLastUsedTimeSlot = sLastUsedTimeSlot == null ? null : StringParser.parseLongObj (sLastUsedTimeSlot);

    final ICommonsList <String> aRecoveryCodeHashes = new CommonsArrayList <> ();
    for (final IMicroElement aChild : aElement.getAllChildElements (ELEMENT_RECOVERY_CODE))
    {
      final String sHash = aChild.getAttributeValue (ATTR_RECOVERY_CODE_HASH);
      if (StringHelper.isNotEmpty (sHash))
        aRecoveryCodeHashes.add (sHash);
    }

    return new SMPUserTotp (sUserID, sSecret, bEnabled, aRegistrationDT, aLastUsedTimeSlot, aRecoveryCodeHashes);
  }
}
