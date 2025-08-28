/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.state.EChange;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.photon.audit.AuditHelper;
import com.helger.typeconvert.impl.TypeConverter;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
  private static final String BSON_SERVICE_GROUP_ID = "sgid";
  private static final String BSON_DOCTYPE_ID = "doctypeid";
  private static final String BSON_PROCESSES = "processes";
  private static final String BSON_PROCESS_ID = "processid";
  private static final String BSON_ENDPOINTS = "endpoints";
  private static final String BSON_EXTENSIONS = "extensions";
  private static final String BSON_TRANSPORT_PROFILE = "transportprofile";
  private static final String BSON_ENDPOINT_REFERENCE = "endpointreference";
  private static final String BSON_BUSINESSLEVELSIG = "businesslevelsig";
  private static final String BSON_MINIMUM_AUTHENTICATION_LEVEL = "minauth";
  private static final String BSON_SERVICEACTIVATION = "serviceactivation";
  private static final String BSON_SERVICEEXPIRATION = "serviceexpiration";
  private static final String BSON_CERTIFICATE = "certificate";
  private static final String BSON_SERVICE_DESCRIPTION = "servicedesc";
  private static final String BSON_TECHCONTACTURL = "techcontacturl";
  private static final String BSON_TECHINFOURL = "techinfourl";

  private final IIdentifierFactory m_aIdentifierFactory;
  private final CallbackList <ISMPServiceInformationCallback> m_aCBs = new CallbackList <> ();

  public SMPServiceInformationManagerMongoDB (@Nonnull final IIdentifierFactory aIdentifierFactory)
  {
    super ("smp-serviceinfo");
    m_aIdentifierFactory = aIdentifierFactory;
  }

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceInformationCallback> serviceInformationCallbacks ()
  {
    return m_aCBs;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final ISMPEndpoint aValue)
  {
    final Document ret = new Document ().append (BSON_TRANSPORT_PROFILE, aValue.getTransportProfile ());
    if (aValue.hasEndpointReference ())
      ret.append (BSON_ENDPOINT_REFERENCE, aValue.getEndpointReference ());
    ret.append (BSON_BUSINESSLEVELSIG, Boolean.valueOf (aValue.isRequireBusinessLevelSignature ()));
    if (aValue.hasMinimumAuthenticationLevel ())
      ret.append (BSON_MINIMUM_AUTHENTICATION_LEVEL, aValue.getMinimumAuthenticationLevel ());
    if (aValue.hasServiceActivationDateTime ())
      ret.append (BSON_SERVICEACTIVATION, TypeConverter.convert (aValue.getServiceActivationDateTime (), Date.class));
    if (aValue.hasServiceExpirationDateTime ())
      ret.append (BSON_SERVICEEXPIRATION, TypeConverter.convert (aValue.getServiceExpirationDateTime (), Date.class));
    if (aValue.hasCertificate ())
      ret.append (BSON_CERTIFICATE, aValue.getCertificate ());
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

  @Nonnull
  @ReturnsMutableCopy
  public static SMPEndpoint toEndpoint (@Nonnull final Document aDoc)
  {
    final String sTransportProfile = aDoc.getString (BSON_TRANSPORT_PROFILE);
    final String sEndpointReference = aDoc.getString (BSON_ENDPOINT_REFERENCE);
    final boolean bRequireBusinessLevelSignature = aDoc.getBoolean (BSON_BUSINESSLEVELSIG,
                                                                    SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE);
    final String sMinimumAuthenticationLevel = aDoc.getString (BSON_MINIMUM_AUTHENTICATION_LEVEL);
    final XMLOffsetDateTime aServiceActivationDT = TypeConverter.convert (aDoc.getDate (BSON_SERVICEACTIVATION),
                                                                          XMLOffsetDateTime.class);
    final XMLOffsetDateTime aServiceExpirationDT = TypeConverter.convert (aDoc.getDate (BSON_SERVICEEXPIRATION),
                                                                          XMLOffsetDateTime.class);
    final String sCertificate = aDoc.getString (BSON_CERTIFICATE);
    final String sServiceDescription = aDoc.getString (BSON_SERVICE_DESCRIPTION);
    final String sTechnicalContactUrl = aDoc.getString (BSON_TECHCONTACTURL);
    final String sTechnicalInformationUrl = aDoc.getString (BSON_TECHINFOURL);
    final String sExtension = aDoc.getString (BSON_EXTENSIONS);
    return new SMPEndpoint (sTransportProfile,
                            sEndpointReference,
                            bRequireBusinessLevelSignature,
                            sMinimumAuthenticationLevel,
                            aServiceActivationDT,
                            aServiceExpirationDT,
                            sCertificate,
                            sServiceDescription,
                            sTechnicalContactUrl,
                            sTechnicalInformationUrl,
                            sExtension);
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final ISMPProcess aValue)
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
  public static SMPProcess toProcess (@Nonnull final Document aDoc)
  {
    final IProcessIdentifier aProcessID = toProcessID ((Document) aDoc.get (BSON_PROCESS_ID));
    final List <Document> aEndpointDocs = aDoc.getList (BSON_ENDPOINTS, Document.class);
    if (aEndpointDocs == null)
      return null;

    final ICommonsList <SMPEndpoint> aEndpoints = new CommonsArrayList <> ();
    for (final Document aDocEP : aEndpointDocs)
      aEndpoints.add (toEndpoint (aDocEP));
    final String sExtension = aDoc.getString (BSON_EXTENSIONS);
    return new SMPProcess (aProcessID, aEndpoints, sExtension);
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final ISMPServiceInformation aValue)
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

  @Nonnull
  @ReturnsMutableCopy
  public SMPServiceInformation toServiceInformation (@Nonnull final Document aDoc, final boolean bNeedProcesses)
  {
    final IParticipantIdentifier aParticipantID = m_aIdentifierFactory.parseParticipantIdentifier (aDoc.getString (BSON_SERVICE_GROUP_ID));
    final IDocumentTypeIdentifier aDocTypeID = toDocumentTypeID (aDoc.get (BSON_DOCTYPE_ID, Document.class));
    final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList <> ();
    if (bNeedProcesses)
      for (final Document aDocP : aDoc.getList (BSON_PROCESSES, Document.class))
      {
        final SMPProcess aProcess = toProcess (aDocP);
        if (aProcess != null)
          aProcesses.add (aProcess);
      }
    final String sExtension = aDoc.getString (BSON_EXTENSIONS);

    // The ID itself is derived from ServiceGroupID and DocTypeID
    return new SMPServiceInformation (aParticipantID, aDocTypeID, aProcesses, sExtension);
  }

  @Nullable
  public ISMPServiceInformation findServiceInformation (@Nullable final IParticipantIdentifier aParticipantID,
                                                        @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                                        @Nullable final IProcessIdentifier aProcessID,
                                                        @Nullable final String sTransportProfileID)
  {
    final ISMPServiceInformation aServiceInfo = getSMPServiceInformationOfServiceGroupAndDocumentType (aParticipantID,
                                                                                                       aDocTypeID);
    if (aServiceInfo != null)
    {
      final ISMPProcess aProcess = aServiceInfo.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        final ISMPEndpoint aEndpoint = aProcess.getEndpointOfTransportProfile (sTransportProfileID);
        if (aEndpoint != null)
          return aServiceInfo;
      }
    }
    return null;
  }

  @Nonnull
  public ESuccess mergeSMPServiceInformation (@Nonnull final ISMPServiceInformation aSMPServiceInformationObj)
  {
    final SMPServiceInformation aSMPServiceInformation = (SMPServiceInformation) aSMPServiceInformationObj;
    ValueEnforcer.notNull (aSMPServiceInformation, "ServiceInformation");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("mergeSMPServiceInformation (" + aSMPServiceInformationObj + ")");

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

      if (bChangedExisting)
        m_aCBs.forEach (x -> x.onSMPServiceInformationUpdated (aSMPServiceInformation));
      else
        m_aCBs.forEach (x -> x.onSMPServiceInformationCreated (aSMPServiceInformation));
    }
    return ESuccess.SUCCESS;
  }

  @Nonnull
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

  @Nonnull
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    EChange eChange = EChange.UNCHANGED;
    for (final ISMPServiceInformation aSMPServiceInformation : getAllSMPServiceInformationOfServiceGroup (aParticipantIdentifier))
      eChange = eChange.or (deleteSMPServiceInformation (aSMPServiceInformation));
    return eChange;
  }

  @Nonnull
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

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation ()
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    forEachSMPServiceInformation (ret::add);
    return ret;
  }

  public void forEachSMPServiceInformation (@Nonnull final Consumer <? super ISMPServiceInformation> aConsumer)
  {
    getCollection ().find ().forEach (x -> aConsumer.accept (toServiceInformation (x, true)));
  }

  @Nonnegative
  public long getSMPServiceInformationCount ()
  {
    return getCollection ().countDocuments ();
  }

  @Nonnull
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

  @Nonnull
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
                    .forEach ((Consumer <Document>) x -> ret.add (toServiceInformation (x, true)));

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

  public boolean containsAnyEndpointWithTransportProfile (@Nullable final String sTransportProfileID)
  {
    if (StringHelper.isEmpty (sTransportProfileID))
      return false;

    // As simple as it can be
    return getCollection ().find (new Document (BSON_PROCESSES + "." + BSON_ENDPOINTS + "." + BSON_TRANSPORT_PROFILE,
                                                sTransportProfileID)).iterator ().hasNext ();
  }
}
