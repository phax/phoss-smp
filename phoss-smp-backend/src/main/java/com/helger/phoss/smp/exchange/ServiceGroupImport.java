/*
 * Copyright (C) 2015-2022 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.exchange;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.CommonsLinkedHashSet;
import com.helger.commons.collection.impl.ICommonsIterable;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.functional.ITriConsumer;
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
 * @since 5.6.0
 */
@Immutable
public final class ServiceGroupImport
{
  @NotThreadSafe
  private static final class InternalImportData
  {
    private final ICommonsList <ISMPServiceInformation> m_aServiceInfos = new CommonsArrayList <> ();
    private final ICommonsList <ISMPRedirect> m_aRedirects = new CommonsArrayList <> ();

    public void addServiceInfo (@Nonnull final ISMPServiceInformation aServiceInfo)
    {
      m_aServiceInfos.add (aServiceInfo);
    }

    @Nonnull
    public ICommonsIterable <ISMPServiceInformation> getServiceInfo ()
    {
      return m_aServiceInfos;
    }

    public void addRedirect (@Nonnull final ISMPRedirect aRedirect)
    {
      m_aRedirects.add (aRedirect);
    }

    @Nonnull
    public ICommonsIterable <ISMPRedirect> getRedirects ()
    {
      return m_aRedirects;
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceGroupImport.class);
  private static final AtomicInteger COUNTER = new AtomicInteger (0);

  private ServiceGroupImport ()
  {}

