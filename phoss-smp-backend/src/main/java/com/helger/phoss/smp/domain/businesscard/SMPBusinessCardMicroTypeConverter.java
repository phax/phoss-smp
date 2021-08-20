/**
 * Copyright (C) 2015-2021 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.businesscard;

import java.time.LocalDate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.IMicroQName;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * This class is internally used to convert {@link SMPBusinessCard} from and to
 * XML.
 * <p>
 * The files in this package are licensed under Apache 2.0 license
 * </p>
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardMicroTypeConverter implements IMicroTypeConverter <SMPBusinessCard>
{
  private static final IMicroQName ATTR_SERVICE_GROUP_ID = new MicroQName ("servicegroupid");
  private static final String ELEMENT_ENTITY = "entity";
  private static final IMicroQName ATTR_ID = new MicroQName ("id");
  private static final String ELEMENT_NAME = "name";
  private static final IMicroQName ATTR_NAME = new MicroQName ("name");
  private static final IMicroQName ATTR_LANGUAGE_CODE = new MicroQName ("language");
  private static final IMicroQName ATTR_COUNTRY_CODE = new MicroQName ("country");
  private static final String ELEMENT_GEOGRAPHICAL_INFORMATION = "geoinfo";
  private static final String ELEMENT_IDENTIFIER = "identifier";
  private static final IMicroQName ATTR_SCHEME = new MicroQName ("scheme");
  private static final IMicroQName ATTR_VALUE = new MicroQName ("value");
  private static final String ELEMENT_WEBSITE_URI = "website";
  private static final String ELEMENT_CONTACT = "contact";
  private static final IMicroQName ATTR_TYPE = new MicroQName ("type");
  private static final IMicroQName ATTR_PHONE = new MicroQName ("phone");
  private static final IMicroQName ATTR_EMAIL = new MicroQName ("email");
  private static final String ELEMENT_ADDITIONAL_INFORMATION = "additional";
  private static final IMicroQName ATTR_REGISTRATION_DATE = new MicroQName ("regdate");

  @Nonnull
  public static IMicroElement convertToMicroElement (@Nonnull final ISMPBusinessCard aValue,
                                                     @Nullable final String sNamespaceURI,
                                                     @Nonnull @Nonempty final String sTagName,
                                                     final boolean bManualExport)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_SERVICE_GROUP_ID, aValue.getID ());
    for (final SMPBusinessCardEntity aEntity : aValue.getAllEntities ())
    {
      final IMicroElement eEntity = aElement.appendElement (sNamespaceURI, ELEMENT_ENTITY);
      if (!bManualExport)
        eEntity.setAttribute (ATTR_ID, aEntity.getID ());
      for (final SMPBusinessCardName aItem : aEntity.names ())
      {
        final IMicroElement eName = eEntity.appendElement (sNamespaceURI, ELEMENT_NAME);
        eName.setAttribute (ATTR_NAME, aItem.getName ());
        eName.setAttribute (ATTR_LANGUAGE_CODE, aItem.getLanguageCode ());
      }
      eEntity.setAttribute (ATTR_COUNTRY_CODE, aEntity.getCountryCode ());
      if (aEntity.hasGeographicalInformation ())
      {
        eEntity.appendElement (sNamespaceURI, ELEMENT_GEOGRAPHICAL_INFORMATION).appendText (aEntity.getGeographicalInformation ());
      }
      for (final SMPBusinessCardIdentifier aIdentifier : aEntity.identifiers ())
      {
        eEntity.appendElement (sNamespaceURI, ELEMENT_IDENTIFIER)
               .setAttribute (ATTR_ID, aIdentifier.getID ())
               .setAttribute (ATTR_SCHEME, aIdentifier.getScheme ())
               .setAttribute (ATTR_VALUE, aIdentifier.getValue ());
      }
      for (final String sWebsiteURI : aEntity.websiteURIs ())
      {
        eEntity.appendElement (sNamespaceURI, ELEMENT_WEBSITE_URI).appendText (sWebsiteURI);
      }
      for (final SMPBusinessCardContact aContact : aEntity.contacts ())
      {
        eEntity.appendElement (sNamespaceURI, ELEMENT_CONTACT)
               .setAttribute (ATTR_ID, aContact.getID ())
               .setAttribute (ATTR_TYPE, aContact.getType ())
               .setAttribute (ATTR_NAME, aContact.getName ())
               .setAttribute (ATTR_PHONE, aContact.getPhoneNumber ())
               .setAttribute (ATTR_EMAIL, aContact.getEmail ());
      }
      if (aEntity.hasAdditionalInformation ())
      {
        eEntity.appendElement (sNamespaceURI, ELEMENT_ADDITIONAL_INFORMATION).appendText (aEntity.getAdditionalInformation ());
      }
      eEntity.setAttributeWithConversion (ATTR_REGISTRATION_DATE, aEntity.getRegistrationDate ());
    }
    return aElement;
  }

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final SMPBusinessCard aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull @Nonempty final String sTagName)
  {
    return convertToMicroElement (aValue, sNamespaceURI, sTagName, false);
  }

  @Nonnull
  public SMPBusinessCard convertToNative (@Nonnull final IMicroElement aElement)
  {
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final String sServiceGroupID = aElement.getAttributeValue (ATTR_SERVICE_GROUP_ID);

    final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
    if (aParticipantID == null)
      throw new IllegalStateException ("Failed to parse participant ID '" + sServiceGroupID + "'");

    final ICommonsList <SMPBusinessCardEntity> aEntities = new CommonsArrayList <> ();
    for (final IMicroElement eEntity : aElement.getAllChildElements (ELEMENT_ENTITY))
    {
      String sEntityID = eEntity.getAttributeValue (ATTR_ID);
      if (sEntityID == null)
      {
        // In manual import/export the ID is not exported!
        sEntityID = GlobalIDFactory.getNewPersistentStringID ();
      }

      final SMPBusinessCardEntity aEntity = new SMPBusinessCardEntity (sEntityID);
      final String sOldName = eEntity.getAttributeValue (ATTR_NAME);
      if (sOldName != null)
      {
        // No language code
        aEntity.names ().add (new SMPBusinessCardName (sOldName, (String) null));
      }
      else
      {
        // Multiple names with different languages
        for (final IMicroElement eName : eEntity.getAllChildElements (ELEMENT_NAME))
        {
          final String sName = eName.getAttributeValue (ATTR_NAME);
          final String sLanguageCode = eName.getAttributeValue (ATTR_LANGUAGE_CODE);
          aEntity.names ().add (new SMPBusinessCardName (sName, sLanguageCode));
        }
      }
      aEntity.setCountryCode (eEntity.getAttributeValue (ATTR_COUNTRY_CODE));
      aEntity.setGeographicalInformation (MicroHelper.getChildTextContentTrimmed (eEntity, ELEMENT_GEOGRAPHICAL_INFORMATION));
      for (final IMicroElement eIdentifier : eEntity.getAllChildElements (ELEMENT_IDENTIFIER))
      {
        aEntity.identifiers ()
               .add (new SMPBusinessCardIdentifier (eIdentifier.getAttributeValue (ATTR_ID),
                                                    eIdentifier.getAttributeValue (ATTR_SCHEME),
                                                    eIdentifier.getAttributeValue (ATTR_VALUE)));
      }
      for (final IMicroElement eWebsite : eEntity.getAllChildElements (ELEMENT_WEBSITE_URI))
      {
        aEntity.websiteURIs ().add (eWebsite.getTextContentTrimmed ());
      }
      for (final IMicroElement eContact : eEntity.getAllChildElements (ELEMENT_CONTACT))
      {
        aEntity.contacts ()
               .add (new SMPBusinessCardContact (eContact.getAttributeValue (ATTR_ID),
                                                 eContact.getAttributeValue (ATTR_TYPE),
                                                 eContact.getAttributeValue (ATTR_NAME),
                                                 eContact.getAttributeValue (ATTR_PHONE),
                                                 eContact.getAttributeValue (ATTR_EMAIL)));
      }
      aEntity.setAdditionalInformation (MicroHelper.getChildTextContentTrimmed (eEntity, ELEMENT_ADDITIONAL_INFORMATION));
      aEntity.setRegistrationDate (eEntity.getAttributeValueWithConversion (ATTR_REGISTRATION_DATE, LocalDate.class));
      aEntities.add (aEntity);
    }

    return new SMPBusinessCard (aParticipantID, aEntities);
  }
}
