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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.LocalDate;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroElement;
import com.helger.commons.microdom.convert.IMicroTypeConverter;
import com.helger.commons.microdom.util.MicroHelper;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardContact;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardEntity;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardIdentifier;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;

/**
 * This class is internally used to convert {@link SMPBusinessCard} from and to
 * XML.
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_SERVICE_GROUPD_ID = "servicegroupid";
  private static final String ELEMENT_ENTITY = "entity";
  private static final String ATTR_NAME = "name";
  private static final String ATTR_COUNTRY_CODE = "country";
  private static final String ELEMENT_GEOGRAPHICAL_INFORMATION = "geoinfo";
  private static final String ELEMENT_IDENTIFIER = "identifier";
  private static final String ATTR_SCHEME = "scheme";
  private static final String ATTR_VALUE = "value";
  private static final String ELEMENT_WEBSITE_URI = "website";
  private static final String ELEMENT_CONTACT = "contact";
  private static final String ATTR_TYPE = "type";
  private static final String ATTR_PHONE = "phone";
  private static final String ATTR_EMAIL = "email";
  private static final String ELEMENT_ADDITIONAL_INFORMATION = "additional";
  private static final String ATTR_REGISTRATION_DATE = "regdate";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull @Nonempty final String sTagName)
  {
    final ISMPBusinessCard aValue = (ISMPBusinessCard) aObject;
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_SERVICE_GROUPD_ID, aValue.getServiceGroupID ());
    for (final SMPBusinessCardEntity aEntity : aValue.getAllEntities ())
    {
      final IMicroElement eEntity = aElement.appendElement (sNamespaceURI, ELEMENT_ENTITY);
      eEntity.setAttribute (ATTR_NAME, aEntity.getName ());
      eEntity.setAttribute (ATTR_COUNTRY_CODE, aEntity.getCountryCode ());
      if (aEntity.hasGeographicalInformation ())
      {
        eEntity.appendElement (sNamespaceURI, ELEMENT_GEOGRAPHICAL_INFORMATION)
               .appendText (aEntity.getGeographicalInformation ());
      }
      for (final SMPBusinessCardIdentifier aIdentifier : aEntity.getIdentifiers ())
      {
        eEntity.appendElement (sNamespaceURI, ELEMENT_IDENTIFIER)
               .setAttribute (ATTR_SCHEME, aIdentifier.getScheme ())
               .setAttribute (ATTR_VALUE, aIdentifier.getValue ());
      }
      for (final String sWebsiteURI : aEntity.getWebsiteURIs ())
      {
        eEntity.appendElement (sNamespaceURI, ELEMENT_WEBSITE_URI).appendText (sWebsiteURI);
      }
      for (final SMPBusinessCardContact aContact : aEntity.getContacts ())
      {
        eEntity.appendElement (sNamespaceURI, ELEMENT_CONTACT)
               .setAttribute (ATTR_TYPE, aContact.getType ())
               .setAttribute (ATTR_NAME, aContact.getName ())
               .setAttribute (ATTR_PHONE, aContact.getPhoneNumber ())
               .setAttribute (ATTR_EMAIL, aContact.getEmail ());
      }
      if (aEntity.hasAdditionalInformation ())
      {
        eEntity.appendElement (sNamespaceURI, ELEMENT_ADDITIONAL_INFORMATION)
               .appendText (aEntity.getAdditionalInformation ());
      }
      eEntity.setAttributeWithConversion (ATTR_REGISTRATION_DATE, aEntity.getRegistrationDate ());
    }
    return aElement;
  }

  @Nonnull
  public ISMPBusinessCard convertToNative (@Nonnull final IMicroElement aElement)
  {
    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final String sServiceGroupID = aElement.getAttributeValue (ATTR_SERVICE_GROUPD_ID);
    final ISMPServiceGroup aServiceGroup = aSGMgr.getSMPServiceGroupOfID (SimpleParticipantIdentifier.createFromURIPart (sServiceGroupID));
    if (aServiceGroup == null)
      throw new IllegalStateException ("Failed to resolve service group with ID '" + sServiceGroupID + "'");

    final List <SMPBusinessCardEntity> aEntities = new ArrayList <> ();
    for (final IMicroElement eEntity : aElement.getAllChildElements (ELEMENT_ENTITY))
    {
      final SMPBusinessCardEntity aEntity = new SMPBusinessCardEntity ();
      aEntity.setName (eEntity.getAttributeValue (ATTR_NAME));
      aEntity.setCountryCode (eEntity.getAttributeValue (ATTR_COUNTRY_CODE));
      aEntity.setGeographicalInformation (MicroHelper.getChildTextContentTrimmed (eEntity,
                                                                                  ELEMENT_GEOGRAPHICAL_INFORMATION));
      for (final IMicroElement eIdentifier : eEntity.getAllChildElements (ELEMENT_IDENTIFIER))
      {
        aEntity.getIdentifiers ().add (new SMPBusinessCardIdentifier (eIdentifier.getAttributeValue (ATTR_SCHEME),
                                                                      eIdentifier.getAttributeValue (ATTR_VALUE)));
      }
      for (final IMicroElement eWebsite : eEntity.getAllChildElements (ELEMENT_WEBSITE_URI))
      {
        aEntity.getWebsiteURIs ().add (eWebsite.getTextContentTrimmed ());
      }
      for (final IMicroElement eContact : eEntity.getAllChildElements (ELEMENT_CONTACT))
      {
        aEntity.getContacts ()
               .add (new SMPBusinessCardContact (eContact.getAttributeValue (ATTR_TYPE),
                                                 eContact.getAttributeValue (ATTR_NAME),
                                                 eContact.getAttributeValue (ATTR_PHONE),
                                                 eContact.getAttributeValue (ATTR_EMAIL)));
      }
      aEntity.setAdditionalInformation (MicroHelper.getChildTextContentTrimmed (eEntity,
                                                                                ELEMENT_ADDITIONAL_INFORMATION));
      aEntity.setRegistrationDate (eEntity.getAttributeValueWithConversion (ATTR_REGISTRATION_DATE, LocalDate.class));
    }

    return new SMPBusinessCard (aServiceGroup, aEntities);
  }
}
