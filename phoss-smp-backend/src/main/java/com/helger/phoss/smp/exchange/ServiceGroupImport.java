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
package com.helger.phoss.smp.exchange;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.concurrent.ExecutorServiceHelper;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.timing.StopWatch;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsConcurrentHashMap;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsIterable;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.collection.commons.ICommonsSet;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.config.SMPConfigProvider;
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
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.serialize.MicroWriter;

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

    public void addServiceInfo (@NonNull final ISMPServiceInformation aServiceInfo)
    {
      m_aServiceInfos.add (aServiceInfo);
    }

    @NonNull
    public ICommonsIterable <ISMPServiceInformation> getServiceInfo ()
    {
      return m_aServiceInfos;
    }

    public void addRedirect (@NonNull final ISMPRedirect aRedirect)
    {
      m_aRedirects.add (aRedirect);
    }

    @NonNull
    public ICommonsIterable <ISMPRedirect> getRedirects ()
    {
      return m_aRedirects;
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceGroupImport.class);
  private static final AtomicInteger COUNTER = new AtomicInteger (0);

  private ServiceGroupImport ()
  {}

  private static void _analyzeXML (@NonNull final ImportLogger aImportLogger,
                                   @Nonnegative final int nImportThreadCount,
                                   @NonNull final IMicroElement eRoot,
                                   @NonNull final IUser aDefaultOwner,
                                   final boolean bOverwriteExisting,
                                   @NonNull final ICommonsSet <String> aAllExistingServiceGroupIDs,
                                   @NonNull final ICommonsSet <String> aAllExistingBusinessCardIDs,
                                   final boolean bDirectoryIntegrationEnabled,
                                   @NonNull final ICommonsOrderedMap <ISMPServiceGroup, InternalImportData> aServiceGroupsToImport,
                                   @NonNull final ICommonsMap <String, ISMPServiceGroup> aServiceGroupsToDelete,
                                   @NonNull final ICommonsOrderedMap <String, ISMPBusinessCard> aBusinessCardsToImport,
                                   @NonNull final ICommonsMap <String, ISMPBusinessCard> aBusinessCardsToDelete)
  {
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    final StopWatch aSW = StopWatch.createdStarted ();

    aImportLogger.info ("Starting analysis of source XML file in " + nImportThreadCount + " parallel threads");

    // Use a separate cache to avoid unnecessary amount of DB calls for users
    final ICommonsMap <String, IUser> aThreadSafeOwnerCache = new CommonsConcurrentHashMap <> ();
    final Function <String, IUser> aUserResolverViaMgr = sUserID -> {
      // This might be a DB access
      IUser aOwner = aUserMgr.getUserOfID (sUserID);
      if (aOwner == null)
      {
        // Select the default owner if an unknown user is contained
        aOwner = aDefaultOwner;
        LOGGER.warn ("Failed to resolve stored owner '" +
                     sUserID +
                     "' - using default owner '" +
                     aDefaultOwner.getID () +
                     "'");
      }
      // If the user is deleted, but existing - keep the deleted user
      return aOwner;
    };

    final ExecutorService aExecutorSvc = Executors.newFixedThreadPool (nImportThreadCount);

    // First read all service groups as they are dependents of the
    // business cards
    {
      // Thread-safe wrappers
      final Map <ISMPServiceGroup, InternalImportData> aThreadSafeServiceGroupsToImport = Collections.synchronizedMap (aServiceGroupsToImport);
      final Map <String, ISMPServiceGroup> aThreadSafeServiceGroupsToDelete = Collections.synchronizedMap (aServiceGroupsToDelete);

      final AtomicInteger aSGCount = new AtomicInteger (0);
      // Safe to run in parallelStream
      // 1. Read service group and service information
      eRoot.forAllChildElements (IMicroElement.filterName (CSMPExchange.ELEMENT_SERVICEGROUP),
                                 eServiceGroup -> aExecutorSvc.submit ( () -> {
                                   // Convert XML to domain object
                                   final ISMPServiceGroup aServiceGroup;
                                   try
                                   {
                                     aServiceGroup = SMPServiceGroupMicroTypeConverter.convertToNative (eServiceGroup,
                                                                                                        sUserID -> aThreadSafeOwnerCache.computeIfAbsent (sUserID,
                                                                                                                                                          aUserResolverViaMgr));
                                   }
                                   catch (final RuntimeException ex)
                                   {
                                     aImportLogger.error ("Error parsing Service Group - will ignore it. Source element:\n" +
                                                          MicroWriter.getNodeAsString (eServiceGroup),
                                                          ex);
                                     return;
                                   }

                                   final String sServiceGroupID = aServiceGroup.getID ();
                                   final boolean bIsServiceGroupContained = aAllExistingServiceGroupIDs.contains (sServiceGroupID);
                                   if (!bIsServiceGroupContained || bOverwriteExisting)
                                   {
                                     if (aThreadSafeServiceGroupsToImport.containsKey (aServiceGroup))
                                     {
                                       aImportLogger.error (sServiceGroupID,
                                                            "The Service Group with ID '" +
                                                                             sServiceGroupID +
                                                                             "' is already contained in the file. Will overwrite the previous definition.");
                                     }

                                     // Remember to create/overwrite the service group
                                     final InternalImportData aImportData = new InternalImportData ();
                                     aThreadSafeServiceGroupsToImport.put (aServiceGroup, aImportData);
                                     if (bIsServiceGroupContained)
                                       aThreadSafeServiceGroupsToDelete.put (sServiceGroupID, aServiceGroup);
                                     aImportLogger.success (sServiceGroupID,
                                                            "Will " +
                                                                             (bIsServiceGroupContained ? "overwrite"
                                                                                                       : "import") +
                                                                             " Service Group");

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
                                       aImportLogger.detail (sServiceGroupID,
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
                                         final ISMPRedirect aRedirect = SMPRedirectMicroTypeConverter.convertToNative (eRedirect,
                                                                                                                       x -> aServiceGroup);
                                         aImportData.addRedirect (aRedirect);
                                         ++nRDCount;
                                       }
                                       aImportLogger.detail (sServiceGroupID,
                                                             "Read " +
                                                                              nRDCount +
                                                                              " Redirect " +
                                                                              (nRDCount == 1 ? "element" : "elements") +
                                                                              " of Service Group");
                                     }
                                   }
                                   else
                                   {
                                     aImportLogger.warn (sServiceGroupID, "Ignoring already existing Service Group");
                                   }
                                   final int nCount = aSGCount.incrementAndGet ();
                                   if ((nCount % 1_000) == 0)
                                     LOGGER.info ("  Evaluated " + nCount + " Service Groups so far");
                                 }));
    }

    if (bDirectoryIntegrationEnabled)
    {
      // 2. Now read the business cards
      // Read them only if the Peppol Directory integration is enabled

      // Thread-safe wrappers
      final Map <String, ISMPBusinessCard> aThreadSafeBusinessCardsToImport = Collections.synchronizedMap (aBusinessCardsToImport);
      final Map <String, ISMPBusinessCard> aThreadSafeBusinessCardsToDelete = Collections.synchronizedMap (aBusinessCardsToDelete);

      final AtomicInteger aBCCount = new AtomicInteger (0);
      // Safe to run in parallelStream
      eRoot.forAllChildElements (IMicroElement.filterName (CSMPExchange.ELEMENT_BUSINESSCARD),
                                 eBusinessCard -> aExecutorSvc.submit ( () -> {
                                   // Read business card
                                   ISMPBusinessCard aBusinessCard = null;
                                   try
                                   {
                                     aBusinessCard = new SMPBusinessCardMicroTypeConverter ().convertToNative (eBusinessCard);
                                   }
                                   catch (final RuntimeException ex)
                                   {
                                     // Service group not found
                                     aImportLogger.error ("Business Card contains an invalid/unknown Service Group!",
                                                          ex);
                                   }

                                   if (aBusinessCard == null)
                                   {
                                     aImportLogger.error ("Failed to read Business Card. Source element:\n" +
                                                          MicroWriter.getNodeAsString (eBusinessCard));
                                   }
                                   else
                                   {
                                     final String sBusinessCardID = aBusinessCard.getID ();
                                     final boolean bIsBusinessCardContained = aAllExistingBusinessCardIDs.contains (sBusinessCardID);
                                     if (!bIsBusinessCardContained || bOverwriteExisting)
                                     {
                                       if (aThreadSafeBusinessCardsToImport.remove (sBusinessCardID) != null)
                                       {
                                         aImportLogger.error (sBusinessCardID,
                                                              "The Business Card already contained in the file. Will overwrite the previous definition.");
                                       }
                                       aThreadSafeBusinessCardsToImport.put (sBusinessCardID, aBusinessCard);
                                       if (bIsBusinessCardContained)
                                       {
                                         // BCs are deleted when the SGs are deleted
                                         aThreadSafeBusinessCardsToDelete.putIfAbsent (sBusinessCardID, aBusinessCard);
                                       }
                                       aImportLogger.success (sBusinessCardID,
                                                              "Will " +
                                                                               (bIsBusinessCardContained ? "overwrite"
                                                                                                         : "import") +
                                                                               " Business Card");
                                     }
                                     else
                                     {
                                       aImportLogger.warn (sBusinessCardID,
                                                           "Ignoring already existing Business Card '" +
                                                                            sBusinessCardID +
                                                                            "'");
                                     }
                                   }
                                   final int nCount = aBCCount.incrementAndGet ();
                                   if ((nCount % 1_000) == 0)
                                     LOGGER.info ("  Evaluated " + nCount + " Business Cards so far");
                                 }));
    }

    ExecutorServiceHelper.shutdownAndWaitUntilAllTasksAreFinished (aExecutorSvc);

    aSW.stop ();
    aImportLogger.info ("Finalized analysis of source XML file after " + aSW.getDuration ());
  }

  /**
   * Import Service Groups and Business Cards from V1.0 format
   * 
   * @param eRoot
   *        XML root element to read. May not be <code>null</code>.
   * @param bOverwriteExisting
   *        <code>true</code> to overwrite existing items, <code>false</code> to skip them
   * @param aDefaultOwner
   *        The default owner to be used, in case no user can be deduced from the uploaded file. May
   *        not be <code>null</code>.
   * @param aAllExistingServiceGroupIDs
   *        A read-only set with existing service group IDs. May not be <code>null</code>.
   * @param aAllExistingBusinessCardIDs
   *        A read-only set with existing service group IDs that have business cards. May not be
   *        <code>null</code>.
   * @param aActionList
   *        The action list to be filled. May not be <code>null</code>.
   * @param aSummary
   *        The import summary to be filled. May not be <code>null</code>.
   */
  public static void importXMLVer10 (@NonNull final IMicroElement eRoot,
                                     final boolean bOverwriteExisting,
                                     @NonNull final IUser aDefaultOwner,
                                     @NonNull final ICommonsSet <String> aAllExistingServiceGroupIDs,
                                     @NonNull final ICommonsSet <String> aAllExistingBusinessCardIDs,
                                     @NonNull final ICommonsList <ImportActionItem> aActionList,
                                     @NonNull final ImportSummary aSummary)
  {
    ValueEnforcer.notNull (eRoot, "Root");
    ValueEnforcer.notNull (aDefaultOwner, "DefaultOwner");
    ValueEnforcer.notNull (aAllExistingServiceGroupIDs, "AllExistingServiceGroupIDs");
    ValueEnforcer.notNull (aAllExistingBusinessCardIDs, "AllExistingBusinessCardIDs");
    ValueEnforcer.notNull (aActionList, "ActionList");
    ValueEnforcer.notNull (aSummary, "Summary");

    // Make 'em thread-safe
    final ImportLogger aImportLogger = new ImportLogger (aActionList, aSummary, COUNTER.incrementAndGet ());

    LOGGER.info ("Starting import of Service Groups from XML v1.0, overwrite is " +
                 (bOverwriteExisting ? "enabled" : "disabled"));

    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final boolean bDirectoryIntegrationEnabled = aSettings.isDirectoryIntegrationEnabled ();

    final ICommonsOrderedMap <ISMPServiceGroup, InternalImportData> aServiceGroupsToImport = new CommonsLinkedHashMap <> ();
    final ICommonsMap <String, ISMPServiceGroup> aServiceGroupsToDelete = new CommonsHashMap <> ();
    final ICommonsOrderedMap <String, ISMPBusinessCard> aBusinessCardsToImport = new CommonsLinkedHashMap <> ();
    final ICommonsMap <String, ISMPBusinessCard> aBusinessCardsToDelete = new CommonsHashMap <> ();

    // Minimum 1, default 8
    final int nImportThreadCount = Math.max (1,
                                             SMPConfigProvider.getConfig ().getAsInt ("smp.sgimport.threadcount", 8));

    _analyzeXML (aImportLogger,
                 nImportThreadCount,
                 eRoot,
                 aDefaultOwner,
                 bOverwriteExisting,
                 aAllExistingServiceGroupIDs,
                 aAllExistingBusinessCardIDs,
                 bDirectoryIntegrationEnabled,
                 aServiceGroupsToImport,
                 aServiceGroupsToDelete,
                 aBusinessCardsToImport,
                 aBusinessCardsToDelete);

    if (aServiceGroupsToImport.isEmpty () && aBusinessCardsToImport.isEmpty ())
    {
      aImportLogger.warn (bDirectoryIntegrationEnabled ? "Found neither a Service Group nor a Business Card to import."
                                                       : "Found no Service Group to import.");
    }
    else
      if (aImportLogger.containsAnyError ())
      {
        aImportLogger.error ("Nothing will be imported because of the previous errors.");
      }
      else
      {
        // Start importing
        final StopWatch aSW = StopWatch.createdStarted ();
        aImportLogger.info ("Import is now performed with " + nImportThreadCount + " parallel threads");

        final ExecutorService aExecutorSvc = Executors.newFixedThreadPool (nImportThreadCount);

        final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
        final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
        final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

        // 1. delete all existing service groups to be imported (if overwrite);
        // this may implicitly delete business cards
        final ICommonsSet <IParticipantIdentifier> aDeletedServiceGroups = new CommonsHashSet <> ();
        final Set <IParticipantIdentifier> aThreadSafeDeletedServiceGroups = Collections.synchronizedSet (aDeletedServiceGroups);

        aImportLogger.info ("Trying to delete " + aServiceGroupsToDelete.size () + " Service Groups");

        // This requires more sophisticated threading, as scopes are needed
        aServiceGroupsToDelete.entrySet ().forEach (aEntry -> aExecutorSvc.submit ( () -> {
          try (final WebScoped aWebScoped = new WebScoped ())
          {
            final String sServiceGroupID = aEntry.getKey ();
            final ISMPServiceGroup aDeleteServiceGroup = aEntry.getValue ();
            final IParticipantIdentifier aPI = aDeleteServiceGroup.getParticipantIdentifier ();
            try
            {
              // Delete locally only
              if (aServiceGroupMgr.deleteSMPServiceGroup (aPI, false).isChanged ())
              {
                aImportLogger.success (sServiceGroupID, "Successfully deleted Service Group");
                aThreadSafeDeletedServiceGroups.add (aPI);
                aImportLogger.onSuccess (EImportSummaryAction.DELETE_SG);
              }
              else
              {
                aImportLogger.error (sServiceGroupID, "Failed to delete Service Group");
                aImportLogger.onError (EImportSummaryAction.DELETE_SG);
              }
            }
            catch (final SMPServerException ex)
            {
              aImportLogger.error (sServiceGroupID, "Failed to delete Service Group", ex);
              aImportLogger.onError (EImportSummaryAction.DELETE_SG);
            }
          }
        }));

        // 2. create all service groups
        aImportLogger.info ("Trying to create " + aServiceGroupsToImport.size () + " Service Groups");
        final Map <String, ISMPBusinessCard> aThreadSafeBusinessCardsToImport = Collections.synchronizedMap (aBusinessCardsToImport);
        final AtomicInteger aSGCount = new AtomicInteger (0);
        aServiceGroupsToImport.entrySet ().forEach (aEntry -> aExecutorSvc.submit ( () -> {
          try (final WebScoped aWebScoped = new WebScoped ())
          {
            final ISMPServiceGroup aImportServiceGroup = aEntry.getKey ();
            final String sServiceGroupID = aImportServiceGroup.getID ();

            ISMPServiceGroup aNewServiceGroup = null;
            try
            {
              // Create in SML only for newly created entries
              // If the SG was deleted before, it was also only deleted locally and not in SML
              final boolean bCreateInSML = !aServiceGroupsToDelete.containsKey (sServiceGroupID);
              aNewServiceGroup = aServiceGroupMgr.createSMPServiceGroup (aImportServiceGroup.getOwnerID (),
                                                                         aImportServiceGroup.getParticipantIdentifier (),
                                                                         aImportServiceGroup.getExtensions ()
                                                                                            .getExtensionsAsJsonString (),
                                                                         bCreateInSML);
              aImportLogger.success (sServiceGroupID, "Successfully created Service Group");
              aImportLogger.onSuccess (EImportSummaryAction.CREATE_SG);
            }
            catch (final Exception ex)
            {
              // E.g. if SML connection failed
              aImportLogger.error (sServiceGroupID, "Error creating the new Service Group", ex);

              // Delete Business Card again, if already present
              aThreadSafeBusinessCardsToImport.remove (sServiceGroupID);
              aImportLogger.onError (EImportSummaryAction.CREATE_SG);
            }

            if (aNewServiceGroup != null)
            {
              // 3a. create all endpoints
              for (final ISMPServiceInformation aServiceInfoToImport : aEntry.getValue ().getServiceInfo ())
              {
                try
                {
                  if (aServiceInfoMgr.mergeSMPServiceInformation (aServiceInfoToImport).isSuccess ())
                  {
                    aImportLogger.success (sServiceGroupID, "Successfully created Service Information");
                    aImportLogger.onSuccess (EImportSummaryAction.CREATE_SI);
                  }
                  else
                  {
                    aImportLogger.error (sServiceGroupID, "Error creating the new Service Information");
                    aImportLogger.onError (EImportSummaryAction.CREATE_SI);
                  }
                }
                catch (final Exception ex)
                {
                  aImportLogger.error (sServiceGroupID, "Error creating the new Service Information", ex);
                  aImportLogger.onError (EImportSummaryAction.CREATE_SI);
                }
              }

              // 3b. create all redirects
              for (final ISMPRedirect aImportRedirect : aEntry.getValue ().getRedirects ())
              {
                try
                {
                  if (aRedirectMgr.createOrUpdateSMPRedirect (aNewServiceGroup.getParticipantIdentifier (),
                                                              aImportRedirect.getDocumentTypeIdentifier (),
                                                              aImportRedirect.getTargetHref (),
                                                              aImportRedirect.getSubjectUniqueIdentifier (),
                                                              aImportRedirect.getCertificate (),
                                                              aImportRedirect.getExtensions ()
                                                                             .getExtensionsAsJsonString ()) != null)
                  {
                    aImportLogger.success (sServiceGroupID, "Successfully created Redirect");
                    aImportLogger.onSuccess (EImportSummaryAction.CREATE_REDIRECT);
                  }
                  else
                  {
                    aImportLogger.success (sServiceGroupID, "Error creating the new Redirect");
                    aImportLogger.onError (EImportSummaryAction.CREATE_REDIRECT);
                  }
                }
                catch (final Exception ex)
                {
                  aImportLogger.error (sServiceGroupID, "Error creating the new Redirect", ex);
                  aImportLogger.onError (EImportSummaryAction.CREATE_REDIRECT);
                }
              }
            }
            final int nCount = aSGCount.incrementAndGet ();
            if ((nCount % 1_000) == 0)
              LOGGER.info ("  Imported " + nCount + " Service Groups so far");
          }
        }));

        if (bDirectoryIntegrationEnabled)
        {
          // 4. delete all existing business cards to be imported (if overwrite)
          // Note: if PD integration is disabled, the list is empty
          aImportLogger.info ("Trying to delete " + aBusinessCardsToDelete.size () + " Business Cards");
          aBusinessCardsToDelete.entrySet ().forEach (aEntry -> aExecutorSvc.submit ( () -> {
            try (final WebScoped aWebScoped = new WebScoped ())
            {
              final String sServiceGroupID = aEntry.getKey ();
              final ISMPBusinessCard aDeleteBusinessCard = aEntry.getValue ();

              try
              {
                // No need to sync to the directory, because the update comes later anyway
                if (aBusinessCardMgr.deleteSMPBusinessCard (aDeleteBusinessCard, false).isChanged ())
                {
                  aImportLogger.success (sServiceGroupID, "Successfully deleted Business Card");
                  aImportLogger.onSuccess (EImportSummaryAction.DELETE_BC);
                }
                else
                {
                  // If the service group to which the business card belongs was
                  // already deleted, don't display an error, as the business card
                  // was automatically deleted afterwards
                  if (!aThreadSafeDeletedServiceGroups.contains (aDeleteBusinessCard.getParticipantIdentifier ()))
                  {
                    aImportLogger.error (sServiceGroupID, "Failed to delete Business Card");
                    aImportLogger.onError (EImportSummaryAction.DELETE_BC);
                  }
                }
              }
              catch (final Exception ex)
              {
                aImportLogger.error (sServiceGroupID, "Failed to delete Business Card", ex);
                aImportLogger.onError (EImportSummaryAction.DELETE_BC);
              }
            }
          }));

          // 5. create all new business cards
          // Note: if PD integration is disabled, the list is empty
          aImportLogger.info ("Trying to create " + aBusinessCardsToImport.size () + " Business Cards");
          final AtomicInteger aBCCount = new AtomicInteger (0);
          aBusinessCardsToImport.values ().forEach (aImportBusinessCard -> aExecutorSvc.submit ( () -> {
            try (final WebScoped aWebScoped = new WebScoped ())
            {
              final String sBusinessCardID = aImportBusinessCard.getID ();

              try
              {
                // Always sync to the Directory after the creation
                if (aBusinessCardMgr.createOrUpdateSMPBusinessCard (aImportBusinessCard.getParticipantIdentifier (),
                                                                    aImportBusinessCard.getAllEntities (),
                                                                    true) != null)
                {
                  aImportLogger.success (sBusinessCardID, "Successfully created Business Card");
                  aImportLogger.onSuccess (EImportSummaryAction.CREATE_BC);
                }
                else
                {
                  aImportLogger.error (sBusinessCardID, "Failed to create Business Card");
                  aImportLogger.onError (EImportSummaryAction.CREATE_BC);
                }
              }
              catch (final Exception ex)
              {
                aImportLogger.error (sBusinessCardID, "Failed to create Business Card", ex);
                aImportLogger.onError (EImportSummaryAction.CREATE_BC);
              }

              final int nCount = aBCCount.incrementAndGet ();
              if ((nCount % 1_000) == 0)
                LOGGER.info ("  Imported " + nCount + " Business Groups so far");
            }
          }));
        }
        ExecutorServiceHelper.shutdownAndWaitUntilAllTasksAreFinished (aExecutorSvc);
        aSW.stop ();
        aImportLogger.info ("Import is finalized after " + aSW.getDuration ());
      }
  }
}
