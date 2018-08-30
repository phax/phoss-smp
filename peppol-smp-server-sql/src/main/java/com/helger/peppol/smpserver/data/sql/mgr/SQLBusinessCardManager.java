/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.data.sql.mgr;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import com.helger.collection.multimap.IMultiMapListBased;
import com.helger.collection.multimap.MultiHashMapArrayListBased;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.db.jpa.JPAExecutionResult;
import com.helger.json.IJson;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.data.sql.AbstractSMPJPAEnabledManager;
import com.helger.peppol.smpserver.data.sql.model.DBBusinessCardEntity;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardCallback;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardContact;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardEntity;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardIdentifier;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;

/**
 * Manager for all {@link SMPBusinessCard} objects.
 *
 * @author Philip Helger
 */
public final class SQLBusinessCardManager extends AbstractSMPJPAEnabledManager implements ISMPBusinessCardManager
{
  // Create as minimal as possible
  private static final JsonWriterSettings JWS = new JsonWriterSettings ().setIndentEnabled (false)
                                                                         .setWriteNewlineAtEnd (false);

  private final ISMPServiceGroupManager m_aServiceGroupMgr;
  private final CallbackList <ISMPBusinessCardCallback> m_aCBs = new CallbackList <> ();

  public SQLBusinessCardManager (@Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    ValueEnforcer.notNull (aServiceGroupMgr, "ServiceGroupMgr");
    m_aServiceGroupMgr = aServiceGroupMgr;
  }

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPBusinessCardCallback> bcCallbacks ()
  {
    return m_aCBs;
  }

  @Nonnull
  public static IJson getBCIAsJson (@Nullable final List <SMPBusinessCardIdentifier> aIDs)
  {
    final JsonArray ret = new JsonArray ();
    if (aIDs != null)
      for (final SMPBusinessCardIdentifier aID : aIDs)
        ret.add (new JsonObject ().add ("id", aID.getID ())
                                  .add ("scheme", aID.getScheme ())
                                  .add ("value", aID.getValue ()));
    return ret;
  }

  @Nonnull
  public static ICommonsList <SMPBusinessCardIdentifier> getJsonAsBCI (@Nullable final String sJson)
  {
    final ICommonsList <SMPBusinessCardIdentifier> ret = new CommonsArrayList <> ();
    final IJson aJson = sJson == null ? null : JsonReader.readFromString (sJson);
    if (aJson != null && aJson.isArray ())
      for (final IJson aItem : aJson.getAsArray ())
      {
        final IJsonObject aItemObject = aItem.getAsObject ();
        final SMPBusinessCardIdentifier aBCI = new SMPBusinessCardIdentifier (aItemObject.getAsString ("id"),
                                                                              aItemObject.getAsString ("scheme"),
                                                                              aItemObject.getAsString ("value"));
        ret.add (aBCI);
      }
    return ret;
  }

  @Nonnull
  public static IJson getStringAsJson (@Nullable final Iterable <String> aIDs)
  {
    return new JsonArray ().addAll (aIDs);
  }

