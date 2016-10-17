/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.xml.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * This class is internally used to convert {@link SMPServiceInformation} from
 * and to XML.
 *
 * @author Philip Helger
 */
public final class SMPServiceInformationMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_SERVICE_GROUP_ID = "servicegroupid";
  private static final String ELEMENT_DOCUMENT_TYPE_IDENTIFIER = "doctypeidentifier";
  private static final String ELEMENT_PROCESS = "process";
  private static final String ELEMENT_EXTENSION = "extension";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull @Nonempty final String sTagName)
  {
    final ISMPServiceInformation aValue = (ISMPServiceInformation) aObject;
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_SERVICE_GROUP_ID, aValue.getServiceGroupID ());
    aElement.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getDocumentTypeIdentifier (),
                                                                    sNamespaceURI,
                                                                    ELEMENT_DOCUMENT_TYPE_IDENTIFIER));
    for (final ISMPProcess aProcess : aValue.getAllProcesses ())
      aElement.appendChild (MicroTypeConverter.convertToMicroElement (aProcess, sNamespaceURI, ELEMENT_PROCESS));
    if (aValue.hasExtension ())
      aElement.appendElement (sNamespaceURI, ELEMENT_EXTENSION).appendText (aValue.getExtensionAsString ());
    return aElement;
  }

  @Nonnull
  public ISMPServiceInformation convertToNative (@Nonnull final IMicroElement aElement)
  {
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final String sServiceGroupID = aElement.getAttributeValue (ATTR_SERVICE_GROUP_ID);
    final ISMPServiceGroup aServiceGroup = aSGMgr.getSMPServiceGroupOfID (aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID));
    if (aServiceGroup == null)
      throw new IllegalStateException ("Failed to resolve service group with ID '" + sServiceGroupID + "'");

    final SimpleDocumentTypeIdentifier aDocTypeIdentifier = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_DOCUMENT_TYPE_IDENTIFIER),
                                                                                                SimpleDocumentTypeIdentifier.class);
    final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList<> ();
    for (final IMicroElement aProcess : aElement.getAllChildElements (ELEMENT_PROCESS))
      aProcesses.add (MicroTypeConverter.convertToNative (aProcess, SMPProcess.class));
    final String sExtension = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_EXTENSION);

    return new SMPServiceInformation (aServiceGroup, aDocTypeIdentifier, aProcesses, sExtension);
  }
}
