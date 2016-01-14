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

import org.joda.time.LocalDateTime;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroElement;
import com.helger.commons.microdom.convert.IMicroTypeConverter;
import com.helger.commons.microdom.util.MicroHelper;
import com.helger.commons.string.StringParser;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPEndpoint;

/**
 * This class is internally used to convert {@link SMPEndpoint} from and to XML.
 *
 * @author Philip Helger
 */
public final class SMPEndpointMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_TRANSPORT_PROFILE = "transportprofile";
  private static final String ATTR_ENDPOINT_REFERENCE = "endpointref";
  private static final String ATTR_REQUIRE_BUSINESS_LEVEL_SIGNATURE = "reqblsig";
  private static final String ATTR_MINIMUM_AUTHENTICATION_LEVEL = "minauthlevel";
  private static final String ATTR_SERVICE_ACTIVATION_DATE = "activation";
  private static final String ATTR_SERVICE_EXPIRATION_DATE = "expiration";
  private static final String ELEMENT_CERTIFICATE = "certificate";
  private static final String ELEMENT_SERVICE_DESCRIPTION = "svcdescription";
  private static final String ATTR_TECHNICAL_CONTACT_URL = "techcontacturl";
  private static final String ATTR_TECHNICAL_INFORMATION_URL = "techinfourl";
  private static final String ELEMENT_EXTENSION = "extension";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull @Nonempty final String sTagName)
  {
    final SMPEndpoint aValue = (SMPEndpoint) aObject;
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_TRANSPORT_PROFILE, aValue.getTransportProfile ());
    aElement.setAttribute (ATTR_ENDPOINT_REFERENCE, aValue.getEndpointReference ());
    aElement.setAttribute (ATTR_REQUIRE_BUSINESS_LEVEL_SIGNATURE,
                           Boolean.toString (aValue.isRequireBusinessLevelSignature ()));
    aElement.setAttribute (ATTR_MINIMUM_AUTHENTICATION_LEVEL, aValue.getMinimumAuthenticationLevel ());
    aElement.setAttributeWithConversion (ATTR_SERVICE_ACTIVATION_DATE, aValue.getServiceActivationDateTime ());
    aElement.setAttributeWithConversion (ATTR_SERVICE_EXPIRATION_DATE, aValue.getServiceExpirationDateTime ());
    aElement.appendElement (sNamespaceURI, ELEMENT_CERTIFICATE).appendText (aValue.getCertificate ());
    aElement.appendElement (sNamespaceURI, ELEMENT_SERVICE_DESCRIPTION).appendText (aValue.getServiceDescription ());
    aElement.setAttribute (ATTR_TECHNICAL_CONTACT_URL, aValue.getTechnicalContactUrl ());
    aElement.setAttribute (ATTR_TECHNICAL_INFORMATION_URL, aValue.getTechnicalInformationUrl ());
    if (aValue.hasExtension ())
      aElement.appendElement (sNamespaceURI, ELEMENT_EXTENSION).appendText (aValue.getExtension ());
    return aElement;
  }

  @Nonnull
  public ISMPEndpoint convertToNative (@Nonnull final IMicroElement aElement)
  {
    final String sTransportProfile = aElement.getAttributeValue (ATTR_TRANSPORT_PROFILE);
    final String sEndpointReference = aElement.getAttributeValue (ATTR_ENDPOINT_REFERENCE);
    final String sRequireBusinessLevelSignature = aElement.getAttributeValue (ATTR_REQUIRE_BUSINESS_LEVEL_SIGNATURE);
    final boolean bRequireBusinessLevelSignature = StringParser.parseBool (sRequireBusinessLevelSignature, false);
    final String sMinimumAuthenticationLevel = aElement.getAttributeValue (ATTR_MINIMUM_AUTHENTICATION_LEVEL);
    final LocalDateTime aServiceActivationDate = aElement.getAttributeValueWithConversion (ATTR_SERVICE_ACTIVATION_DATE,
                                                                                           LocalDateTime.class);
    final LocalDateTime aServiceExpirationDate = aElement.getAttributeValueWithConversion (ATTR_SERVICE_EXPIRATION_DATE,
                                                                                           LocalDateTime.class);
    final String sCertificate = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_CERTIFICATE);
    final String sServiceDescription = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_SERVICE_DESCRIPTION);
    final String sTechnicalContactUrl = aElement.getAttributeValue (ATTR_TECHNICAL_CONTACT_URL);
    final String sTechnicalInformationUrl = aElement.getAttributeValue (ATTR_TECHNICAL_INFORMATION_URL);
    final String sExtension = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_EXTENSION);

    return new SMPEndpoint (sTransportProfile,
                            sEndpointReference,
                            bRequireBusinessLevelSignature,
                            sMinimumAuthenticationLevel,
                            aServiceActivationDate,
                            aServiceExpirationDate,
                            sCertificate,
                            sServiceDescription,
                            sTechnicalContactUrl,
                            sTechnicalInformationUrl,
                            sExtension);
  }
}
