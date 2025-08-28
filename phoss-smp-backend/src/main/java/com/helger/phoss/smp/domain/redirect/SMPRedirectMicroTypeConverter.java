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
package com.helger.phoss.smp.domain.redirect;

import java.security.cert.X509Certificate;

import com.helger.annotation.Nonempty;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.simple.doctype.SimpleDocumentTypeIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupProvider;
import com.helger.security.certificate.CertificateHelper;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This class is internally used to convert {@link SMPRedirect} from and to XML.
 *
 * @author Philip Helger
 */
public final class SMPRedirectMicroTypeConverter implements IMicroTypeConverter <SMPRedirect>
{
  private static final String ATTR_SERVICE_GROUPD_ID = "servicegroupid";
  private static final String ELEMENT_DOCUMENT_TYPE_IDENTIFIER = "doctypeidentifier";
  private static final String ATTR_TARGET_HREF = "targethref";
  private static final String ELEMENT_CERTIFICATE_SUID = "suid";
  private static final String ELEMENT_CERTIFICATE = "certificate";
  private static final String ELEMENT_EXTENSION = "extension";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final SMPRedirect aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_SERVICE_GROUPD_ID, aValue.getServiceGroupID ());
    aElement.addChild (MicroTypeConverter.convertToMicroElement (aValue.getDocumentTypeIdentifier (),
                                                                 sNamespaceURI,
                                                                 ELEMENT_DOCUMENT_TYPE_IDENTIFIER));
    aElement.setAttribute (ATTR_TARGET_HREF, aValue.getTargetHref ());
    aElement.addElementNS (sNamespaceURI, ELEMENT_CERTIFICATE_SUID).addText (aValue.getSubjectUniqueIdentifier ());
    if (aValue.hasCertificate ())
      aElement.addElementNS (sNamespaceURI, ELEMENT_CERTIFICATE)
              .addText (CertificateHelper.getPEMEncodedCertificate (aValue.getCertificate ()));
    if (aValue.getExtensions ().extensions ().isNotEmpty ())
      aElement.addElementNS (sNamespaceURI, ELEMENT_EXTENSION)
              .addText (aValue.getExtensions ().getExtensionsAsJsonString ());
    return aElement;
  }

  @Nonnull
  public static SMPRedirect convertToNative (@Nonnull final IMicroElement aElement,
                                             @Nonnull final ISMPServiceGroupProvider aSGProvider)
  {
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final String sServiceGroupID = aElement.getAttributeValue (ATTR_SERVICE_GROUPD_ID);
    final ISMPServiceGroup aServiceGroup = aSGProvider.getSMPServiceGroupOfID (aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID));
    if (aServiceGroup == null)
      throw new IllegalStateException ("Failed to resolve service group with ID '" + sServiceGroupID + "'");

    final SimpleDocumentTypeIdentifier aDocTypeIdentifier = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_DOCUMENT_TYPE_IDENTIFIER),
                                                                                                SimpleDocumentTypeIdentifier.class);
    final String sTargetHref = aElement.getAttributeValue (ATTR_TARGET_HREF);
    final String sSubjectUniqueIdentifier = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_CERTIFICATE_SUID);
    final X509Certificate aCertificate = CertificateHelper.convertStringToCertficateOrNull (MicroHelper.getChildTextContentTrimmed (aElement,
                                                                                                                                    ELEMENT_CERTIFICATE));
    final String sExtension = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_EXTENSION);

    return new SMPRedirect (aServiceGroup.getParticipantIdentifier (),
                            aDocTypeIdentifier,
                            sTargetHref,
                            sSubjectUniqueIdentifier,
                            aCertificate,
                            sExtension);
  }

  @Nonnull
  public SMPRedirect convertToNative (@Nonnull final IMicroElement aElement)
  {
    return convertToNative (aElement, SMPMetaManager.getServiceGroupMgr ());
  }
}