  public static void importXMLVer10 (@Nonnull final IMicroElement eRoot,
                                     final boolean bOverwriteExisting,
                                     @Nonnull final IUser aDefaultOwner,
                                     @Nonnull final ICommonsSet <String> aAllExistingServiceGroupIDs,
                                     @Nonnull final ICommonsSet <String> aAllExistingBusinessCardIDs,
                                     @Nonnull final ICommonsList <ImportActionItem> aActionList,
                                     @Nonnull final ImportSummary aSummary)
  {
    ValueEnforcer.notNull (eRoot, "Root");
    ValueEnforcer.notNull (aDefaultOwner, "DefaultOwner");
    ValueEnforcer.notNull (aAllExistingServiceGroupIDs, "AllExistingServiceGroupIDs");
    ValueEnforcer.notNull (aAllExistingBusinessCardIDs, "AllExistingBusinessCardIDs");
    ValueEnforcer.notNull (aActionList, "ActionList");
    ValueEnforcer.notNull (aSummary, "Summary");

    final String sLogPrefix = "[SG-IMPORT-" + COUNTER.incrementAndGet () + "] ";
    final BiConsumer <String, String> aLoggerSuccess = (pi, msg) -> {
      LOGGER.info (sLogPrefix + "[" + pi + "] " + msg);
      aActionList.add (ImportActionItem.createSuccess (pi, msg));
    };
    final BiConsumer <String, String> aLoggerInfo = (pi, msg) -> {
      LOGGER.info (sLogPrefix + (pi == null ? "" : "[" + pi + "] ") + msg);
      aActionList.add (ImportActionItem.createInfo (pi, msg));
    };
    final BiConsumer <String, String> aLoggerWarn = (pi, msg) -> {
      LOGGER.info (sLogPrefix + (pi == null ? "" : "[" + pi + "] ") + msg);
      aActionList.add (ImportActionItem.createWarning (pi, msg));
    };
    final Consumer <String> aLoggerError = msg -> {
      LOGGER.error (sLogPrefix + msg);
      aActionList.add (ImportActionItem.createError (null, msg, null));
    };
    final BiConsumer <String, Exception> aLoggerErrorEx = (msg, ex) -> {
      LOGGER.error (sLogPrefix + msg, ex);
      aActionList.add (ImportActionItem.createError (null, msg, ex));
    };
    final BiConsumer <String, String> aLoggerErrorPI = (pi, msg) -> {
      LOGGER.error (sLogPrefix + "[" + pi + "] " + msg);
      aActionList.add (ImportActionItem.createError (pi, msg, null));
    };
    final ITriConsumer <String, String, Exception> aLoggerErrorPIEx = (pi, msg, ex) -> {
      LOGGER.error (sLogPrefix + "[" + pi + "] " + msg, ex);
      aActionList.add (ImportActionItem.createError (pi, msg, ex));
    };

    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Starting import of Service Groups from XML v1.0, overwrite is " + (bOverwriteExisting ? "enabled" : "disabled"));

    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();

    final ICommonsOrderedMap <ISMPServiceGroup, InternalImportData> aImportServiceGroups = new CommonsLinkedHashMap <> ();
    final ICommonsMap <String, ISMPServiceGroup> aDeleteServiceGroups = new CommonsHashMap <> ();

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
          IUser aOwner = aUserMgr.getUserOfID (x);
          if (aOwner == null)
          {
            // Select the default owner if an unknown user is contained
            aOwner = aDefaultOwner;
            LOGGER.warn ("Failed to resolve stored owner '" + x + "' - using default owner '" + aDefaultOwner.getID () + "'");
          }
          // If the user is deleted, but existing - keep the deleted user
          return aOwner;
        });
      }
      catch (final RuntimeException ex)
      {
        aLoggerErrorEx.accept ("Error parsing the Service Group at index " + nSGIndex + ". Ignoring this Service Group.", ex);
        continue;
      }

      final String sServiceGroupID = aServiceGroup.getID ();
      final boolean bIsServiceGroupContained = aAllExistingServiceGroupIDs.contains (sServiceGroupID);
      if (!bIsServiceGroupContained || bOverwriteExisting)
      {
        if (aImportServiceGroups.containsKey (aServiceGroup))
        {
          aLoggerErrorPI.accept (sServiceGroupID,
                                 "The Service Group at index " +
                                                  nSGIndex +
                                                  " is already contained in the file. Will overwrite the previous definition.");
        }

        // Remember to create/overwrite the service group
        final InternalImportData aImportData = new InternalImportData ();
        aImportServiceGroups.put (aServiceGroup, aImportData);
        if (bIsServiceGroupContained)
          aDeleteServiceGroups.put (sServiceGroupID, aServiceGroup);
        aLoggerSuccess.accept (sServiceGroupID, "Will " + (bIsServiceGroupContained ? "overwrite" : "import") + " Service Group");

        // read all contained service information
        {
          int nSICount = 0;
          for (final IMicroElement eServiceInfo : eServiceGroup.getAllChildElements (CSMPExchange.ELEMENT_SERVICEINFO))
          {
            final ISMPServiceInformation aServiceInfo = SMPServiceInformationMicroTypeConverter.convertToNative (eServiceInfo,
                                                                                                                 x -> aServiceGroup);
            aImportData.addServiceInfo (aServiceInfo);
            ++nSICount;
          }
          aLoggerInfo.accept (sServiceGroupID,
                              "Read " +
                                               nSICount +
                                               " Service Information " +
                                               (nSICount == 1 ? "element" : "elements") +
                                               " of Service Group");
        }

        // read all contained redirects
        {
          int nRDCount = 0;
          for (final IMicroElement eRedirect : eServiceGroup.getAllChildElements (CSMPExchange.ELEMENT_REDIRECT))
          {
            final ISMPRedirect aRedirect = SMPRedirectMicroTypeConverter.convertToNative (eRedirect, x -> aServiceGroup);
            aImportData.addRedirect (aRedirect);
            ++nRDCount;
          }
          aLoggerInfo.accept (sServiceGroupID,
                              "Read " + nRDCount + " Redirect " + (nRDCount == 1 ? "element" : "elements") + " of Service Group");
        }
      }
      else
      {
        aLoggerWarn.accept (sServiceGroupID, "Ignoring already existing Service Group");
      }
      ++nSGIndex;
    }

    // Now read the business cards
    final ICommonsOrderedSet <ISMPBusinessCard> aImportBusinessCards = new CommonsLinkedHashSet <> ();
    final ICommonsMap <String, ISMPBusinessCard> aDeleteBusinessCards = new CommonsHashMap <> ();
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
        catch (final RuntimeException ex)
        {
          // Service group not found
          aLoggerError.accept ("Business Card at index " + nBCIndex + " contains an invalid/unknown Service Group!");
        }
        if (aBusinessCard == null)
        {
          aLoggerError.accept ("Failed to read Business Card at index " + nBCIndex);
        }
        else
        {
          final String sBusinessCardID = aBusinessCard.getID ();
          final boolean bIsBusinessCardContained = aAllExistingBusinessCardIDs.contains (sBusinessCardID);
          if (!bIsBusinessCardContained || bOverwriteExisting)
          {
            if (aImportBusinessCards.removeIf (x -> x.getID ().equals (sBusinessCardID)))
            {
              aLoggerErrorPI.accept (sBusinessCardID,
                                     "The Business Card already contained in the file. Will overwrite the previous definition.");
            }
            aImportBusinessCards.add (aBusinessCard);
            if (bIsBusinessCardContained)
            {
              // BCs are deleted when the SGs are deleted
              if (!aDeleteServiceGroups.containsKey (sBusinessCardID))
                aDeleteBusinessCards.put (sBusinessCardID, aBusinessCard);
            }
            aLoggerSuccess.accept (sBusinessCardID, "Will " + (bIsBusinessCardContained ? "overwrite" : "import") + " Business Card");
          }
          else
          {
            aLoggerWarn.accept (sBusinessCardID, "Ignoring already existing Business Card");
          }
        }
        ++nBCIndex;
      }
    }

    if (aImportServiceGroups.isEmpty () && aImportBusinessCards.isEmpty ())
    {
      aLoggerWarn.accept (null,
                          aSettings.isDirectoryIntegrationEnabled () ? "Found neither a Service Group nor a Business Card to import."
                                                                     : "Found no Service Group to import.");
    }
    else
      if (aActionList.containsAny (ImportActionItem::isError))
      {
        aLoggerError.accept ("Nothing will be imported because of the previous errors.");
      }
      else
      {
        // Start importing
        aLoggerInfo.accept (null, "Import is performed!");

        final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
        final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
        final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

        // 1. delete all existing service groups to be imported (if overwrite);
        // this may implicitly delete business cards
        final ICommonsSet <IParticipantIdentifier> aDeletedServiceGroups = new CommonsHashSet <> ();
        for (final Map.Entry <String, ISMPServiceGroup> aEntry : aDeleteServiceGroups.entrySet ())
        {
          final String sServiceGroupID = aEntry.getKey ();
          final ISMPServiceGroup aDeleteServiceGroup = aEntry.getValue ();
          final IParticipantIdentifier aPI = aDeleteServiceGroup.getParticipantIdentifier ();
          try
          {
            // Delete locally only
            if (aServiceGroupMgr.deleteSMPServiceGroup (aPI, false).isChanged ())
            {
              aLoggerSuccess.accept (sServiceGroupID, "Successfully deleted Service Group");
              aDeletedServiceGroups.add (aPI);
              aSummary.onSuccess (EImportSummaryAction.DELETE_SG);
            }
            else
            {
              aLoggerErrorPI.accept (sServiceGroupID, "Failed to delete Service Group");
              aSummary.onError (EImportSummaryAction.DELETE_SG);
            }
          }
          catch (final SMPServerException ex)
          {
            aLoggerErrorPIEx.accept (sServiceGroupID, "Failed to delete Service Group", ex);
            aSummary.onError (EImportSummaryAction.DELETE_SG);
          }
        }

        // 2. create all service groups
        for (final Map.Entry <ISMPServiceGroup, InternalImportData> aEntry : aImportServiceGroups.entrySet ())
        {
          final ISMPServiceGroup aImportServiceGroup = aEntry.getKey ();
          final String sServiceGroupID = aImportServiceGroup.getID ();

          ISMPServiceGroup aNewServiceGroup = null;
          try
          {
            final boolean bIsOverwrite = aDeleteServiceGroups.containsKey (sServiceGroupID);

            // Create in SML only for newly created entries
            aNewServiceGroup = aServiceGroupMgr.createSMPServiceGroup (aImportServiceGroup.getOwnerID (),
                                                                       aImportServiceGroup.getParticipantIdentifier (),
                                                                       aImportServiceGroup.getExtensionsAsString (),
                                                                       !bIsOverwrite);
            aLoggerSuccess.accept (sServiceGroupID, "Successfully created Service Group");
            aSummary.onSuccess (EImportSummaryAction.CREATE_SG);
          }
          catch (final Exception ex)
          {
            // E.g. if SML connection failed
            aLoggerErrorPIEx.accept (sServiceGroupID, "Error creating the new Service Group", ex);

            // Delete Business Card again, if already present
            aImportBusinessCards.removeIf (x -> x.getID ().equals (sServiceGroupID));
            aSummary.onError (EImportSummaryAction.CREATE_SG);
          }

          if (aNewServiceGroup != null)
          {
            // 3a. create all endpoints
            for (final ISMPServiceInformation aImportServiceInfo : aEntry.getValue ().getServiceInfo ())
            {
              try
              {
                if (aServiceInfoMgr.mergeSMPServiceInformation (aImportServiceInfo).isSuccess ())
                {
                  aLoggerSuccess.accept (sServiceGroupID, "Successfully created Service Information");
                  aSummary.onSuccess (EImportSummaryAction.CREATE_SI);
                }
                else
                {
                  aLoggerErrorPI.accept (sServiceGroupID, "Error creating the new Service Information");
                  aSummary.onError (EImportSummaryAction.CREATE_SI);
                }
              }
              catch (final Exception ex)
              {
                aLoggerErrorPIEx.accept (sServiceGroupID, "Error creating the new Service Information", ex);
                aSummary.onError (EImportSummaryAction.CREATE_SI);
              }
            }

            // 3b. create all redirects
            for (final ISMPRedirect aImportRedirect : aEntry.getValue ().getRedirects ())
            {
              try
              {
                if (aRedirectMgr.createOrUpdateSMPRedirect (aNewServiceGroup,
                                                            aImportRedirect.getDocumentTypeIdentifier (),
                                                            aImportRedirect.getTargetHref (),
                                                            aImportRedirect.getSubjectUniqueIdentifier (),
                                                            aImportRedirect.getCertificate (),
                                                            aImportRedirect.getExtensionsAsString ()) != null)
                {
                  aLoggerSuccess.accept (sServiceGroupID, "Successfully created Redirect");
                  aSummary.onSuccess (EImportSummaryAction.CREATE_REDIRECT);
                }
                else
                {
                  aLoggerErrorPI.accept (sServiceGroupID, "Error creating the new Redirect");
                  aSummary.onError (EImportSummaryAction.CREATE_REDIRECT);
                }
              }
              catch (final Exception ex)
              {
                aLoggerErrorPIEx.accept (sServiceGroupID, "Error creating the new Redirect", ex);
                aSummary.onError (EImportSummaryAction.CREATE_REDIRECT);
              }
            }
          }
        }

        // 4. delete all existing business cards to be imported (if overwrite)
        // Note: if PD integration is disabled, the list is empty
        for (final Map.Entry <String, ISMPBusinessCard> aEntry : aDeleteBusinessCards.entrySet ())
        {
          final String sServiceGroupID = aEntry.getKey ();
          final ISMPBusinessCard aDeleteBusinessCard = aEntry.getValue ();

          try
          {
            if (aBusinessCardMgr.deleteSMPBusinessCard (aDeleteBusinessCard).isChanged ())
            {
              aLoggerSuccess.accept (sServiceGroupID, "Successfully deleted Business Card");
              aSummary.onSuccess (EImportSummaryAction.DELETE_BC);
            }
            else
            {
              aSummary.onError (EImportSummaryAction.DELETE_BC);

              // If the service group to which the business card belongs was
              // already deleted, don't display an error, as the business card
              // was automatically deleted afterwards
              if (!aDeletedServiceGroups.contains (aDeleteBusinessCard.getParticipantIdentifier ()))
                aLoggerErrorPI.accept (sServiceGroupID, "Failed to delete Business Card");
            }
          }
          catch (final Exception ex)
          {
            aLoggerErrorPIEx.accept (sServiceGroupID, "Failed to delete Business Card", ex);
            aSummary.onError (EImportSummaryAction.DELETE_BC);
          }
        }

        // 5. create all new business cards
        // Note: if PD integration is disabled, the list is empty
        for (final ISMPBusinessCard aImportBusinessCard : aImportBusinessCards)
        {
          final String sBusinessCardID = aImportBusinessCard.getID ();

          try
          {
            if (aBusinessCardMgr.createOrUpdateSMPBusinessCard (aImportBusinessCard.getParticipantIdentifier (),
                                                                aImportBusinessCard.getAllEntities ()) != null)
            {
              aLoggerSuccess.accept (sBusinessCardID, "Successfully created Business Card");
              aSummary.onSuccess (EImportSummaryAction.CREATE_BC);
            }
            else
            {
              aLoggerErrorPI.accept (sBusinessCardID, "Failed to create Business Card");
              aSummary.onError (EImportSummaryAction.CREATE_BC);
            }
          }
          catch (final Exception ex)
          {
            aLoggerErrorPIEx.accept (sBusinessCardID, "Failed to create Business Card", ex);
            aSummary.onError (EImportSummaryAction.CREATE_BC);
          }
        }
      }
  }
}
