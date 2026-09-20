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

import org.bson.Document;
import org.bson.conversions.Bson;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.collection.paging.IPagingSpec;
import com.helger.json.IJsonArray;
import com.helger.json.serialize.JsonReader;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.backend.mongodb.SMPMongoQueryHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationDirection;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationState;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupColumn;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupFilter;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupCallback;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sgprops.SGCustomPropertyList;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPSMLException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.smlhook.IRegistrationHook;
import com.helger.phoss.smp.smlhook.RegistrationHookException;
import com.helger.phoss.smp.smlhook.RegistrationHookFactory;
import com.helger.photon.audit.AuditHelper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;

/**
 * Implementation of {@link ISMPServiceGroupManager} for the XML backend.
 *
 * @author Philip Helger
 */
public final class SMPServiceGroupManagerMongoDB extends AbstractManagerMongoDB implements ISMPServiceGroupManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPServiceGroupManagerMongoDB.class);
  private static final String BSON_ID = "id";
  private static final ESMPServiceGroupColumn [] COLUMNS = ESMPServiceGroupColumn.values ();
  private static final String BSON_OWNER_ID = "ownerid";
  private static final String BSON_PARTICIPANT_ID = "participantid";
  private static final String BSON_EXTENSION = "extension";
  private static final String BSON_CUSTOM_PROPERTIES = "customproperties";

  /** The temporary field name used for the <code>$lookup</code> results of the filtered queries */
  private static final String BSON_JOINED = "__joined";
  /** The temporary field name used for the <code>$count</code> results of the filtered queries */
  private static final String BSON_COUNT = "__count";

  private final CallbackList <ISMPServiceGroupCallback> m_aCBs = new CallbackList <> ();

  public SMPServiceGroupManagerMongoDB ()
  {
    super ("smp-servicegroup");
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
  }

  @NonNull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceGroupCallback> serviceGroupCallbacks ()
  {
    return m_aCBs;
  }

  @Nullable
  private static String _toString (@Nullable final SGCustomPropertyList aCustomProperties)
  {
    return aCustomProperties != null && aCustomProperties.isNotEmpty () ? aCustomProperties.getAsJson ()
                                                                                           .getAsJsonString () : null;
  }

  @NonNull
  @ReturnsMutableCopy
  public static Document toBson (@NonNull final ISMPServiceGroup aValue)
  {
    final Document ret = new Document ().append (BSON_ID, aValue.getID ())
                                        .append (BSON_OWNER_ID, aValue.getOwnerID ())
                                        .append (BSON_PARTICIPANT_ID, toBson (aValue.getParticipantIdentifier ()));
    if (aValue.getExtensions ().extensions ().isNotEmpty ())
      ret.append (BSON_EXTENSION, aValue.getExtensions ().getExtensionsAsJsonString ());
    ret.append (BSON_CUSTOM_PROPERTIES, _toString (aValue.getCustomProperties ()));
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public static SMPServiceGroup toDomain (@NonNull final Document aDoc)
  {
    final String sOwnerID = aDoc.getString (BSON_OWNER_ID);
    final IParticipantIdentifier aParticipantIdentifier = toParticipantID (aDoc.get (BSON_PARTICIPANT_ID,
                                                                                     Document.class));
    final String sExtension = aDoc.getString (BSON_EXTENSION);

    SGCustomPropertyList aCustomProperties = null;
    final String sCustomPropsJson = aDoc.getString (BSON_CUSTOM_PROPERTIES);
    if (StringHelper.isNotEmpty (sCustomPropsJson))
    {
      final IJsonArray aJson = JsonReader.builder ().source (sCustomPropsJson).readAsArray ();
      if (aJson != null)
        aCustomProperties = SGCustomPropertyList.fromJson (aJson);
    }
    return new SMPServiceGroup (sOwnerID, aParticipantIdentifier, sExtension, aCustomProperties);
  }

  @NonNull
  public SMPServiceGroup createSMPServiceGroup (@NonNull @Nonempty final String sOwnerID,
                                                @NonNull final IParticipantIdentifier aParticipantID,
                                                @Nullable final String sExtension,
                                                @Nullable final SGCustomPropertyList aCustomProperties,
                                                final boolean bCreateInSML) throws SMPServerException
  {
    ValueEnforcer.notEmpty (sOwnerID, "OwnerID");
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup (" +
                    sOwnerID +
                    ", " +
                    aParticipantID.getURIEncoded () +
                    ", " +
                    (StringHelper.isNotEmpty (sExtension) ? "with extension" : "without extension") +
                    ", " +
                    bCreateInSML +
                    ")");

    final SMPServiceGroup aSMPServiceGroup = new SMPServiceGroup (sOwnerID,
                                                                  aParticipantID,
                                                                  sExtension,
                                                                  aCustomProperties);

    // It's a new service group - throws exception in case of an error
    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    if (bCreateInSML)
      try
      {
        aHook.createServiceGroup (aParticipantID);
      }
      catch (final RegistrationHookException ex)
      {
        throw new SMPSMLException ("Failed to create '" + aParticipantID.getURIEncoded () + "' in SML", ex);
      }

    try
    {
      if (!getCollection ().insertOne (toBson (aSMPServiceGroup)).wasAcknowledged ())
        throw new IllegalStateException ("Failed to insert into MongoDB Collection");
    }
    catch (final RuntimeException ex)
    {
      // An error occurred - remove from SML again
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createSMPServiceGroup - failure in storing");

      if (bCreateInSML)
        try
        {
          aHook.undoCreateServiceGroup (aParticipantID);
        }
        catch (final RegistrationHookException ex2)
        {
          LOGGER.error ("Failed to undoCreateServiceGroup (" + aParticipantID.getURIEncoded () + ")", ex2);
        }
      throw ex;
    }

    AuditHelper.onAuditCreateSuccess (SMPServiceGroup.OT,
                                      aSMPServiceGroup.getID (),
                                      sOwnerID,
                                      aParticipantID.getURIEncoded (),
                                      sExtension,
                                      Boolean.valueOf (bCreateInSML));
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup - success");

    m_aCBs.forEach (x -> x.onSMPServiceGroupCreated (aSMPServiceGroup, bCreateInSML));

    return aSMPServiceGroup;
  }

  @NonNull
  public EChange updateSMPServiceGroup (@NonNull final IParticipantIdentifier aParticipantID,
                                        @NonNull @Nonempty final String sNewOwnerID,
                                        @Nullable final String sExtension,
                                        @Nullable final SGCustomPropertyList aCustomProperties) throws SMPServerException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notEmpty (sNewOwnerID, "NewOwnerID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("updateSMPServiceGroup (" +
                    aParticipantID.getURIEncoded () +
                    ", " +
                    sNewOwnerID +
                    ", " +
                    (StringHelper.isNotEmpty (sExtension) ? "with extension" : "without extension") +
                    ")");

    final String sServiceGroupID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    final String sCustomPropsJson = _toString (aCustomProperties);
    final Document aOldDoc = getCollection ().findOneAndUpdate (new Document (BSON_ID, sServiceGroupID),
                                                                Updates.combine (Updates.set (BSON_OWNER_ID,
                                                                                              sNewOwnerID),
                                                                                 Updates.set (BSON_EXTENSION,
                                                                                              sExtension),
                                                                                 Updates.set (BSON_CUSTOM_PROPERTIES,
                                                                                              sCustomPropsJson)));
    if (aOldDoc == null)
    {
      AuditHelper.onAuditModifyFailure (SMPServiceGroup.OT, "set-all", sServiceGroupID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (SMPServiceGroup.OT, "set-all", sServiceGroupID, sNewOwnerID, sExtension);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("updateSMPServiceGroup - success");

    m_aCBs.forEach (x -> x.onSMPServiceGroupUpdated (aParticipantID));

    return EChange.CHANGED;
  }

  @NonNull
  public EChange deleteSMPServiceGroup (@NonNull final IParticipantIdentifier aParticipantID,
                                        final boolean bDeleteInSML) throws SMPServerException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup (" + aParticipantID.getURIEncoded () + ", " + bDeleteInSML + ")");

    // Check first in memory, to avoid unnecessary deletion
    final ISMPServiceGroup aServiceGroup = getSMPServiceGroupOfID (aParticipantID);
    if (aServiceGroup == null)
      return EChange.UNCHANGED;

    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    if (bDeleteInSML)
    {
      // Delete in SML - throws exception in case of error
      try
      {
        aHook.deleteServiceGroup (aParticipantID);
      }
      catch (final RegistrationHookException ex)
      {
        throw new SMPSMLException ("Failed to delete '" + aParticipantID.getURIEncoded () + "' in SML", ex);
      }
    }

    // Delete all redirects (must be done before the SG is deleted)
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
    aRedirectMgr.deleteAllSMPRedirectsOfServiceGroup (aParticipantID);

    // Delete all service information (must be done before the SG is deleted)
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    aServiceInfoMgr.deleteAllSMPServiceInformationOfServiceGroup (aParticipantID);

    final String sServiceGroupID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    final DeleteResult aDR = getCollection ().deleteOne (new Document (BSON_ID, sServiceGroupID));
    if (!aDR.wasAcknowledged () || aDR.getDeletedCount () == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceGroup.OT, aParticipantID, "no-such-id");
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPServiceGroup - failure");

      // restore in SML
      if (bDeleteInSML)
      {
        // Undo deletion in SML!
        try
        {
          aHook.undoDeleteServiceGroup (aParticipantID);
        }
        catch (final RegistrationHookException ex)
        {
          LOGGER.error ("Failed to undoDeleteServiceGroup (" + aParticipantID.getURIEncoded () + ")", ex);
        }
      }

      throw new SMPNotFoundException ("No such service group '" + aParticipantID.getURIEncoded () + "'");
    }

    AuditHelper.onAuditDeleteSuccess (SMPServiceGroup.OT, aParticipantID, Boolean.valueOf (bDeleteInSML));
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup - success");

    m_aCBs.forEach (x -> x.onSMPServiceGroupDeleted (aParticipantID, bDeleteInSML));

    return EChange.CHANGED;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroups ()
  {
    final ICommonsList <ISMPServiceGroup> ret = new CommonsArrayList <> ();
    getCollection ().find ().forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  @Override
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroups (@NonNull final IPagingSpec aPagingSpec,
                                                                 @Nullable final String sSearchText)
  {
    final ICommonsList <ISMPServiceGroup> ret = new CommonsArrayList <> ();
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
    aCursor.forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @Override
  public long getSMPServiceGroupCount (@Nullable final String sSearchText)
  {
    final Bson aFilter = SMPMongoQueryHelper.createSearchFilter (COLUMNS, sSearchText);
    return aFilter == null ? getSMPServiceGroupCount () : getCollection ().countDocuments (aFilter);
  }

  /**
   * Create the first part of the aggregation pipeline, that applies the search text, the sorting
   * and the provided filter. The filter is resolved with a <code>$lookup</code> on the respective
   * other collection, so that no additional query is necessary. The sorting is applied before the
   * <code>$lookup</code>, because only there it can still use an index.
   *
   * @param eFilter
   *        The filter to be applied. May not be <code>null</code> and may not be
   *        {@link ESMPServiceGroupFilter#ALL}.
   * @param sSearchText
   *        The global search text to filter by. May be <code>null</code>.
   * @param aSort
   *        The sort document to be applied. May be <code>null</code> if no sorting is needed.
   * @return Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  private static ICommonsList <Bson> _createFilterPipeline (@NonNull final ESMPServiceGroupFilter eFilter,
                                                            @Nullable final String sSearchText,
                                                            @Nullable final Bson aSort)
  {
    final ICommonsList <Bson> ret = new CommonsArrayList <> ();

    final Bson aSearchFilter = SMPMongoQueryHelper.createSearchFilter (COLUMNS, sSearchText);
    if (aSearchFilter != null)
      ret.add (Aggregates.match (aSearchFilter));

    // Sort before the $lookup, so that an index can be used for it
    if (aSort != null)
      ret.add (Aggregates.sort (aSort));

    switch (eFilter)
    {
      case ALL -> throw new IllegalStateException ("This method must not be called with " + eFilter);
      case NO_BUSINESS_CARD ->
      {
        ret.add (Aggregates.lookup (SMPBusinessCardManagerMongoDB.COLLECTION_NAME,
                                    BSON_ID,
                                    SMPBusinessCardManagerMongoDB.BSON_SERVICE_GROUP_ID,
                                    BSON_JOINED));
        // Keep only the Service Groups that have no Business Card at all
        ret.add (Aggregates.match (Filters.size (BSON_JOINED, 0)));
      }
      case NO_BLOCKING_MIGRATION ->
      {
        // All states that prevent a new migration
        final ICommonsList <String> aStateIDs = new CommonsArrayList <> ();
        for (final EParticipantMigrationState eState : EParticipantMigrationState.values ())
          if (eState.preventsNewMigration ())
            aStateIDs.add (eState.getID ());
        if (aStateIDs.isNotEmpty ())
        {
          // The participant identifier is a sub document in both collections, and both are created
          // by the same method, so they can be compared as a whole
          ret.add (Aggregates.lookup (SMPParticipantMigrationManagerMongoDB.COLLECTION_NAME,
                                      BSON_PARTICIPANT_ID,
                                      SMPParticipantMigrationManagerMongoDB.BSON_PARTICIPANT_ID,
                                      BSON_JOINED));
          // Keep only the Service Groups that have no blocking outbound migration
          ret.add (Aggregates.match (Filters.not (Filters.elemMatch (BSON_JOINED,
                                                                     Filters.and (Filters.eq (SMPParticipantMigrationManagerMongoDB.BSON_DIRECTION,
                                                                                              EParticipantMigrationDirection.OUTBOUND.getID ()),
                                                                                  Filters.in (SMPParticipantMigrationManagerMongoDB.BSON_STATE,
                                                                                              aStateIDs))))));
        }
        // else: no state prevents a new migration - so nothing to filter
      }
    }
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroups (@NonNull final ESMPServiceGroupFilter eFilter,
                                                                 @NonNull final IPagingSpec aPagingSpec,
                                                                 @Nullable final String sSearchText)
  {
    ValueEnforcer.notNull (eFilter, "Filter");
    ValueEnforcer.notNull (aPagingSpec, "PagingSpec");

    if (eFilter.isAll ())
      return getAllSMPServiceGroups (aPagingSpec, sSearchText);

    final ICommonsList <ISMPServiceGroup> ret = new CommonsArrayList <> ();
    if (aPagingSpec.isEmptyPage ())
      return ret;

    final ICommonsList <Bson> aPipeline = _createFilterPipeline (eFilter,
                                                                 sSearchText,
                                                                 SMPMongoQueryHelper.createSort (COLUMNS, aPagingSpec));
    if (aPagingSpec.getStartIndex () > 0)
      aPipeline.add (Aggregates.skip ((int) Math.min (aPagingSpec.getStartIndex (), Integer.MAX_VALUE)));
    if (!aPagingSpec.isUnlimited ())
      aPipeline.add (Aggregates.limit ((int) Math.min (aPagingSpec.getMaxCount (), Integer.MAX_VALUE)));

    getCollection ().aggregate (aPipeline).forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  public long getSMPServiceGroupCount (@NonNull final ESMPServiceGroupFilter eFilter,
                                       @Nullable final String sSearchText)
  {
    ValueEnforcer.notNull (eFilter, "Filter");

    if (eFilter.isAll ())
      return getSMPServiceGroupCount (sSearchText);

    final ICommonsList <Bson> aPipeline = _createFilterPipeline (eFilter, sSearchText, null);
    aPipeline.add (Aggregates.count (BSON_COUNT));

    final Document aDoc = getCollection ().aggregate (aPipeline).first ();
    if (aDoc == null)
      return 0;

    final Object aCount = aDoc.get (BSON_COUNT);
    return aCount instanceof final Number n ? n.longValue () : 0;
  }

  public boolean containsAnySMPServiceGroup ()
  {
    return getCollection ().find ().projection (Projections.include ("_id")).limit (1).first () != null;
  }

  public boolean containsAnySMPServiceGroup (@NonNull final ESMPServiceGroupFilter eFilter)
  {
    ValueEnforcer.notNull (eFilter, "Filter");

    if (eFilter.isAll ())
      return containsAnySMPServiceGroup ();

    // Only the first matching document is of interest
    final ICommonsList <Bson> aPipeline = _createFilterPipeline (eFilter, null, null);
    aPipeline.add (Aggregates.limit (1));
    return getCollection ().aggregate (aPipeline).first () != null;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllSMPServiceGroupIDs ()
  {
    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    getCollection ().find ().forEach (x -> ret.add (x.getString (BSON_ID)));
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (@NonNull final String sOwnerID)
  {
    final ICommonsList <ISMPServiceGroup> ret = new CommonsArrayList <> ();
    getCollection ().find (new Document (BSON_OWNER_ID, sOwnerID)).forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @Nonnegative
  public long getSMPServiceGroupCountOfOwner (@NonNull final String sOwnerID)
  {
    return getCollection ().countDocuments (new Document (BSON_OWNER_ID, sOwnerID));
  }

  public ISMPServiceGroup getSMPServiceGroupOfID (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return null;

    final String sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    return getCollection ().find (new Document (BSON_ID, sID)).map (SMPServiceGroupManagerMongoDB::toDomain).first ();
  }

  public boolean containsSMPServiceGroupWithID (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return false;

    final String sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    return getCollection ().find (new Document (BSON_ID, sID)).first () != null;
  }

  @Nonnegative
  public long getSMPServiceGroupCount ()
  {
    return getCollection ().countDocuments ();
  }
}
