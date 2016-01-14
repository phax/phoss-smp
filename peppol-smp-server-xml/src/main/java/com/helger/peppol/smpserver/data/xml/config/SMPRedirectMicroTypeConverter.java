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
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroElement;
import com.helger.commons.microdom.convert.IMicroTypeConverter;
import com.helger.commons.microdom.convert.MicroTypeConverter;
import com.helger.commons.microdom.util.MicroHelper;
import com.helger.peppol.identifier.IMutableDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.SMPRedirect;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;

/**
 * This class is internally used to convert {@link SMPRedirect} from and to XML.
 *
 * @author Philip Helger
 */
public final class SMPRedirectMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_SERVICE_GROUPD_ID = "servicegroupid";
  private static final String ELEMENT_DOCUMENT_TYPE_IDENTIFIER = "doctypeidentifier";
  private static final String ATTR_TARGET_HREF = "targethref";
  private static final String ELEMENT_CERTIFICATE_SUID = "suid";
  private static final String ELEMENT_EXTENSION = "extension";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull @Nonempty final String sTagName)
  {
    final SMPRedirect aValue = (SMPRedirect) aObject;
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_SERVICE_GROUPD_ID, aValue.getServiceGroupID ());
    aElement.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getDocumentTypeIdentifier (),
                                                                    sNamespaceURI,
                                                                    ELEMENT_DOCUMENT_TYPE_IDENTIFIER));
    aElement.setAttribute (ATTR_TARGET_HREF, aValue.getTargetHref ());
    aElement.appendElement (sNamespaceURI, ELEMENT_CERTIFICATE_SUID).appendText (aValue.getSubjectUniqueIdentifier ());
    if (aValue.hasExtension ())
      aElement.appendElement (sNamespaceURI, ELEMENT_EXTENSION).appendText (aValue.getExtension ());
    return aElement;
  }

  @Nonnull
  public ISMPRedirect convertToNative (@Nonnull final IMicroElement aElement)
  {
    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final String sServiceGroupID = aElement.getAttributeValue (ATTR_SERVICE_GROUPD_ID);
    final ISMPServiceGroup aServiceGroup = aSGMgr.getSMPServiceGroupOfID (SimpleParticipantIdentifier.createFromURIPart (sServiceGroupID));
    if (aServiceGroup == null)
      throw new IllegalStateException ("Failed to resolve service group with ID '" + sServiceGroupID + "'");

    final IMutableDocumentTypeIdentifier aDocTypeIdentifier = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_DOCUMENT_TYPE_IDENTIFIER),
                                                                                                  IMutableDocumentTypeIdentifier.class);
    final String sTargetHref = aElement.getAttributeValue (ATTR_TARGET_HREF);
    final String sSubjectUniqueIdentifier = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_CERTIFICATE_SUID);
    final String sExtension = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_EXTENSION);

    return new SMPRedirect (aServiceGroup, aDocTypeIdentifier, sTargetHref, sSubjectUniqueIdentifier, sExtension);
  }
}
