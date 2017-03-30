/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.domain.businesscard;

import java.time.LocalDate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupProvider;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * This class is internally used to convert {@link SMPBusinessCard} from and to
 * XML.
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_SERVICE_GROUP_ID = "servicegroupid";
  private static final String ELEMENT_ENTITY = "entity";
  private static final String ATTR_ID = "id";
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
  public static IMicroElement convertToMicroElement (@Nonnull final ISMPBusinessCard aValue,
                                                     @Nullable final String sNamespaceURI,
                                                     @Nonnull @Nonempty final String sTagName,
                                                     final boolean bManualExport)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_SERVICE_GROUP_ID, aValue.getServiceGroupID ());
    for (final SMPBusinessCardEntity aEntity : aValue.getAllEntities ())
    {
      final IMicroElement eEntity = aElement.appendElement (sNamespaceURI, ELEMENT_ENTITY);
      if (!bManualExport)
        eEntity.setAttribute (ATTR_ID, aEntity.getID ());
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
               .setAttribute (ATTR_ID, aIdentifier.getID ())
               .setAttribute (ATTR_SCHEME, aIdentifier.getScheme ())
               .setAttribute (ATTR_VALUE, aIdentifier.getValue ());
      }
      for (final String sWebsiteURI : aEntity.getAllWebsiteURIs ())
      {
        eEntity.appendElement (sNamespaceURI, ELEMENT_WEBSITE_URI).appendText (sWebsiteURI);
      }
      for (final SMPBusinessCardContact aContact : aEntity.getContacts ())
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
        eEntity.appendElement (sNamespaceURI, ELEMENT_ADDITIONAL_INFORMATION)
               .appendText (aEntity.getAdditionalInformation ());
      }
      eEntity.setAttributeWithConversion (ATTR_REGISTRATION_DATE, aEntity.getRegistrationDate ());
    }
    return aElement;
  }

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull @Nonempty final String sTagName)
  {
    return convertToMicroElement ((ISMPBusinessCard) aObject, sNamespaceURI, sTagName, false);
  }

  @Nonnull
  public static ISMPBusinessCard convertToNative (@Nonnull final IMicroElement aElement,
                                                  @Nonnull final ISMPServiceGroupProvider aSGProvider)
  {
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final String sServiceGroupID = aElement.getAttributeValue (ATTR_SERVICE_GROUP_ID);

    final ISMPServiceGroup aServiceGroup = aSGProvider.getSMPServiceGroupOfID (aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID));
    if (aServiceGroup == null)
      throw new IllegalStateException ("Failed to resolve service group with ID '" + sServiceGroupID + "'");

    final ICommonsList <SMPBusinessCardEntity> aEntities = new CommonsArrayList<> ();
    for (final IMicroElement eEntity : aElement.getAllChildElements (ELEMENT_ENTITY))
    {
      String sEntityID = eEntity.getAttributeValue (ATTR_ID);
      if (sEntityID == null)
      {
        // In manual import/export the ID is not exported!
        sEntityID = GlobalIDFactory.getNewPersistentStringID ();
      }

      final SMPBusinessCardEntity aEntity = new SMPBusinessCardEntity (sEntityID);
      aEntity.setName (eEntity.getAttributeValue (ATTR_NAME));
      aEntity.setCountryCode (eEntity.getAttributeValue (ATTR_COUNTRY_CODE));
      aEntity.setGeographicalInformation (MicroHelper.getChildTextContentTrimmed (eEntity,
                                                                                  ELEMENT_GEOGRAPHICAL_INFORMATION));
      for (final IMicroElement eIdentifier : eEntity.getAllChildElements (ELEMENT_IDENTIFIER))
      {
        aEntity.addIdentifier (new SMPBusinessCardIdentifier (eIdentifier.getAttributeValue (ATTR_ID),
                                                              eIdentifier.getAttributeValue (ATTR_SCHEME),
                                                              eIdentifier.getAttributeValue (ATTR_VALUE)));
      }
      for (final IMicroElement eWebsite : eEntity.getAllChildElements (ELEMENT_WEBSITE_URI))
      {
        aEntity.addWebsiteURI (eWebsite.getTextContentTrimmed ());
      }
      for (final IMicroElement eContact : eEntity.getAllChildElements (ELEMENT_CONTACT))
      {
        aEntity.addContact (new SMPBusinessCardContact (eContact.getAttributeValue (ATTR_ID),
                                                        eContact.getAttributeValue (ATTR_TYPE),
                                                        eContact.getAttributeValue (ATTR_NAME),
                                                        eContact.getAttributeValue (ATTR_PHONE),
                                                        eContact.getAttributeValue (ATTR_EMAIL)));
      }
      aEntity.setAdditionalInformation (MicroHelper.getChildTextContentTrimmed (eEntity,
                                                                                ELEMENT_ADDITIONAL_INFORMATION));
      aEntity.setRegistrationDate (eEntity.getAttributeValueWithConversion (ATTR_REGISTRATION_DATE, LocalDate.class));
      aEntities.add (aEntity);
    }

    return new SMPBusinessCard (aServiceGroup, aEntities);
  }

  @Nonnull
  public ISMPBusinessCard convertToNative (@Nonnull final IMicroElement aElement)
  {
    return convertToNative (aElement, SMPMetaManager.getServiceGroupMgr ());
  }
}
