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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.misc.ContainsSoftMigration;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringParser;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPoint;
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
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPEndpointMicroTypeConverter.class);
  public static final MicroQName ATTR_ID = new MicroQName ("id");
  public static final MicroQName ATTR_TRANSPORT_PROFILE = new MicroQName ("transportprofile");
  public static final MicroQName ATTR_ACCESS_POINT_ID = new MicroQName ("apid");
  /** @deprecated Only used for reading data written before v8.4.4 */
  @Deprecated
  public static final MicroQName ATTR_ENDPOINT_REFERENCE = new MicroQName ("endpointref");
  public static final MicroQName ATTR_REQUIRE_BUSINESS_LEVEL_SIGNATURE = new MicroQName ("reqblsig");
  public static final MicroQName ATTR_MINIMUM_AUTHENTICATION_LEVEL = new MicroQName ("minauthlevel");
  public static final MicroQName ATTR_SERVICE_ACTIVATION_DATE = new MicroQName ("activation");
  public static final MicroQName ATTR_SERVICE_EXPIRATION_DATE = new MicroQName ("expiration");
  /** @deprecated Only used for reading data written before v8.4.4 */
  @Deprecated
  public static final String ELEMENT_CERTIFICATE = "certificate";
  public static final String ELEMENT_SERVICE_DESCRIPTION = "svcdescription";
  public static final MicroQName ATTR_TECHNICAL_CONTACT_URL = new MicroQName ("techcontacturl");
  public static final MicroQName ATTR_TECHNICAL_INFORMATION_URL = new MicroQName ("techinfourl");
  public static final String ELEMENT_EXTENSION = "extension";

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final SMPEndpoint aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_ID, aValue.getID ());
    aElement.setAttribute (ATTR_TRANSPORT_PROFILE, aValue.getTransportProfile ());
    // The endpoint reference URL and the certificate are stored in the
    // referenced Access Point only
    aElement.setAttribute (ATTR_ACCESS_POINT_ID, aValue.getAccessPointID ());
    aElement.setAttribute (ATTR_REQUIRE_BUSINESS_LEVEL_SIGNATURE, aValue.isRequireBusinessLevelSignature ());
    if (aValue.hasMinimumAuthenticationLevel ())
      aElement.setAttribute (ATTR_MINIMUM_AUTHENTICATION_LEVEL, aValue.getMinimumAuthenticationLevel ());
    if (aValue.hasServiceActivationDateTime ())
      aElement.setAttributeWithConversion (ATTR_SERVICE_ACTIVATION_DATE, aValue.getServiceActivationDateTime ());
    if (aValue.hasServiceExpirationDateTime ())
      aElement.setAttributeWithConversion (ATTR_SERVICE_EXPIRATION_DATE, aValue.getServiceExpirationDateTime ());
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
    return convertToNative (aElement, SMPMetaManager.getAccessPointMgr ());
  }

  @NonNull
  @ContainsSoftMigration
  public static SMPEndpoint convertToNative (@NonNull final IMicroElement aElement,
                                             @NonNull final ISMPAccessPointManager aAccessPointMgr)
  {    // Migration: generate UUID if ID attribute is missing
    String sID = aElement.getAttributeValue (ATTR_ID);
    if (StringHelper.isEmpty (sID))
      sID = SMPEndpointHelper.createUniqueEndpointID ();
    final String sTransportProfile = aElement.getAttributeValue (ATTR_TRANSPORT_PROFILE);
    final String sRequireBusinessLevelSignature = aElement.getAttributeValue (ATTR_REQUIRE_BUSINESS_LEVEL_SIGNATURE);
    final boolean bRequireBusinessLevelSignature = StringParser.parseBool (sRequireBusinessLevelSignature,
                                                                           SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE);
    final String sMinimumAuthenticationLevel = aElement.getAttributeValue (ATTR_MINIMUM_AUTHENTICATION_LEVEL);
    XMLOffsetDateTime aServiceActivationDate = aElement.getAttributeValueWithConversion (ATTR_SERVICE_ACTIVATION_DATE,
                                                                                         XMLOffsetDateTime.class);
    if (aServiceActivationDate == null)
    {
      // Read in old format (without timezone)
      final LocalDateTime aServiceActivationDateLDT = aElement.getAttributeValueWithConversion (ATTR_SERVICE_ACTIVATION_DATE,
                                                                                                LocalDateTime.class);
      if (aServiceActivationDateLDT != null)
        aServiceActivationDate = XMLOffsetDateTime.of (aServiceActivationDateLDT, null);
    }
    XMLOffsetDateTime aServiceExpirationDate = aElement.getAttributeValueWithConversion (ATTR_SERVICE_EXPIRATION_DATE,
                                                                                         XMLOffsetDateTime.class);
    if (aServiceExpirationDate == null)
    {
      // Read in old format (without timezone)
      final LocalDateTime aServiceExpirationDateLDT = aElement.getAttributeValueWithConversion (ATTR_SERVICE_EXPIRATION_DATE,
                                                                                                LocalDateTime.class);
      if (aServiceExpirationDateLDT != null)
        aServiceExpirationDate = XMLOffsetDateTime.of (aServiceExpirationDateLDT, null);
    }
    final String sServiceDescription = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_SERVICE_DESCRIPTION);
    final String sTechnicalContactUrl = aElement.getAttributeValue (ATTR_TECHNICAL_CONTACT_URL);
    final String sTechnicalInformationUrl = aElement.getAttributeValue (ATTR_TECHNICAL_INFORMATION_URL);
    final String sExtension = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_EXTENSION);

    // Resolve the Access Point
    SMPAccessPoint aAccessPoint = null;
    final String sAccessPointID = aElement.getAttributeValue (ATTR_ACCESS_POINT_ID);
    if (StringHelper.isNotEmpty (sAccessPointID))
    {
      // Important: use the managed object as-is and do not create a copy of it. Access Points are
      // shared between all endpoints using the same URL, so that changing the certificate of an
      // Access Point is immediately effective for all of them.
      final ISMPAccessPoint aResolved = aAccessPointMgr.getAccessPointOfID (sAccessPointID);
      if (aResolved instanceof SMPAccessPoint)
        aAccessPoint = (SMPAccessPoint) aResolved;
      else
        if (aResolved != null)
          aAccessPoint = new SMPAccessPoint (aResolved.getID (),
                                             aResolved.getEndpointReference (),
                                             aResolved.getCertificate ());
        else
          LOGGER.warn ("Failed to resolve SMP Access Point with ID '" + sAccessPointID + "'");
    }
    if (aAccessPoint == null)
    {
      // Migration: data written before v8.4.4 contained the endpoint reference
      // and the certificate inline
      final String sEndpointReference = aElement.getAttributeValue (ATTR_ENDPOINT_REFERENCE);
      final String sCertificate = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_CERTIFICATE);
      final ISMPAccessPoint aCreated = aAccessPointMgr.getOrCreateAccessPoint (sEndpointReference, sCertificate);
      if (aCreated instanceof SMPAccessPoint)
        aAccessPoint = (SMPAccessPoint) aCreated;
      else
        aAccessPoint = new SMPAccessPoint (aCreated.getID (),
                                           aCreated.getEndpointReference (),
                                           aCreated.getCertificate ());
    }

    return new SMPEndpoint (sID,
                            sTransportProfile,
                            aAccessPoint,
                            bRequireBusinessLevelSignature,
                            sMinimumAuthenticationLevel,
                            aServiceActivationDate,
                            aServiceExpirationDate,
                            sServiceDescription,
                            sTechnicalContactUrl,
                            sTechnicalInformationUrl,
                            sExtension);
  }

  /**
   * Make an exported endpoint element self-contained by inlining the endpoint reference URL and the
   * certificate of the referenced Access Point and removing the Access Point reference. This is
   * required because Access Point IDs cannot be guaranteed to be unique across multiple
   * installations. The resulting XML uses the format of SMP versions before v8.4.4 and is read back
   * by the soft migration in {@link #convertToNative(IMicroElement, ISMPAccessPointManager)}.
   *
   * @param aEndpointElement
   *        The endpoint element to modify. May not be <code>null</code>.
   * @param aAccessPointMgr
   *        The Access Point manager to resolve the reference. May not be <code>null</code>.
   * @since 8.4.4
   */
  public static void makeSelfContained (@NonNull final IMicroElement aEndpointElement,
                                        @NonNull final ISMPAccessPointManager aAccessPointMgr)
  {
    final String sAccessPointID = aEndpointElement.getAttributeValue (ATTR_ACCESS_POINT_ID);
    if (StringHelper.isEmpty (sAccessPointID))
      return;

    aEndpointElement.removeAttribute (ATTR_ACCESS_POINT_ID);

    final ISMPAccessPoint aAccessPoint = aAccessPointMgr.getAccessPointOfID (sAccessPointID);
    if (aAccessPoint == null)
    {
      LOGGER.warn ("Failed to resolve SMP Access Point with ID '" + sAccessPointID + "' for exporting");
      return;
    }

    if (aAccessPoint.hasEndpointReference ())
      aEndpointElement.setAttribute (ATTR_ENDPOINT_REFERENCE, aAccessPoint.getEndpointReference ());
    if (aAccessPoint.hasCertificate ())
      aEndpointElement.addElementNS (aEndpointElement.getNamespaceURI (), ELEMENT_CERTIFICATE)
                      .addText (aAccessPoint.getCertificate ());
  }
}
