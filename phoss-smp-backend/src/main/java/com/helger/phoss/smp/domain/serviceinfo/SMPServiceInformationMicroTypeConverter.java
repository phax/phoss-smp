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
package com.helger.phoss.smp.domain.serviceinfo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.simple.doctype.SimpleDocumentTypeIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupProvider;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * This class is internally used to convert {@link SMPServiceInformation} from and to XML.
 *
 * @author Philip Helger
 */
public final class SMPServiceInformationMicroTypeConverter implements IMicroTypeConverter <SMPServiceInformation>
{
  private static final MicroQName ATTR_SERVICE_GROUP_ID = new MicroQName ("servicegroupid");
  private static final String ELEMENT_DOCUMENT_TYPE_IDENTIFIER = "doctypeidentifier";
  private static final String ELEMENT_PROCESS = "process";
  private static final String ELEMENT_EXTENSION = "extension";

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final SMPServiceInformation aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_SERVICE_GROUP_ID, aValue.getServiceGroupID ());
    aElement.addChild (MicroTypeConverter.convertToMicroElement (aValue.getDocumentTypeIdentifier (),
                                                                 sNamespaceURI,
                                                                 ELEMENT_DOCUMENT_TYPE_IDENTIFIER));
    for (final ISMPProcess aProcess : aValue.getAllProcesses ())
      aElement.addChild (MicroTypeConverter.convertToMicroElement (aProcess, sNamespaceURI, ELEMENT_PROCESS));
    if (aValue.getExtensions ().extensions ().isNotEmpty ())
      aElement.addElementNS (sNamespaceURI, ELEMENT_EXTENSION)
              .addText (aValue.getExtensions ().getExtensionsAsJsonString ());
    return aElement;
  }

  @NonNull
  public static SMPServiceInformation convertToNative (@NonNull final IMicroElement aElement,
                                                       @NonNull final ISMPServiceGroupProvider aSGProvider)
  {
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final String sServiceGroupID = aElement.getAttributeValue (ATTR_SERVICE_GROUP_ID);
    final ISMPServiceGroup aServiceGroup = aSGProvider.getSMPServiceGroupOfID (aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID));
    if (aServiceGroup == null)
      throw new IllegalStateException ("Failed to resolve service group with ID '" + sServiceGroupID + "'");

    final SimpleDocumentTypeIdentifier aDocTypeIdentifier = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_DOCUMENT_TYPE_IDENTIFIER),
                                                                                                SimpleDocumentTypeIdentifier.class);
    final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList <> ();
    for (final IMicroElement aProcess : aElement.getAllChildElements (ELEMENT_PROCESS))
      aProcesses.add (MicroTypeConverter.convertToNative (aProcess, SMPProcess.class));
    final String sExtension = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_EXTENSION);

    return new SMPServiceInformation (aServiceGroup.getParticipantIdentifier (),
                                      aDocTypeIdentifier,
                                      aProcesses,
                                      sExtension);
  }

  @NonNull
  public SMPServiceInformation convertToNative (@NonNull final IMicroElement aElement)
  {
    return convertToNative (aElement, SMPMetaManager.getServiceGroupMgr ());
  }
}
