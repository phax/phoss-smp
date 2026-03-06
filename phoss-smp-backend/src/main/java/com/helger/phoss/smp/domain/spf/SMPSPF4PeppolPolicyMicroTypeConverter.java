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
package com.helger.phoss.smp.domain.spf;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.IMicroQName;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * MicroTypeConverter for {@link SMPSPF4PeppolPolicy}.
 *
 * @author Steven Noels
 */
public final class SMPSPF4PeppolPolicyMicroTypeConverter implements IMicroTypeConverter <SMPSPF4PeppolPolicy>
{
  private static final IMicroQName ATTR_PARTICIPANT_ID = new MicroQName ("participantid");
  private static final IMicroQName ATTR_TTL = new MicroQName ("ttl");
  private static final String ELEMENT_EXPLANATION = "explanation";
  private static final String ELEMENT_TERM = "term";
  private static final IMicroQName ATTR_QUALIFIER = new MicroQName ("qualifier");
  private static final IMicroQName ATTR_MECHANISM = new MicroQName ("mechanism");
  private static final IMicroQName ATTR_VALUE = new MicroQName ("value");

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final SMPSPF4PeppolPolicy aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_PARTICIPANT_ID, aValue.getID ());
    if (aValue.getTTL () != null)
    {
      aElement.setAttribute (ATTR_TTL, aValue.getTTL ().intValue ());
    }
    if (aValue.hasExplanation ())
    {
      aElement.addElementNS (sNamespaceURI, ELEMENT_EXPLANATION).addText (aValue.getExplanation ());
    }
    for (final SPF4PeppolTerm aTerm : aValue.getAllTerms ())
    {
      final IMicroElement eTerm = aElement.addElementNS (sNamespaceURI, ELEMENT_TERM);
      eTerm.setAttribute (ATTR_QUALIFIER, aTerm.getQualifier ().getID ());
      eTerm.setAttribute (ATTR_MECHANISM, aTerm.getMechanism ().getID ());
      if (aTerm.hasValue ())
      {
        eTerm.setAttribute (ATTR_VALUE, aTerm.getValue ());
      }
    }
    return aElement;
  }

  @NonNull
  public SMPSPF4PeppolPolicy convertToNative (@NonNull final IMicroElement aElement)
  {
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final String sParticipantID = aElement.getAttributeValue (ATTR_PARTICIPANT_ID);

    final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sParticipantID);
    if (aParticipantID == null)
      throw new IllegalStateException ("Failed to parse participant ID '" + sParticipantID + "'");

    // TTL is optional
    final String sTTL = aElement.getAttributeValue (ATTR_TTL);
    final Integer aTTL = sTTL != null ? Integer.valueOf (sTTL) : null;

    // Explanation is optional
    final String sExplanation = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_EXPLANATION);

    // Parse terms
    final ICommonsList <SPF4PeppolTerm> aTerms = new CommonsArrayList <> ();
    for (final IMicroElement eTerm : aElement.getAllChildElements (ELEMENT_TERM))
    {
      final String sQualifier = eTerm.getAttributeValue (ATTR_QUALIFIER);
      final String sMechanism = eTerm.getAttributeValue (ATTR_MECHANISM);
      final String sValue = eTerm.getAttributeValue (ATTR_VALUE);

      final ESPF4PeppolQualifier eQualifier = ESPF4PeppolQualifier.getFromIDOrNull (sQualifier);
      final ESPF4PeppolMechanism eMechanism = ESPF4PeppolMechanism.getFromIDOrNull (sMechanism);

      if (eQualifier == null)
        throw new IllegalStateException ("Invalid SPF4Peppol qualifier: " + sQualifier);
      if (eMechanism == null)
        throw new IllegalStateException ("Invalid SPF4Peppol mechanism: " + sMechanism);

      aTerms.add (new SPF4PeppolTerm (eQualifier, eMechanism, sValue));
    }

    return new SMPSPF4PeppolPolicy (aParticipantID, aTerms, aTTL, sExplanation);
  }
}
