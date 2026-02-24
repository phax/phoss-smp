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

import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.misc.ContainsSoftMigration;
import com.helger.base.string.StringParser;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * This class is internally used to convert {@link SMPEndpoint} from and to XML.
 *
 * @author Philip Helger
 */
public final class SMPEndpointMicroTypeConverter implements IMicroTypeConverter <SMPEndpoint>
{
  private static final MicroQName ATTR_TRANSPORT_PROFILE = new MicroQName ("transportprofile");
  private static final MicroQName ATTR_ENDPOINT_REFERENCE = new MicroQName ("endpointref");
  private static final MicroQName ATTR_REQUIRE_BUSINESS_LEVEL_SIGNATURE = new MicroQName ("reqblsig");
  private static final MicroQName ATTR_MINIMUM_AUTHENTICATION_LEVEL = new MicroQName ("minauthlevel");
  private static final MicroQName ATTR_SERVICE_ACTIVATION_DATE = new MicroQName ("activation");
  private static final MicroQName ATTR_SERVICE_EXPIRATION_DATE = new MicroQName ("expiration");
  private static final String ELEMENT_CERTIFICATE = "certificate";
  private static final String ELEMENT_SERVICE_DESCRIPTION = "svcdescription";
  private static final MicroQName ATTR_TECHNICAL_CONTACT_URL = new MicroQName ("techcontacturl");
  private static final MicroQName ATTR_TECHNICAL_INFORMATION_URL = new MicroQName ("techinfourl");
  private static final String ELEMENT_EXTENSION = "extension";

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final SMPEndpoint aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_TRANSPORT_PROFILE, aValue.getTransportProfile ());
    if (aValue.hasEndpointReference ())
      aElement.setAttribute (ATTR_ENDPOINT_REFERENCE, aValue.getEndpointReference ());
    aElement.setAttribute (ATTR_REQUIRE_BUSINESS_LEVEL_SIGNATURE, aValue.isRequireBusinessLevelSignature ());
    if (aValue.hasMinimumAuthenticationLevel ())
      aElement.setAttribute (ATTR_MINIMUM_AUTHENTICATION_LEVEL, aValue.getMinimumAuthenticationLevel ());
    if (aValue.hasServiceExpirationDateTime ())
      aElement.setAttributeWithConversion (ATTR_SERVICE_ACTIVATION_DATE, aValue.getServiceActivationDateTime ());
    if (aValue.hasServiceExpirationDateTime ())
      aElement.setAttributeWithConversion (ATTR_SERVICE_EXPIRATION_DATE, aValue.getServiceExpirationDateTime ());
    if (aValue.hasCertificate ())
      aElement.addElementNS (sNamespaceURI, ELEMENT_CERTIFICATE).addText (aValue.getCertificate ());
    if (aValue.hasServiceDescription ())
      aElement.addElementNS (sNamespaceURI, ELEMENT_SERVICE_DESCRIPTION).addText (aValue.getServiceDescription ());
    if (aValue.hasTechnicalContactUrl ())
      aElement.setAttribute (ATTR_TECHNICAL_CONTACT_URL, aValue.getTechnicalContactUrl ());
    if (aValue.hasTechnicalInformationUrl ())
      aElement.setAttribute (ATTR_TECHNICAL_INFORMATION_URL, aValue.getTechnicalInformationUrl ());
    if (aValue.getExtensions ().extensions ().isNotEmpty ())
      aElement.addElementNS (sNamespaceURI, ELEMENT_EXTENSION)
              .addText (aValue.getExtensions ().getExtensionsAsJsonString ());
    return aElement;
  }

  @NonNull
  @ContainsSoftMigration

  public SMPEndpoint convertToNative (@NonNull final IMicroElement aElement)
  {
    final String sTransportProfile = aElement.getAttributeValue (ATTR_TRANSPORT_PROFILE);
    final String sEndpointReference = aElement.getAttributeValue (ATTR_ENDPOINT_REFERENCE);
    final String sRequireBusinessLevelSignature = aElement.getAttributeValue (ATTR_REQUIRE_BUSINESS_LEVEL_SIGNATURE);
    final boolean bRequireBusinessLevelSignature = StringParser.parseBool (sRequireBusinessLevelSignature,
                                                                           SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE);
    final String sMinimumAuthenticationLevel = aElement.getAttributeValue (ATTR_MINIMUM_AUTHENTICATION_LEVEL);
    XMLOffsetDateTime aServiceActivationDate = aElement.getAttributeValueWithConversion (ATTR_SERVICE_ACTIVATION_DATE,
                                                                                         XMLOffsetDateTime.class);
    if (aServiceActivationDate == null)
    {
      final LocalDateTime aServiceActivationDateLDT = aElement.getAttributeValueWithConversion (ATTR_SERVICE_ACTIVATION_DATE,
                                                                                                LocalDateTime.class);
      if (aServiceActivationDateLDT != null)
        aServiceActivationDate = XMLOffsetDateTime.of (aServiceActivationDateLDT, null);
    }
    XMLOffsetDateTime aServiceExpirationDate = aElement.getAttributeValueWithConversion (ATTR_SERVICE_EXPIRATION_DATE,
                                                                                         XMLOffsetDateTime.class);
    if (aServiceExpirationDate == null)
    {
      final LocalDateTime aServiceExpirationDateLDT = aElement.getAttributeValueWithConversion (ATTR_SERVICE_EXPIRATION_DATE,
                                                                                                LocalDateTime.class);
      if (aServiceExpirationDateLDT != null)
        aServiceExpirationDate = XMLOffsetDateTime.of (aServiceExpirationDateLDT, null);
    }
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
