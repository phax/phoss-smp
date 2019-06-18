/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
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

import java.time.LocalDateTime;
import java.util.Date;
import java.util.function.Consumer;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.typeconvert.TypeConverter;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.photon.audit.AuditHelper;
import com.mongodb.client.model.Filters;
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

  private final CallbackList <ISMPServiceInformationCallback> m_aCBs = new CallbackList <> ();

  public SMPServiceInformationManagerMongoDB ()
  {
    super ("smp-serviceinfo");
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
    if (aValue.extensions ().isNotEmpty ())
      ret.append (BSON_EXTENSIONS, aValue.getExtensionsAsString ());
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
    // TODO fix conversion twice
    final LocalDateTime aServiceActivationDT = TypeConverter.convert (aDoc.getDate (BSON_SERVICEACTIVATION),
                                                                      LocalDateTime.class);
    final LocalDateTime aServiceExpirationDT = TypeConverter.convert (aDoc.getDate (BSON_SERVICEEXPIRATION),
                                                                      LocalDateTime.class);
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
    final ICommonsList <Document> aEndpoints = new CommonsArrayList <> ();
    for (final ISMPEndpoint aEndpoint : aValue.getAllEndpoints ())
      aEndpoints.add (toBson (aEndpoint));
    if (aEndpoints.isNotEmpty ())
      ret.append (BSON_ENDPOINTS, aEndpoints);
    if (aValue.extensions ().isNotEmpty ())
      ret.append (BSON_EXTENSIONS, aValue.getExtensionsAsString ());
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static SMPProcess toProcess (@Nonnull final Document aDoc)
  {
    final IProcessIdentifier aProcessID = toProcessID ((Document) aDoc.get (BSON_PROCESS_ID));
    final ICommonsList <SMPEndpoint> aEndpoints = new CommonsArrayList <> ();
    for (final Document aDocEP : aDoc.getList (BSON_ENDPOINTS, Document.class))
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
    final ICommonsList <Document> aProcs = new CommonsArrayList <> ();
    for (final ISMPProcess aProc : aValue.getAllProcesses ())
      aProcs.add (toBson (aProc));
    if (aProcs.isNotEmpty ())
      ret.append (BSON_PROCESSES, aProcs);
    if (aValue.extensions ().isNotEmpty ())
      ret.append (BSON_EXTENSIONS, aValue.getExtensionsAsString ());
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static SMPServiceInformation toServiceInformation (@Nonnull final Document aDoc, final boolean bNeedProcesses)
  {
    final IIdentifierFactory aIF = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceGroup aServiceGroup = aSGMgr.getSMPServiceGroupOfID (aIF.parseParticipantIdentifier (aDoc.getString (BSON_SERVICE_GROUP_ID)));
    final IDocumentTypeIdentifier aDocTypeID = toDocumentTypeID (aDoc.get (BSON_DOCTYPE_ID, Document.class));
    final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList <> ();
    if (bNeedProcesses)
      for (final Document aDocP : aDoc.getList (BSON_PROCESSES, Document.class))
        aProcesses.add (toProcess (aDocP));
    final String sExtension = aDoc.getString (BSON_EXTENSIONS);

    // The ID itself is derived from ServiceGroupID and DocTypeID
    return new SMPServiceInformation (aServiceGroup, aDocTypeID, aProcesses, sExtension);
  }

  @Nullable
  public ISMPServiceInformation findServiceInformation (@Nullable final ISMPServiceGroup aServiceGroup,
                                                        @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                                        @Nullable final IProcessIdentifier aProcessID,
                                                        @Nullable final ISMPTransportProfile aTransportProfile)
  {
    final ISMPServiceInformation aServiceInfo = getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                       aDocTypeID);
    if (aServiceInfo != null)
    {
      final ISMPProcess aProcess = aServiceInfo.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        final ISMPEndpoint aEndpoint = aProcess.getEndpointOfTransportProfile (aTransportProfile);
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
    final SMPServiceInformation aOldInformation = (SMPServiceInformation) getSMPServiceInformationOfServiceGroupAndDocumentType (aSMPServiceInformation.getServiceGroup (),
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
                                        aOldInformation.getID (),
                                        aOldInformation.getServiceGroupID (),
                                        aOldInformation.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aOldInformation.getAllProcesses (),
                                        aOldInformation.getExtensionsAsString ());

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

      getCollection ().insertOne (toBson (aSMPServiceInformation));

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
                                        aSMPServiceInformation.getExtensionsAsString ());
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
      AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, "no-such-id", aSMPServiceInformation.getID ());
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
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    EChange eChange = EChange.UNCHANGED;
    for (final ISMPServiceInformation aSMPServiceInformation : getAllSMPServiceInformationOfServiceGroup (aServiceGroup))
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
      AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, "no-such-id", aSMPServiceInformation.getID ());
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPProcess - failure");
      return EChange.UNCHANGED;
    }

    // Main deletion in write lock
    if (aRealServiceInformation.deleteProcess (aProcess).isUnchanged ())
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT,
                                        "no-such-process",
                                        aSMPServiceInformation.getID (),
                                        aProcess.getProcessIdentifier ().getURIEncoded ());
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
    getCollection ().find ().forEach ((Consumer <Document>) x -> ret.add (toServiceInformation (x, true)));
    return ret;
  }

  @Nonnegative
  public long getSMPServiceInformationCount ()
  {
    return getCollection ().countDocuments ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    if (aServiceGroup != null)
      getCollection ().find (new Document (BSON_SERVICE_GROUP_ID, aServiceGroup.getID ()))
                      .forEach ((Consumer <Document>) x -> ret.add (toServiceInformation (x, true)));
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final ICommonsList <IDocumentTypeIdentifier> ret = new CommonsArrayList <> ();
    if (aServiceGroup != null)
    {
      getCollection ().find (new Document (BSON_SERVICE_GROUP_ID, aServiceGroup.getID ()))
                      .forEach ((Consumer <Document>) x -> ret.add (toServiceInformation (x,
                                                                                          false).getDocumentTypeIdentifier ()));
    }
    return ret;
  }

  @Nullable
  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                                       @Nullable final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    if (aServiceGroup == null)
      return null;
    if (aDocumentTypeIdentifier == null)
      return null;

    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    getCollection ().find (Filters.and (new Document (BSON_SERVICE_GROUP_ID, aServiceGroup.getID ()),
                                        new Document (BSON_DOCTYPE_ID, toBson (aDocumentTypeIdentifier))))
                    .forEach ((Consumer <Document>) x -> ret.add (toServiceInformation (x, true)));

    if (ret.isEmpty ())
      return null;
    if (ret.size () > 1)
      LOGGER.warn ("Found more than one entry for service group '" +
                   aServiceGroup.getID () +
                   "' and document type '" +
                   aDocumentTypeIdentifier.getValue () +
                   "'. This seems to be a bug! Using the first one.");
    return ret.getFirst ();
  }
}
