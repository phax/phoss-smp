/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.mgr;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.misc.ContainsSoftMigration;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.numeric.mutable.MutableBoolean;
import com.helger.base.state.EChange;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.commons.ICommonsSet;
import com.helger.collection.paging.IPagingSpec;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.backend.mongodb.SMPMongoQueryHelper;
import com.helger.phoss.smp.domain.serviceinfo.ESMPServiceInformationColumn;
import com.helger.phoss.smp.domain.serviceinfo.EndpointUsageInfo;
import com.helger.phoss.smp.domain.serviceinfo.IEndpointUsageInfo;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpointHelper;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.security.SMPCertificateHelper;
import com.helger.photon.audit.AuditHelper;
import com.helger.typeconvert.impl.TypeConverter;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;

/**
 * Manager for all {@link SMPServiceInformation} objects.
 *
 * @author Philip Helger
 */
public final class SMPServiceInformationManagerMongoDB extends AbstractManagerMongoDB implements
                                                       ISMPServiceInformationManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPServiceInformationManagerMongoDB.class);

  private static final String BSON_ID = "id";
  private static final ESMPServiceInformationColumn [] COLUMNS = ESMPServiceInformationColumn.values ();
  private static final String BSON_SERVICE_GROUP_ID = "sgid";
  private static final String BSON_DOCTYPE_ID = "doctypeid";
  private static final String BSON_PROCESSES = "processes";
  private static final String BSON_PROCESS_ID = "processid";
  private static final String BSON_ENDPOINTS = "endpoints";
  private static final String BSON_EXTENSIONS = "extensions";
  private static final String BSON_ENDPOINT_ID = "endpointid";
  private static final String BSON_TRANSPORT_PROFILE = "transportprofile";
  private static final String BSON_ENDPOINT_REFERENCE = "endpointreference";
  private static final String BSON_ACCESS_POINT_ID = "apid";
  private static final String BSON_BUSINESSLEVELSIG = "businesslevelsig";
  private static final String BSON_MINIMUM_AUTHENTICATION_LEVEL = "minauth";
  private static final String BSON_SERVICEACTIVATION = "serviceactivation";
  private static final String BSON_SERVICEEXPIRATION = "serviceexpiration";
  private static final String BSON_CERTIFICATE = "certificate";
  private static final String BSON_SERVICE_DESCRIPTION = "servicedesc";
  private static final String BSON_TECHCONTACTURL = "techcontacturl";
  private static final String BSON_TECHINFOURL = "techinfourl";
  private static final String BSON_COUNT = "count";

  private static final String BSON_ENDPOINTS_PATH = BSON_PROCESSES + "." + BSON_ENDPOINTS;
  private static final String BSON_ENDPOINT_REFERENCE_PATH = BSON_ENDPOINTS_PATH + "." + BSON_ENDPOINT_REFERENCE;
  private static final String BSON_ACCESS_POINT_ID_PATH = BSON_ENDPOINTS_PATH + "." + BSON_ACCESS_POINT_ID;
  private static final String BSON_TRANSPORT_PROFILE_PATH = BSON_ENDPOINTS_PATH + "." + BSON_TRANSPORT_PROFILE;

  private final IIdentifierFactory m_aIdentifierFactory;
  private final ISMPAccessPointManager m_aAccessPointMgr;
  private final CallbackList <ISMPServiceInformationCallback> m_aCBs = new CallbackList <> ();

  public SMPServiceInformationManagerMongoDB (@NonNull final IIdentifierFactory aIdentifierFactory,
                                              @NonNull final ISMPAccessPointManager aAccessPointMgr)
  {
    super ("smp-serviceinfo");
    ValueEnforcer.notNull (aAccessPointMgr, "AccessPointMgr");
    m_aIdentifierFactory = aIdentifierFactory;
    m_aAccessPointMgr = aAccessPointMgr;
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
    getCollection ().createIndex (Indexes.ascending (BSON_SERVICE_GROUP_ID));
    getCollection ().createIndex (Indexes.ascending (BSON_ENDPOINT_REFERENCE_PATH));
    getCollection ().createIndex (Indexes.ascending (BSON_ACCESS_POINT_ID_PATH));
    getCollection ().createIndex (Indexes.ascending (BSON_TRANSPORT_PROFILE_PATH));
  }

  @NonNull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceInformationCallback> serviceInformationCallbacks ()
  {
    return m_aCBs;
  }

  @NonNull
  @ReturnsMutableCopy
  public static Document toBson (@NonNull final ISMPEndpoint aValue)
  {
    final Document ret = new Document ().append (BSON_ENDPOINT_ID, aValue.getID ())
                                        .append (BSON_TRANSPORT_PROFILE, aValue.getTransportProfile ())
                                        .append (BSON_ACCESS_POINT_ID, aValue.getAccessPointID ());
    ret.append (BSON_BUSINESSLEVELSIG, Boolean.valueOf (aValue.isRequireBusinessLevelSignature ()));
    if (aValue.hasMinimumAuthenticationLevel ())
      ret.append (BSON_MINIMUM_AUTHENTICATION_LEVEL, aValue.getMinimumAuthenticationLevel ());
    if (aValue.hasServiceActivationDateTime ())
      ret.append (BSON_SERVICEACTIVATION, TypeConverter.convert (aValue.getServiceActivationDateTime (), Date.class));
    if (aValue.hasServiceExpirationDateTime ())
      ret.append (BSON_SERVICEEXPIRATION, TypeConverter.convert (aValue.getServiceExpirationDateTime (), Date.class));
    if (aValue.hasServiceDescription ())
      ret.append (BSON_SERVICE_DESCRIPTION, aValue.getServiceDescription ());
    if (aValue.hasTechnicalContactUrl ())
      ret.append (BSON_TECHCONTACTURL, aValue.getTechnicalContactUrl ());
    if (aValue.hasTechnicalInformationUrl ())
      ret.append (BSON_TECHINFOURL, aValue.getTechnicalInformationUrl ());
    if (aValue.getExtensions ().extensions ().isNotEmpty ())
      ret.append (BSON_EXTENSIONS, aValue.getExtensions ().getExtensionsAsJsonString ());
    return ret;
  }

  @NonNull
  @ContainsSoftMigration
  @ReturnsMutableCopy
  public static SMPEndpoint toEndpoint (@NonNull final Document aDoc, @NonNull final MutableBoolean aChange)
  {
    // Migration: generate UUID if endpoint ID is missing
    String sEndpointID = aDoc.getString (BSON_ENDPOINT_ID);
    if (sEndpointID == null)
    {
      sEndpointID = SMPEndpointHelper.createUniqueEndpointID ();
      aChange.set (true);
    }
    final String sTransportProfile = aDoc.getString (BSON_TRANSPORT_PROFILE);
    final boolean bRequireBusinessLevelSignature = aDoc.getBoolean (BSON_BUSINESSLEVELSIG,
                                                                    SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE);
    final String sMinimumAuthenticationLevel = aDoc.getString (BSON_MINIMUM_AUTHENTICATION_LEVEL);
    final XMLOffsetDateTime aServiceActivationDT = TypeConverter.convert (aDoc.getDate (BSON_SERVICEACTIVATION),
                                                                          XMLOffsetDateTime.class);
    final XMLOffsetDateTime aServiceExpirationDT = TypeConverter.convert (aDoc.getDate (BSON_SERVICEEXPIRATION),
                                                                          XMLOffsetDateTime.class);
    final String sServiceDescription = aDoc.getString (BSON_SERVICE_DESCRIPTION);
    final String sTechnicalContactUrl = aDoc.getString (BSON_TECHCONTACTURL);
    final String sTechnicalInformationUrl = aDoc.getString (BSON_TECHINFOURL);
    final String sExtension = aDoc.getString (BSON_EXTENSIONS);

    // Resolve the Access Point
    final ISMPAccessPointManager aAccessPointMgr = SMPMetaManager.getAccessPointMgr ();
    ISMPAccessPoint aAccessPoint = null;
    final String sAccessPointID = aDoc.getString (BSON_ACCESS_POINT_ID);
    if (StringHelper.isNotEmpty (sAccessPointID))
    {
      aAccessPoint = aAccessPointMgr.getAccessPointOfID (sAccessPointID);
      if (aAccessPoint == null)
        LOGGER.warn ("Failed to resolve Access Point with ID '" + sAccessPointID + "' of endpoint '" + sEndpointID + "'");
    }
    if (aAccessPoint == null)
    {
      // Migration: the URL and the certificate were stored inline
      aAccessPoint = aAccessPointMgr.getOrCreateAccessPoint (aDoc.getString (BSON_ENDPOINT_REFERENCE),
                                                             aDoc.getString (BSON_CERTIFICATE));
      aChange.set (true);
    }

    return new SMPEndpoint (sEndpointID,
                            sTransportProfile,
                            aAccessPoint,
                            bRequireBusinessLevelSignature,
                            sMinimumAuthenticationLevel,
                            aServiceActivationDT,
                            aServiceExpirationDT,
                            sServiceDescription,
                            sTechnicalContactUrl,
                            sTechnicalInformationUrl,
                            sExtension);
  }

  @NonNull
  @ReturnsMutableCopy
  public static Document toBson (@NonNull final ISMPProcess aValue)
  {
    final Document ret = new Document ().append (BSON_PROCESS_ID, toBson (aValue.getProcessIdentifier ()));
    final ICommonsList <Document> aEndpoints = new CommonsArrayList <> (aValue.getAllEndpoints (),
                                                                        SMPServiceInformationManagerMongoDB::toBson);
    if (aEndpoints.isNotEmpty ())
      ret.append (BSON_ENDPOINTS, aEndpoints);
    if (aValue.getExtensions ().extensions ().isNotEmpty ())
      ret.append (BSON_EXTENSIONS, aValue.getExtensions ().getExtensionsAsJsonString ());
    return ret;
  }

  @Nullable
  @ReturnsMutableCopy
  public static SMPProcess toProcess (@NonNull final Document aDoc, @NonNull final MutableBoolean aChange)
  {
    final IProcessIdentifier aProcessID = toProcessID ((Document) aDoc.get (BSON_PROCESS_ID));
    final List <Document> aEndpointDocs = aDoc.getList (BSON_ENDPOINTS, Document.class);
    if (aEndpointDocs == null)
      return null;

    final ICommonsList <SMPEndpoint> aEndpoints = new CommonsArrayList <> ();
    for (final Document aDocEP : aEndpointDocs)
      aEndpoints.add (toEndpoint (aDocEP, aChange));
    final String sExtension = aDoc.getString (BSON_EXTENSIONS);
    return new SMPProcess (aProcessID, aEndpoints, sExtension);
  }

  @NonNull
  @ReturnsMutableCopy
  public static Document toBson (@NonNull final ISMPServiceInformation aValue)
  {
    final Document ret = new Document ().append (BSON_ID, aValue.getID ())
                                        .append (BSON_SERVICE_GROUP_ID, aValue.getServiceGroupID ())
                                        .append (BSON_DOCTYPE_ID, toBson (aValue.getDocumentTypeIdentifier ()));
    final ICommonsList <Document> aProcs = new CommonsArrayList <> (aValue.getAllProcesses (),
                                                                    SMPServiceInformationManagerMongoDB::toBson);
    if (aProcs.isNotEmpty ())
      ret.append (BSON_PROCESSES, aProcs);
    if (aValue.getExtensions ().extensions ().isNotEmpty ())
      ret.append (BSON_EXTENSIONS, aValue.getExtensions ().getExtensionsAsJsonString ());
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public SMPServiceInformation toServiceInformation (@NonNull final Document aDoc, final boolean bNeedProcesses)
  {
    final MutableBoolean aChange = new MutableBoolean (false);
    final IParticipantIdentifier aParticipantID = m_aIdentifierFactory.parseParticipantIdentifier (aDoc.getString (BSON_SERVICE_GROUP_ID));
    final IDocumentTypeIdentifier aDocTypeID = toDocumentTypeID (aDoc.get (BSON_DOCTYPE_ID, Document.class));
    final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList <> ();
    if (bNeedProcesses)
    {
      for (final Document aDocP : aDoc.getList (BSON_PROCESSES, Document.class))
      {
        final SMPProcess aProcess = toProcess (aDocP, aChange);
        if (aProcess != null)
          aProcesses.add (aProcess);
      }

    }
    final String sExtension = aDoc.getString (BSON_EXTENSIONS);

    // The ID itself is derived from ServiceGroupID and DocTypeID
    final var ret = new SMPServiceInformation (aParticipantID, aDocTypeID, aProcesses, sExtension);
    if (aChange.booleanValue ())
    {
      // Store back (since 8.1.7)
      getCollection ().replaceOne (new Document (BSON_ID, ret.getID ()), toBson (ret));
    }
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPEndpoint> getAllSMPEndpoints (@Nullable final IParticipantIdentifier aParticipantID,
                                                         @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                                         @Nullable final IProcessIdentifier aProcessID,
                                                         @Nullable final String sTransportProfileID)
  {
    final ICommonsList <ISMPEndpoint> ret = new CommonsArrayList <> ();
    final ISMPServiceInformation aServiceInfo = getSMPServiceInformationOfServiceGroupAndDocumentType (aParticipantID,
                                                                                                       aDocTypeID);
    if (aServiceInfo != null)
    {
      final ISMPProcess aProcess = aServiceInfo.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        ret.addAll (aProcess.getAllEndpointsOfTransportProfile (sTransportProfileID));
      }
    }
    return ret;
  }

  @NonNull
  public ESuccess mergeSMPServiceInformation (@NonNull final ISMPServiceInformation aSMPServiceInformationObj)
  {
    final SMPServiceInformation aSMPServiceInformation = (SMPServiceInformation) aSMPServiceInformationObj;
    ValueEnforcer.notNull (aSMPServiceInformation, "ServiceInformation");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("mergeSMPServiceInformation (" + aSMPServiceInformationObj + ")");

    // Resolve and de-duplicate the Access Points of all endpoints
    SMPEndpointHelper.resolveAccessPoints (m_aAccessPointMgr, aSMPServiceInformation);

    // Check for an update
    boolean bChangedExisting = false;
    final ISMPServiceInformation aOldInformation = getSMPServiceInformationOfServiceGroupAndDocumentType (aSMPServiceInformation.getServiceGroupParticipantIdentifier (),
                                                                                                          aSMPServiceInformation.getDocumentTypeIdentifier ());
    if (aOldInformation != null)
    {
      // If a service information is present, it must be the provided object!
      // This is not true for the REST API
      if (EqualsHelper.identityEqual (aOldInformation, aSMPServiceInformation))
        bChangedExisting = true;
    }

    if (bChangedExisting)
    {
      // Edit existing
      getCollection ().replaceOne (new Document (BSON_ID, aOldInformation.getID ()), toBson (aSMPServiceInformation));

      AuditHelper.onAuditModifySuccess (SMPServiceInformation.OT,
                                        "set-all",
                                        aOldInformation.getID (),
                                        aOldInformation.getServiceGroupID (),
                                        aOldInformation.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aOldInformation.getAllProcesses (),
                                        aOldInformation.getExtensions ().getExtensionsAsJsonString ());

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("mergeSMPServiceInformation - success - updated");

      m_aCBs.forEach (x -> x.onSMPServiceInformationUpdated (aSMPServiceInformation));
    }
    else
    {
      // (Optionally delete the old one and) create the new one
      boolean bRemovedOld = false;
      if (aOldInformation != null)
      {
        // Delete only if present
        final DeleteResult aDR = getCollection ().deleteOne (new Document (BSON_ID, aOldInformation.getID ()));
        bRemovedOld = aDR.wasAcknowledged () && aDR.getDeletedCount () > 0;
      }

      if (!getCollection ().insertOne (toBson (aSMPServiceInformation)).wasAcknowledged ())
        throw new IllegalStateException ("Failed to insert into MongoDB Collection");

      if (bRemovedOld)
      {
        AuditHelper.onAuditDeleteSuccess (SMPServiceInformation.OT,
                                          aOldInformation.getID (),
                                          aOldInformation.getServiceGroupID (),
                                          aOldInformation.getDocumentTypeIdentifier ().getURIEncoded ());
      }
      else
        if (aOldInformation != null)
        {
          AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT,
                                            aOldInformation.getID (),
                                            aOldInformation.getServiceGroupID (),
                                            aOldInformation.getDocumentTypeIdentifier ().getURIEncoded ());
        }

      AuditHelper.onAuditCreateSuccess (SMPServiceInformation.OT,
                                        aSMPServiceInformation.getID (),
                                        aSMPServiceInformation.getServiceGroupID (),
                                        aSMPServiceInformation.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aSMPServiceInformation.getAllProcesses (),
                                        aSMPServiceInformation.getExtensions ().getExtensionsAsJsonString ());
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("mergeSMPServiceInformation - success - created");

      if (aOldInformation != null)
        m_aCBs.forEach (x -> x.onSMPServiceInformationUpdated (aSMPServiceInformation));
      else
        m_aCBs.forEach (x -> x.onSMPServiceInformationCreated (aSMPServiceInformation));
    }
    return ESuccess.SUCCESS;
  }

  @NonNull
  public EChange deleteSMPServiceInformation (@Nullable final ISMPServiceInformation aSMPServiceInformation)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceInformation (" + aSMPServiceInformation + ")");

    if (aSMPServiceInformation == null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPServiceInformation - failure");
      return EChange.UNCHANGED;
    }

    final DeleteResult aDR = getCollection ().deleteOne (new Document (BSON_ID, aSMPServiceInformation.getID ()));
    if (!aDR.wasAcknowledged () || aDR.getDeletedCount () == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, aSMPServiceInformation.getID (), "no-such-id");
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPServiceInformation - failure");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (SMPServiceInformation.OT, aSMPServiceInformation.getID ());
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceInformation - success");

    m_aCBs.forEach (x -> x.onSMPServiceInformationDeleted (aSMPServiceInformation));

    return EChange.CHANGED;
  }

  @NonNull
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    EChange eChange = EChange.UNCHANGED;
    for (final ISMPServiceInformation aSMPServiceInformation : getAllSMPServiceInformationOfServiceGroup (aParticipantIdentifier))
      eChange = eChange.or (deleteSMPServiceInformation (aSMPServiceInformation));
    return eChange;
  }

  @NonNull
  public EChange deleteSMPProcess (@Nullable final ISMPServiceInformation aSMPServiceInformation,
                                   @Nullable final ISMPProcess aProcess)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPProcess (" + aSMPServiceInformation + ", " + aProcess + ")");

    if (aSMPServiceInformation == null || aProcess == null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPProcess - failure");
      return EChange.UNCHANGED;
    }

    // Find implementation object
    final SMPServiceInformation aRealServiceInformation = getCollection ().find (new Document (BSON_ID,
                                                                                               aSMPServiceInformation.getID ()))
                                                                          .map (x -> toServiceInformation (x, true))
                                                                          .first ();
    if (aRealServiceInformation == null)
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, aSMPServiceInformation.getID (), "no-such-id");
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPProcess - failure");
      return EChange.UNCHANGED;
    }

    // Main deletion in write lock
    if (aRealServiceInformation.deleteProcess (aProcess.getProcessIdentifier ()).isUnchanged ())
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT,
                                        aSMPServiceInformation.getID (),
                                        aProcess.getProcessIdentifier ().getURIEncoded (),
                                        "no-such-process");
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPProcess - failure");
      return EChange.UNCHANGED;
    }

    // Save new one
    getCollection ().replaceOne (new Document (BSON_ID, aSMPServiceInformation.getID ()),
                                 toBson (aRealServiceInformation));

    AuditHelper.onAuditDeleteSuccess (SMPServiceInformation.OT,
                                      aSMPServiceInformation.getID (),
                                      aProcess.getProcessIdentifier ().getURIEncoded ());
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPProcess - success");
    return EChange.CHANGED;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation ()
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    forEachSMPServiceInformation (ret::add);
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  @Override
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation (@NonNull final IPagingSpec aPagingSpec,
                                                                            @Nullable final String sSearchText)
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    if (aPagingSpec.isEmptyPage ())
      return ret;

    final Bson aFilter = SMPMongoQueryHelper.createSearchFilter (COLUMNS, sSearchText);
    final FindIterable <Document> aCursor = aFilter == null ? getCollection ().find ()
                                                            : getCollection ().find (aFilter);
    aCursor.sort (SMPMongoQueryHelper.createSort (COLUMNS, aPagingSpec));
    if (aPagingSpec.getStartIndex () > 0)
      aCursor.skip ((int) Math.min (aPagingSpec.getStartIndex (), Integer.MAX_VALUE));
    if (!aPagingSpec.isUnlimited ())
      aCursor.limit ((int) Math.min (aPagingSpec.getMaxCount (), Integer.MAX_VALUE));
    aCursor.forEach (x -> ret.add (toServiceInformation (x, true)));
    return ret;
  }

  @Override
  public long getSMPServiceInformationCount (@Nullable final String sSearchText)
  {
    final Bson aFilter = SMPMongoQueryHelper.createSearchFilter (COLUMNS, sSearchText);
    return aFilter == null ? getSMPServiceInformationCount () : getCollection ().countDocuments (aFilter);
  }

  public void forEachSMPServiceInformation (@NonNull final Consumer <? super ISMPServiceInformation> aConsumer)
  {
    getCollection ().find ().forEach (x -> aConsumer.accept (toServiceInformation (x, true)));
  }

  @Nonnegative
  public long getSMPServiceInformationCount ()
  {
    return getCollection ().countDocuments ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformationOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    if (aParticipantIdentifier != null)
    {
      getCollection ().find (new Document (BSON_SERVICE_GROUP_ID, aParticipantIdentifier.getURIEncoded ()))
                      .forEach ((Consumer <Document>) x -> ret.add (toServiceInformation (x, true)));
    }
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    final ICommonsList <IDocumentTypeIdentifier> ret = new CommonsArrayList <> ();
    if (aParticipantIdentifier != null)
    {
      getCollection ().find (new Document (BSON_SERVICE_GROUP_ID, aParticipantIdentifier.getURIEncoded ()))
                      .forEach ((Consumer <Document>) x -> ret.add (toServiceInformation (x, false)
                                                                                                   .getDocumentTypeIdentifier ()));
    }
    return ret;
  }

  @Nullable
  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final IParticipantIdentifier aParticipantIdentifier,
                                                                                       @Nullable final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    if (aParticipantIdentifier == null)
      return null;
    if (aDocumentTypeIdentifier == null)
      return null;

    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    getCollection ().find (Filters.and (new Document (BSON_SERVICE_GROUP_ID, aParticipantIdentifier.getURIEncoded ()),
                                        new Document (BSON_DOCTYPE_ID, toBson (aDocumentTypeIdentifier))))
                    .forEach (x -> ret.add (toServiceInformation (x, true)));

    if (ret.isEmpty ())
      return null;
    if (ret.size () > 1)
      LOGGER.warn ("Found more than one entry for service group '" +
                   aParticipantIdentifier.getURIEncoded () +
                   "' and document type '" +
                   aDocumentTypeIdentifier.getURIEncoded () +
                   "'. This seems to be a bug! Using the first one.");
    return ret.getFirstOrNull ();
  }

  /**
   * Create an aggregation pipeline that unwinds all endpoints of all documents, so that the
   * provided final stage sees one document per endpoint.
   *
   * @param aFinalStage
   *        The last stage of the pipeline. May not be <code>null</code>.
   * @return The pipeline stages. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  private static ICommonsList <Bson> _createUnwindEndpointsPipeline (@NonNull final Bson aFinalStage)
  {
    return new CommonsArrayList <> (Aggregates.unwind ("$" + BSON_PROCESSES),
                                    Aggregates.unwind ("$" + BSON_ENDPOINTS_PATH),
                                    aFinalStage);
  }

  /**
   * Create the aggregation pipeline that counts all endpoints grouped by the provided group ID
   * expression.
   *
   * @param aGroupID
   *        The <code>_id</code> expression of the <code>$group</code> stage. May not be
   *        <code>null</code>.
   * @return The pipeline stages. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  private static ICommonsList <Bson> _createEndpointGroupPipeline (@NonNull final Document aGroupID)
  {
    return _createUnwindEndpointsPipeline (Aggregates.group (aGroupID,
                                                             Accumulators.sum (BSON_COUNT, Integer.valueOf (1))));
  }

  @Nonnegative
  public long getEndpointCount ()
  {
    final Document aResult = getCollection ().aggregate (_createUnwindEndpointsPipeline (Aggregates.count ())).first ();
    return aResult == null ? 0 : ((Number) aResult.get (BSON_COUNT)).longValue ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsMap <String, IEndpointUsageInfo> getEndpointURLUsageMap ()
  {
    final ICommonsMap <String, IEndpointUsageInfo> ret = new CommonsHashMap <> ();
    final Document aGroupID = new Document (BSON_ACCESS_POINT_ID, "$" + BSON_ACCESS_POINT_ID_PATH).append (BSON_SERVICE_GROUP_ID,
                                                                                                           "$" +
                                                                                                                                  BSON_SERVICE_GROUP_ID);
    for (final Document aDoc : getCollection ().aggregate (_createEndpointGroupPipeline (aGroupID))
                                               .allowDiskUse (Boolean.TRUE))
    {
      final Document aID = aDoc.get (BSON_MONGO_ID, Document.class);
      final ISMPAccessPoint aAP = m_aAccessPointMgr.getAccessPointOfID (aID.getString (BSON_ACCESS_POINT_ID));
      if (aAP == null)
        continue;

      final String sURL = aAP.getEndpointReference ();
      if (StringHelper.isNotEmpty (sURL))
      {
        final String sServiceGroupID = aID.getString (BSON_SERVICE_GROUP_ID);
        final EndpointUsageInfo aInfo = (EndpointUsageInfo) ret.computeIfAbsent (sURL, k -> new EndpointUsageInfo ());
        aInfo.addForServiceGroupID (sServiceGroupID, ((Number) aDoc.get (BSON_COUNT)).intValue ());
      }
    }
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsMap <String, IEndpointUsageInfo> getEndpointCertificateUsageMap ()
  {
    final ICommonsMap <String, IEndpointUsageInfo> ret = new CommonsHashMap <> ();
    final Document aGroupID = new Document (BSON_ACCESS_POINT_ID, "$" + BSON_ACCESS_POINT_ID_PATH).append (BSON_SERVICE_GROUP_ID,
                                                                                                           "$" +
                                                                                                                                  BSON_SERVICE_GROUP_ID);
    for (final Document aDoc : getCollection ().aggregate (_createEndpointGroupPipeline (aGroupID))
                                               .allowDiskUse (Boolean.TRUE))
    {
      final Document aID = aDoc.get (BSON_MONGO_ID, Document.class);
      final ISMPAccessPoint aAP = m_aAccessPointMgr.getAccessPointOfID (aID.getString (BSON_ACCESS_POINT_ID));
      if (aAP == null)
        continue;

      final String sNormalizedCert = SMPCertificateHelper.getNormalizedCert (aAP.getCertificate ());
      final String sServiceGroupID = aID.getString (BSON_SERVICE_GROUP_ID);
      final EndpointUsageInfo aInfo = (EndpointUsageInfo) ret.computeIfAbsent (sNormalizedCert,
                                                                               k -> new EndpointUsageInfo ());
      aInfo.addForServiceGroupID (sServiceGroupID, ((Number) aDoc.get (BSON_COUNT)).intValue ());
    }
    return ret;
  }

  /**
   * Replace the Access Point IDs of all matching endpoints.
   *
   * @param aServiceGroupID
   *        Optional service group filter. May be <code>null</code>.
   * @param aOldToNewAPID
   *        Map from old Access Point ID to new Access Point ID. May not be <code>null</code>.
   * @return The number of changed endpoints.
   */
  @Nonnegative
  private long _replaceAccessPointIDs (@Nullable final IParticipantIdentifier aServiceGroupID,
                                       @NonNull final ICommonsMap <String, String> aOldToNewAPID)
  {
    if (aOldToNewAPID.isEmpty ())
      return 0;

    long nEndpointsChanged = 0;

    Bson aFilter = Filters.in (BSON_ACCESS_POINT_ID_PATH, aOldToNewAPID.keySet ());
    if (aServiceGroupID != null)
      aFilter = Filters.and (aFilter, Filters.eq (BSON_SERVICE_GROUP_ID, aServiceGroupID.getURIEncoded ()));

    for (final Document aDoc : getCollection ().find (aFilter))
    {
      boolean bDocChanged = false;
      final List <Document> aProcesses = aDoc.getList (BSON_PROCESSES, Document.class);
      if (aProcesses != null)
        for (final Document aProcess : aProcesses)
        {
          final List <Document> aEndpoints = aProcess.getList (BSON_ENDPOINTS, Document.class);
          if (aEndpoints != null)
            for (final Document aEndpoint : aEndpoints)
            {
              final String sNewAPID = aOldToNewAPID.get (aEndpoint.getString (BSON_ACCESS_POINT_ID));
              if (sNewAPID != null)
              {
                aEndpoint.put (BSON_ACCESS_POINT_ID, sNewAPID);
                bDocChanged = true;
                nEndpointsChanged++;
              }
            }
        }
      if (bDocChanged)
        getCollection ().replaceOne (Filters.eq (BSON_ID, aDoc.getString (BSON_ID)), aDoc);
    }
    return nEndpointsChanged;
  }

  @Nonnegative
  public long updateAllEndpointURLs (@Nullable final IParticipantIdentifier aServiceGroupID,
                                     @NonNull final String sOldURL,
                                     @NonNull final String sNewURL)
  {
    ValueEnforcer.notNull (sOldURL, "OldURL");
    ValueEnforcer.notNull (sNewURL, "NewURL");

    // Determine all Access Points that need to be re-pointed
    final ICommonsMap <String, String> aOldToNewAPID = new CommonsHashMap <> ();
    for (final ISMPAccessPoint aAP : m_aAccessPointMgr.getAllAccessPoints ())
      if (sOldURL.equals (aAP.getEndpointReference ()))
      {
        final ISMPAccessPoint aNewAP = m_aAccessPointMgr.getOrCreateAccessPoint (sNewURL, aAP.getCertificate ());
        if (!aNewAP.getID ().equals (aAP.getID ()))
          aOldToNewAPID.put (aAP.getID (), aNewAP.getID ());
      }

    final long nEndpointsChanged = _replaceAccessPointIDs (aServiceGroupID, aOldToNewAPID);
    if (nEndpointsChanged > 0)
      _deleteAllUnusedAccessPoints ();
    return nEndpointsChanged;
  }

  @Nonnegative
  public long updateAllEndpointCertificates (@NonNull final String sOldCert, @NonNull final String sNewCert)
  {
    ValueEnforcer.notNull (sOldCert, "OldCert");
    ValueEnforcer.notNull (sNewCert, "NewCert");

    final String sOldCertNormalized = SMPCertificateHelper.getNormalizedCert (sOldCert);

    // Determine all Access Points that need to be re-pointed
    final ICommonsMap <String, String> aOldToNewAPID = new CommonsHashMap <> ();
    for (final ISMPAccessPoint aAP : m_aAccessPointMgr.getAllAccessPoints ())
    {
      final String sCert = aAP.getCertificate ();
      if (sCert != null && sOldCertNormalized.equals (SMPCertificateHelper.getNormalizedCert (sCert)))
      {
        final ISMPAccessPoint aNewAP = m_aAccessPointMgr.getOrCreateAccessPoint (aAP.getEndpointReference (), sNewCert);
        if (!aNewAP.getID ().equals (aAP.getID ()))
          aOldToNewAPID.put (aAP.getID (), aNewAP.getID ());
      }
    }

    final long nEndpointsChanged = _replaceAccessPointIDs (null, aOldToNewAPID);
    if (nEndpointsChanged > 0)
      _deleteAllUnusedAccessPoints ();
    return nEndpointsChanged;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllUsedAccessPointIDs ()
  {
    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    getCollection ().distinct (BSON_ACCESS_POINT_ID_PATH, String.class).forEach (x -> {
      if (x != null)
        ret.add (x);
    });
    return ret;
  }

  private void _deleteAllUnusedAccessPoints ()
  {
    m_aAccessPointMgr.deleteAllUnusedAccessPoints (getAllUsedAccessPointIDs ());
  }

  public boolean containsAnyEndpointWithTransportProfile (@Nullable final String sTransportProfileID)
  {
    if (StringHelper.isEmpty (sTransportProfileID))
      return false;

    // As simple as it can be
    return getCollection ().find (Filters.eq (BSON_TRANSPORT_PROFILE_PATH, sTransportProfileID)).iterator ().hasNext ();
  }
}
