/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.domain.serviceinfo;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.StringParser;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * This class is internally used to convert {@link SMPEndpoint} from and to XML.
 *
 * @author Philip Helger
 */
public final class SMPEndpointMicroTypeConverter implements IMicroTypeConverter <SMPEndpoint>
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
  public IMicroElement convertToMicroElement (@Nonnull final SMPEndpoint aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_TRANSPORT_PROFILE, aValue.getTransportProfile ());
    if (StringHelper.hasText (aValue.getEndpointReference ()))
      aElement.setAttribute (ATTR_ENDPOINT_REFERENCE, aValue.getEndpointReference ());
    aElement.setAttribute (ATTR_REQUIRE_BUSINESS_LEVEL_SIGNATURE, aValue.isRequireBusinessLevelSignature ());
    if (StringHelper.hasText (aValue.getMinimumAuthenticationLevel ()))
      aElement.setAttribute (ATTR_MINIMUM_AUTHENTICATION_LEVEL, aValue.getMinimumAuthenticationLevel ());
    aElement.setAttributeWithConversion (ATTR_SERVICE_ACTIVATION_DATE, aValue.getServiceActivationDateTime ());
    aElement.setAttributeWithConversion (ATTR_SERVICE_EXPIRATION_DATE, aValue.getServiceExpirationDateTime ());
    if (StringHelper.hasText (aValue.getCertificate ()))
      aElement.appendElement (sNamespaceURI, ELEMENT_CERTIFICATE).appendText (aValue.getCertificate ());
    if (StringHelper.hasText (aValue.getServiceDescription ()))
      aElement.appendElement (sNamespaceURI, ELEMENT_SERVICE_DESCRIPTION).appendText (aValue.getServiceDescription ());
    if (StringHelper.hasText (aValue.getTechnicalContactUrl ()))
      aElement.setAttribute (ATTR_TECHNICAL_CONTACT_URL, aValue.getTechnicalContactUrl ());
    if (StringHelper.hasText (aValue.getTechnicalInformationUrl ()))
      aElement.setAttribute (ATTR_TECHNICAL_INFORMATION_URL, aValue.getTechnicalInformationUrl ());
    if (aValue.hasExtension ())
      aElement.appendElement (sNamespaceURI, ELEMENT_EXTENSION).appendText (aValue.getExtensionAsString ());
    return aElement;
  }

  @Nonnull
  public SMPEndpoint convertToNative (@Nonnull final IMicroElement aElement)
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
