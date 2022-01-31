/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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
package com.helger.phoss.smp.exchange;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.CommonsLinkedHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.error.IError;
import com.helger.commons.error.SingleError;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardMicroTypeConverter;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.redirect.SMPRedirectMicroTypeConverter;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroupMicroTypeConverter;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformationMicroTypeConverter;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.helger.xml.microdom.IMicroElement;

/**
 * Import Service Groups from XML.
 *
 * @author Philip Helger
 * @since 6.0.0
 */
@Immutable
public final class ServiceGroupImport
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceGroupImport.class);
  private static final AtomicInteger COUNTER = new AtomicInteger (0);

  private ServiceGroupImport ()
  {}

  public static void importXMLVer10 (@Nonnull final IMicroElement eRoot,
                                     final boolean bOverwriteExisting,
                                     @Nonnull final IUser aDefaultOwner,
                                     @Nonnull final ICommonsSet <String> aAllServiceGroupIDs,
                                     @Nonnull final ICommonsSet <String> aAllBusinessCardIDs,
                                     @Nonnull final ICommonsList <IError> aActionList)
  {
    ValueEnforcer.notNull (eRoot, "Root");
    ValueEnforcer.notNull (aDefaultOwner, "DefaultOwner");
    ValueEnforcer.notNull (aAllServiceGroupIDs, "AllServiceGroupIDs");
    ValueEnforcer.notNull (aAllBusinessCardIDs, "AllBusinessCardIDs");
    ValueEnforcer.notNull (aActionList, "ActionList");

    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Starting import of Service Groups from XML v1.0, overwrite is " + (bOverwriteExisting ? "enabled" : "disabled"));

    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();

    final ICommonsOrderedMap <ISMPServiceGroup, ServiceGroupImportData> aImportServiceGroups = new CommonsLinkedHashMap <> ();
    final ICommonsList <ISMPServiceGroup> aDeleteServiceGroups = new CommonsArrayList <> ();

    final String sLogPrefix = "[SG-IMPORT-" + COUNTER.incrementAndGet () + "] ";
    final Consumer <String> aLoggerSuccess = s -> {
      LOGGER.info (sLogPrefix + s);
      aActionList.add (SingleError.builderSuccess ().errorText (s).build ());
    };
    final Consumer <String> aLoggerInfo = s -> {
      LOGGER.info (sLogPrefix + s);
      aActionList.add (SingleError.builderInfo ().errorText (s).build ());
    };
    final Consumer <String> aLoggerWarn = s -> {
      LOGGER.warn (sLogPrefix + s);
      aActionList.add (SingleError.builderWarn ().errorText (s).build ());
    };
    final BiConsumer <String, Throwable> aLoggerError = (s, e) -> {
      LOGGER.error (sLogPrefix + s, e);
      aActionList.add (SingleError.builderError ().errorText (s).linkedException (e).build ());
    };

    // First read all service groups as they are dependents of the
    // business cards
    int nSGIndex = 0;
    for (final IMicroElement eServiceGroup : eRoot.getAllChildElements (CSMPExchange.ELEMENT_SERVICEGROUP))
    {
      // Read service group and service information
      final ISMPServiceGroup aServiceGroup;
      try
      {
        aServiceGroup = SMPServiceGroupMicroTypeConverter.convertToNative (eServiceGroup, x -> {
          IUser ret = aUserMgr.getUserOfID (x);
          if (ret == null)
          {
            // Select the default owner if an unknown user is contained
            ret = aDefaultOwner;
            LOGGER.warn ("Failed to resolve stored owner '" + x + "' - using default owner '" + aDefaultOwner.getID () + "'");
          }
          return ret;
        });
      }
      catch (final RuntimeException ex)
      {
        aLoggerError.accept ("Error parsing the service group at index " + nSGIndex + ". Ignoring this service group.", ex);
        continue;
      }
      final String sServiceGroupID = aServiceGroup.getID ();
      final boolean bIsServiceGroupContained = aAllServiceGroupIDs.contains (sServiceGroupID);
      if (!bIsServiceGroupContained || bOverwriteExisting)
      {
        if (aImportServiceGroups.containsKey (aServiceGroup))
        {
          aLoggerError.accept ("The service group " +
                               sServiceGroupID +
                               " (index " +
                               nSGIndex +
                               ") is already contained in the file. Will overwrite the previous definition.",
                               null);
        }

        // Remember to create/overwrite the service group
        final ServiceGroupImportData aSGInfo = new ServiceGroupImportData ();
        aImportServiceGroups.put (aServiceGroup, aSGInfo);
        if (bIsServiceGroupContained)
          aDeleteServiceGroups.add (aServiceGroup);
        aLoggerSuccess.accept ("Will " + (bIsServiceGroupContained ? "overwrite" : "import") + " service group " + sServiceGroupID);

        // read all contained service information
        {
          int nSICount = 0;
          for (final IMicroElement eServiceInfo : eServiceGroup.getAllChildElements (CSMPExchange.ELEMENT_SERVICEINFO))
          {
            final ISMPServiceInformation aServiceInfo = SMPServiceInformationMicroTypeConverter.convertToNative (eServiceInfo,
                                                                                                                 x -> aServiceGroup);
            aSGInfo.addServiceInfo (aServiceInfo);
            ++nSICount;
          }
          aLoggerInfo.accept ("Read " + nSICount + " service information of service group " + sServiceGroupID);
        }

        // read all contained redirects
        {
          int nRDCount = 0;
          for (final IMicroElement eRedirect : eServiceGroup.getAllChildElements (CSMPExchange.ELEMENT_REDIRECT))
          {
            final ISMPRedirect aRedirect = SMPRedirectMicroTypeConverter.convertToNative (eRedirect, x -> aServiceGroup);
            aSGInfo.addRedirect (aRedirect);
            ++nRDCount;
          }
          aLoggerInfo.accept ("Read " + nRDCount + " redirects of service group " + sServiceGroupID);
        }
      }
      else
      {
        aLoggerWarn.accept ("Ignoring already contained service group " + sServiceGroupID);
      }
      ++nSGIndex;
    }

    // Now read the business cards
    final ICommonsOrderedSet <ISMPBusinessCard> aImportBusinessCards = new CommonsLinkedHashSet <> ();
    final ICommonsList <ISMPBusinessCard> aDeleteBusinessCards = new CommonsArrayList <> ();
    if (aSettings.isDirectoryIntegrationEnabled ())
    {
      // Read them only if the Peppol Directory integration is enabled
      int nBCIndex = 0;
      for (final IMicroElement eBusinessCard : eRoot.getAllChildElements (CSMPExchange.ELEMENT_BUSINESSCARD))
      {
        // Read business card
        ISMPBusinessCard aBusinessCard = null;
        try
        {
          aBusinessCard = new SMPBusinessCardMicroTypeConverter ().convertToNative (eBusinessCard);
        }
        catch (final IllegalStateException ex)
        {
          // Service group not found
          aLoggerError.accept ("Business card at index " + nBCIndex + " contains an invalid/unknown service group!", null);
        }
        if (aBusinessCard == null)
        {
          aLoggerError.accept ("Failed to read business card at index " + nBCIndex, null);
        }
        else
        {
          final String sBusinessCardID = aBusinessCard.getID ();
          final boolean bIsBusinessCardContained = aAllBusinessCardIDs.contains (sBusinessCardID);
          if (!bIsBusinessCardContained || bOverwriteExisting)
          {
            if (aImportBusinessCards.removeIf (x -> x.getID ().equals (sBusinessCardID)))
            {
              aLoggerError.accept ("The business card for " +
                                   sBusinessCardID +
                                   " is already contained in the file. Will overwrite the previous definition.",
                                   null);
            }
            aImportBusinessCards.add (aBusinessCard);
            if (bIsBusinessCardContained)
              aDeleteBusinessCards.add (aBusinessCard);
            aLoggerSuccess.accept ("Will " + (bIsBusinessCardContained ? "overwrite" : "import") + " business card for " + sBusinessCardID);
          }
          else
          {
            aLoggerWarn.accept ("Ignoring already contained business card " + sBusinessCardID);
          }
        }
        ++nBCIndex;
      }
    }

    if (aImportServiceGroups.isEmpty () && aImportBusinessCards.isEmpty ())
    {
      if (aSettings.isDirectoryIntegrationEnabled ())
        aLoggerWarn.accept ("Found neither a service group nor a business card to import.");
      else
        aLoggerWarn.accept ("Found no service group to import.");
    }
    else
      if (aActionList.containsAny (IError::isError))
      {
        aLoggerError.accept ("Nothing will be imported because of the previous errors!", null);
      }
      else
      {
        // Start importing
        aLoggerInfo.accept ("Import is performed!");

        final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
        final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
        final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

        // 1. delete all existing service groups to be imported (if overwrite);
        // this may implicitly delete business cards
        final ICommonsSet <IParticipantIdentifier> aDeletedServiceGroups = new CommonsHashSet <> ();
        for (final ISMPServiceGroup aDeleteServiceGroup : aDeleteServiceGroups)
        {
          final IParticipantIdentifier aPI = aDeleteServiceGroup.getParticipantIdentifier ();
          try
          {
            if (aServiceGroupMgr.deleteSMPServiceGroup (aPI, true).isChanged ())
            {
              aLoggerSuccess.accept ("Successfully deleted service group " + aDeleteServiceGroup.getID ());
              aDeletedServiceGroups.add (aPI);
            }
            else
              aLoggerError.accept ("Failed to delete service group " + aDeleteServiceGroup.getID (), null);
          }
          catch (final SMPServerException ex)
          {
            aLoggerError.accept ("Failed to delete service group " + aDeleteServiceGroup.getID (), ex);
          }
        }

        // 2. create all service groups
        for (final Map.Entry <ISMPServiceGroup, ServiceGroupImportData> aEntry : aImportServiceGroups.entrySet ())
        {
          final ISMPServiceGroup aImportServiceGroup = aEntry.getKey ();
          ISMPServiceGroup aNewServiceGroup = null;
          try
          {
            aNewServiceGroup = aServiceGroupMgr.createSMPServiceGroup (aImportServiceGroup.getOwnerID (),
                                                                       aImportServiceGroup.getParticipantIdentifier (),
                                                                       aImportServiceGroup.getExtensionsAsString (),
                                                                       true);
            aLoggerSuccess.accept ("Successfully created service group " + aImportServiceGroup.getID ());
          }
          catch (final Exception ex)
          {
            // E.g. if SML connection failed
            aLoggerError.accept ("Error creating the new service group " + aImportServiceGroup.getID (), ex);

            // Delete Business Card again, if already present
            aImportBusinessCards.removeIf (x -> x.getID ().equals (aImportServiceGroup.getID ()));
          }
          if (aNewServiceGroup != null)
          {
            // 3a. create all endpoints
            for (final ISMPServiceInformation aImportServiceInfo : aEntry.getValue ().getServiceInfo ())
              try
              {
                if (aServiceInfoMgr.mergeSMPServiceInformation (aImportServiceInfo).isSuccess ())
                  aLoggerSuccess.accept ("Successfully created service information for " + aImportServiceGroup.getID ());
              }
              catch (final Exception ex)
              {
                aLoggerError.accept ("Error creating the new service information for " + aImportServiceGroup.getID (), ex);
              }

            // 3b. create all redirects
            for (final ISMPRedirect aImportRedirect : aEntry.getValue ().getRedirects ())
              try
              {
                if (aRedirectMgr.createOrUpdateSMPRedirect (aNewServiceGroup,
                                                            aImportRedirect.getDocumentTypeIdentifier (),
                                                            aImportRedirect.getTargetHref (),
                                                            aImportRedirect.getSubjectUniqueIdentifier (),
                                                            aImportRedirect.getCertificate (),
                                                            aImportRedirect.getExtensionsAsString ()) != null)
                  aLoggerSuccess.accept ("Successfully created redirect for " + aImportServiceGroup.getID ());
              }
              catch (final Exception ex)
              {
                aLoggerError.accept ("Error creating the new redirect for " + aImportServiceGroup.getID (), ex);
              }
          }
        }

        // 4. delete all existing business cards to be imported (if overwrite)
        // Note: if PD integration is disabled, the list is empty
        for (final ISMPBusinessCard aDeleteBusinessCard : aDeleteBusinessCards)
          try
          {
            if (aBusinessCardMgr.deleteSMPBusinessCard (aDeleteBusinessCard).isChanged ())
              aLoggerSuccess.accept ("Successfully deleted business card " + aDeleteBusinessCard.getID ());
            else
            {
              // If the service group to which the business card belongs was
              // already deleted, don't display an error, as the business card
              // was automatically deleted afterwards
              if (!aDeletedServiceGroups.contains (aDeleteBusinessCard.getParticipantIdentifier ()))
                aLoggerError.accept ("Failed to delete business card " + aDeleteBusinessCard.getID (), null);
            }
          }
          catch (final Exception ex)
          {
            aLoggerError.accept ("Failed to delete business card " + aDeleteBusinessCard.getID (), ex);
          }

        // 5. create all new business cards
        // Note: if PD integration is disabled, the list is empty
        for (final ISMPBusinessCard aImportBusinessCard : aImportBusinessCards)
          try
          {
            if (aBusinessCardMgr.createOrUpdateSMPBusinessCard (aImportBusinessCard.getParticipantIdentifier (),
                                                                aImportBusinessCard.getAllEntities ()) != null)
              aLoggerSuccess.accept ("Successfully created business card " + aImportBusinessCard.getID ());
          }
          catch (final Exception ex)
          {
            aLoggerError.accept ("Failed to create business card " + aImportBusinessCard.getID (), ex);
          }
      }
  }
}
