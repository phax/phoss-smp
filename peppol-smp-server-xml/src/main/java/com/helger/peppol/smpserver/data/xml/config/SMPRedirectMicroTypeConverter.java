/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
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
import com.helger.peppol.smpserver.domain.MetaManager;
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
    final ISMPServiceGroupManager aSGMgr = MetaManager.getServiceGroupMgr ();
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
