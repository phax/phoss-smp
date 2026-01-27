/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.rest;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.mime.CMimeType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.spf.ISMPSPF4PeppolPolicy;
import com.helger.phoss.smp.domain.spf.ISMPSPF4PeppolPolicyManager;
import com.helger.phoss.smp.domain.spf.SPF4PeppolTerm;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * REST API executor for GET /{participantID}/ext/spf
 *
 * @author Steven Noels
 */
public final class APIExecutorSPF4PeppolGet extends AbstractSMPAPIExecutor
{
  /** SPF4Peppol XML namespace */
  public static final String SPF4PEPPOL_NS = "urn:peppol:spf4peppol:1.0";

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map<String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sParticipantID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Check if SPF4Peppol is enabled
    final ISMPSPF4PeppolPolicyManager aSPFMgr = SMPMetaManager.getSPFPolicyMgr ();
    if (aSPFMgr == null)
    {
      throw new SMPPreconditionFailedException ("SPF4Peppol support is not enabled", aDataProvider.getCurrentURI ());
    }

    // Parse the participant identifier
    final IParticipantIdentifier aParticipantID = SMPMetaManager.getIdentifierFactory ()
                                                                 .parseParticipantIdentifier (sParticipantID);
    if (aParticipantID == null)
    {
      throw new SMPBadRequestException ("Invalid participant identifier '" + sParticipantID + "'",
                                        aDataProvider.getCurrentURI ());
    }

    // Get the policy
    final ISMPSPF4PeppolPolicy aPolicy = aSPFMgr.getSPFPolicyOfID (aParticipantID);
    if (aPolicy == null)
    {
      throw new SMPNotFoundException ("No SPF4Peppol policy found for participant '" + sParticipantID + "'",
                                      aDataProvider.getCurrentURI ());
    }

    // Convert to XML per SPF4Peppol schema
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement ePolicy = aDoc.appendElement (SPF4PEPPOL_NS, "Policy");
    ePolicy.setAttribute ("version", ISMPSPF4PeppolPolicy.VERSION);

    // Terms
    final IMicroElement eTerms = ePolicy.appendElement (SPF4PEPPOL_NS, "Terms");
    for (final SPF4PeppolTerm aTerm : aPolicy.getAllTerms ())
    {
      final IMicroElement eTerm = eTerms.appendElement (SPF4PEPPOL_NS, "Term");
      eTerm.setAttribute ("qualifier", aTerm.getQualifier ().getID ());
      eTerm.setAttribute ("mechanism", aTerm.getMechanism ().getID ());
      if (aTerm.hasValue ())
      {
        eTerm.appendText (aTerm.getValue ());
      }
    }

    // Modifiers (optional)
    if (aPolicy.getTTL () != null || aPolicy.hasExplanation ())
    {
      final IMicroElement eModifiers = ePolicy.appendElement (SPF4PEPPOL_NS, "Modifiers");
      if (aPolicy.getTTL () != null)
      {
        eModifiers.appendElement (SPF4PEPPOL_NS, "TTL").appendText (aPolicy.getTTL ().toString ());
      }
      if (aPolicy.hasExplanation ())
      {
        eModifiers.appendElement (SPF4PEPPOL_NS, "Explanation").appendText (aPolicy.getExplanation ());
      }
    }

    // Serialize to XML
    final XMLWriterSettings aXWS = new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN);
    final String sXML = MicroWriter.getNodeAsString (aDoc, aXWS);

    aUnifiedResponse.setContent (sXML.getBytes (StandardCharsets.UTF_8))
                    .setMimeType (CMimeType.APPLICATION_XML)
                    .setCharset (StandardCharsets.UTF_8);
  }
}