  @Nonnull
  public static ICommonsList <String> getJsonAsString (@Nullable final String sJson)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    final IJson aJson = sJson == null ? null : JsonReader.readFromString (sJson);
    if (aJson != null && aJson.isArray ())
      for (final IJson aItem : aJson.getAsArray ())
      {
        final String sValue = aItem.getAsValue ().getAsString ();
        ret.add (sValue);
      }
    return ret;
  }

  @Nonnull
  public static IJson getBCCAsJson (@Nullable final Iterable <SMPBusinessCardContact> aIDs)
  {
    final JsonArray ret = new JsonArray ();
    if (aIDs != null)
      for (final SMPBusinessCardContact aID : aIDs)
        ret.add (new JsonObject ().add ("id", aID.getID ())
                                  .add ("type", aID.getType ())
                                  .add ("name", aID.getName ())
                                  .add ("phone", aID.getPhoneNumber ())
                                  .add ("email", aID.getEmail ()));
    return ret;
  }

  @Nonnull
  public static ICommonsList <SMPBusinessCardContact> getJsonAsBCC (@Nullable final String sJson)
  {
    final ICommonsList <SMPBusinessCardContact> ret = new CommonsArrayList <> ();
    final IJson aJson = sJson == null ? null : JsonReader.readFromString (sJson);
    if (aJson != null && aJson.isArray ())
      for (final IJson aItem : aJson.getAsArray ())
      {
        final IJsonObject aItemObject = aItem.getAsObject ();
        final SMPBusinessCardContact aBCC = new SMPBusinessCardContact (aItemObject.getAsString ("id"),
                                                                        aItemObject.getAsString ("type"),
                                                                        aItemObject.getAsString ("name"),
                                                                        aItemObject.getAsString ("phone"),
                                                                        aItemObject.getAsString ("email"));
        ret.add (aBCC);
      }
    return ret;
  }

  @Nullable
  public ISMPBusinessCard createOrUpdateSMPBusinessCard (@Nonnull final ISMPServiceGroup aServiceGroup,
                                                         @Nonnull final Collection <SMPBusinessCardEntity> aEntities)
  {
    ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    ValueEnforcer.notNull (aEntities, "Entities");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createOrUpdateSMPBusinessCard (" +
                    aServiceGroup.getParticpantIdentifier ().getURIEncoded () +
                    ", " +
                    aEntities.size () +
                    " entities" +
                    ")");

    JPAExecutionResult <?> ret;
    ret = doInTransaction ( () -> {
      final EntityManager aEM = getEntityManager ();
      // Delete all existing entities
      final int nDeleted = aEM.createQuery ("DELETE FROM DBBusinessCardEntity p WHERE p.participantId = :id",
                                            DBBusinessCardEntity.class)
                              .setParameter ("id", aServiceGroup.getParticpantIdentifier ().getURIEncoded ())
                              .executeUpdate ();

      if (LOGGER.isDebugEnabled () && nDeleted > 0)
        LOGGER.info ("Deleted " + nDeleted + " existing DBBusinessCardEntity rows");

      for (final SMPBusinessCardEntity aEntity : aEntities)
      {
        final DBBusinessCardEntity aDBBCE = new DBBusinessCardEntity (aEntity.getID (),
                                                                      aServiceGroup.getParticpantIdentifier ()
                                                                                   .getURIEncoded (),
                                                                      aEntity.getName (),
                                                                      aEntity.getCountryCode (),
                                                                      aEntity.getGeographicalInformation (),
                                                                      getBCIAsJson (aEntity.getIdentifiers ()).getAsJsonString (JWS),
                                                                      getStringAsJson (aEntity.getAllWebsiteURIs ()).getAsJsonString (JWS),
                                                                      getBCCAsJson (aEntity.getContacts ()).getAsJsonString (JWS),
                                                                      aEntity.getAdditionalInformation (),
                                                                      aEntity.getRegistrationDate ());
        aEM.persist (aDBBCE);
      }
    });
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return null;
    }

    final SMPBusinessCard aNewBusinessCard = new SMPBusinessCard (aServiceGroup, aEntities);

    // Invoke generic callbacks
    m_aCBs.forEach (x -> x.onCreateOrUpdateSMPBusinessCard (aNewBusinessCard));

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Finished createOrUpdateSMPBusinessCard");

    return aNewBusinessCard;
  }

  @Nonnull
  public EChange deleteSMPBusinessCard (@Nullable final ISMPBusinessCard aSMPBusinessCard)
  {
    if (aSMPBusinessCard == null)
      return EChange.UNCHANGED;

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPBusinessCard (" + aSMPBusinessCard.getID () + ")");

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction ( () -> {
      final ISMPServiceGroup aServiceGroup = aSMPBusinessCard.getServiceGroup ();
      final EntityManager aEM = getEntityManager ();
      final int nCount = aEM.createQuery ("DELETE FROM DBBusinessCardEntity p WHERE p.participantId = :id",
                                          DBBusinessCardEntity.class)
                            .setParameter ("id", aServiceGroup.getParticpantIdentifier ().getURIEncoded ())
                            .executeUpdate ();

      return EChange.valueOf (nCount > 0);
    });
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return EChange.UNCHANGED;
    }

    final EChange eChange = ret.get ();

    if (eChange.isChanged ())
    {
      // Invoke generic callbacks
      m_aCBs.forEach (x -> x.onDeleteSMPBusinessCard (aSMPBusinessCard));
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Finished deleteSMPBusinessCard. Change=" + eChange.isChanged ());

    return eChange;
  }

  @Nullable
  private SMPBusinessCard _convert (@Nonnull final IParticipantIdentifier aID,
                                    @Nonnull final List <DBBusinessCardEntity> aDBEntities)
  {
    final ISMPServiceGroup aServiceGroup = m_aServiceGroupMgr.getSMPServiceGroupOfID (aID);
    if (aServiceGroup == null)
    {
      // Can happen if there is an inconsistency between BCE and SG tables
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Failed to resolve service group " + aID);
      return null;
    }

    final ICommonsList <SMPBusinessCardEntity> aEntities = new CommonsArrayList <> ();
    for (final DBBusinessCardEntity aDBEntity : aDBEntities)
    {
      final SMPBusinessCardEntity aEntity = new SMPBusinessCardEntity (aDBEntity.getId ());
      aEntity.setName (aDBEntity.getName ());
      aEntity.setCountryCode (aDBEntity.getCountryCode ());
      aEntity.setGeographicalInformation (aDBEntity.getGeographicalInformation ());
      aEntity.setIdentifiers (getJsonAsBCI (aDBEntity.getIdentifiers ()));
      aEntity.setWebsiteURIs (getJsonAsString (aDBEntity.getWebsiteURIs ()));
      aEntity.setContacts (getJsonAsBCC (aDBEntity.getContacts ()));
      aEntity.setAdditionalInformation (aDBEntity.getAdditionalInformation ());
      aEntity.setRegistrationDate (aDBEntity.getRegistrationDate ());
      aEntities.add (aEntity);
    }
    return new SMPBusinessCard (aServiceGroup, aEntities);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards ()
  {
    JPAExecutionResult <List <DBBusinessCardEntity>> ret;
    ret = doInTransaction ( () -> getEntityManager ().createQuery ("SELECT p FROM DBBusinessCardEntity p",
                                                                   DBBusinessCardEntity.class)
                                                     .getResultList ());
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return new CommonsArrayList <> ();
    }

    /// Group by ID
    final IMultiMapListBased <IParticipantIdentifier, DBBusinessCardEntity> aGrouped = new MultiHashMapArrayListBased <> ();
    for (final DBBusinessCardEntity aDBItem : ret.get ())
      aGrouped.putSingle (aDBItem.getAsBusinessIdentifier (), aDBItem);

    // Convert
    final ICommonsList <ISMPBusinessCard> aBCs = new CommonsArrayList <> ();
    for (final Map.Entry <IParticipantIdentifier, ICommonsList <DBBusinessCardEntity>> aEntry : aGrouped.entrySet ())
    {
      final ISMPBusinessCard aBC = _convert (aEntry.getKey (), aEntry.getValue ());
      if (aBC != null)
        aBCs.add (aBC);
    }
    return aBCs;
  }

  @Nullable
  public ISMPBusinessCard getSMPBusinessCardOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return null;

    JPAExecutionResult <List <DBBusinessCardEntity>> ret;
    ret = doInTransaction ( () -> getEntityManager ().createQuery ("SELECT p FROM DBBusinessCardEntity p WHERE p.participantId = :id",
                                                                   DBBusinessCardEntity.class)
                                                     .setParameter ("id",
                                                                    aServiceGroup.getParticpantIdentifier ()
                                                                                 .getURIEncoded ())
                                                     .getResultList ());
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return null;
    }

    if (ret.get ().isEmpty ())
      return null;

    return _convert (aServiceGroup.getParticpantIdentifier (), ret.get ());
  }

  @Nullable
  public ISMPBusinessCard getSMPBusinessCardOfID (@Nullable final String sID)
  {
    if (StringHelper.hasText (sID))
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aPI = aIdentifierFactory.parseParticipantIdentifier (sID);
      if (aPI != null)
        return getSMPBusinessCardOfServiceGroup (m_aServiceGroupMgr.getSMPServiceGroupOfID (aPI));
    }
    return null;
  }

  @Nonnegative
  public int getSMPBusinessCardCount ()
  {
    JPAExecutionResult <Number> ret;
    ret = doInTransaction ( () -> {
      final EntityManager em = getEntityManager ();
      return getSelectCountResultObj (em.createQuery ("SELECT COUNT(DISTINCT p.participantId) FROM DBBusinessCardEntity p"));
    });
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return 0;
    }

    if (ret.get () == null)
      return 0;
    return ret.get ().intValue ();
  }
}
