/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend.sql.mgr;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.multimap.IMultiMapListBased;
import com.helger.collection.multimap.MultiHashMapArrayListBased;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.json.IJson;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.phoss.smp.backend.sql.AbstractJDBCEnabledManager;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardCallback;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardContact;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardIdentifier;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardName;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;

/**
 * A JDBC based implementation of the {@link ISMPBusinessCardManager} interface.
 *
 * @author Philip Helger
 * @since 9.2.4
 */
public final class SMPBusinessCardManagerJDBC extends AbstractJDBCEnabledManager implements ISMPBusinessCardManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPBusinessCardManagerJDBC.class);

  // Create with as minimal output as possible
  private static final JsonWriterSettings JWS = new JsonWriterSettings ().setIndentEnabled (false)
                                                                         .setWriteNewlineAtEnd (false);

  private final ISMPServiceGroupManager m_aServiceGroupMgr;
  private final CallbackList <ISMPBusinessCardCallback> m_aCBs = new CallbackList <> ();

  public SMPBusinessCardManagerJDBC (@Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
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

    final ESuccess eSucces = executor ().performInTransaction ( () -> {
      // Delete all existing entities
      final String sPID = aServiceGroup.getParticpantIdentifier ().getURIEncoded ();
      final long nDeleted = executor ().insertOrUpdateOrDelete ("DELETE FROM smp_bce WHERE pid=?",
                                                                new ConstantPreparedStatementDataProvider (sPID));
      if (LOGGER.isDebugEnabled () && nDeleted > 0)
        LOGGER.info ("Deleted " + nDeleted + " existing DBBusinessCardEntity rows");

      for (final SMPBusinessCardEntity aEntity : aEntities)
      {
        // Single name only
        executor ().insertOrUpdateOrDelete ("INSERT INTO smp_bce (id, pid, name, country, geoinfo, identifiers, websites, contacts, addon, regdate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                            new ConstantPreparedStatementDataProvider (aEntity.getID (),
                                                                                       sPID,
                                                                                       aEntity.names ()
                                                                                              .getFirst ()
                                                                                              .getName (),
                                                                                       aEntity.getCountryCode (),
                                                                                       aEntity.getGeographicalInformation (),
                                                                                       getBCIAsJson (aEntity.identifiers ()).getAsJsonString (JWS),
                                                                                       getStringAsJson (aEntity.websiteURIs ()).getAsJsonString (JWS),
                                                                                       getBCCAsJson (aEntity.contacts ()).getAsJsonString (JWS),
                                                                                       aEntity.getAdditionalInformation (),
                                                                                       aEntity.getRegistrationDate ()));
      }
    });
    if (eSucces.isFailure ())
      return null;

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

    final long nCount = executor ().insertOrUpdateOrDelete ("DELETE FROM smp_bce WHERE pid=?",
                                                            new ConstantPreparedStatementDataProvider (aSMPBusinessCard.getServiceGroup ()
                                                                                                                       .getParticpantIdentifier ()
                                                                                                                       .getURIEncoded ()));
    final EChange eChange = EChange.valueOf (nCount > 0);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Finished deleteSMPBusinessCard. Change=" + eChange.isChanged ());

    if (eChange.isChanged ())
      // Invoke generic callbacks
      m_aCBs.forEach (x -> x.onDeleteSMPBusinessCard (aSMPBusinessCard));

    return eChange;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards ()
  {
    final ICommonsList <ISMPBusinessCard> ret = new CommonsArrayList <> ();
    final Optional <ICommonsList <DBResultRow>> aDBResult = executor ().queryAll ("SELECT id, pid, name, country, geoinfo, identifiers, websites, contacts, addon, regdate" +
                                                                                  " FROM smp_bce");
    if (aDBResult.isPresent ())
    {
      // Group by ID
      final IMultiMapListBased <IParticipantIdentifier, SMPBusinessCardEntity> aEntityMap = new MultiHashMapArrayListBased <> ();
      for (final DBResultRow aRow : aDBResult.get ())
      {
        final SMPBusinessCardEntity aEntity = new SMPBusinessCardEntity (aRow.getAsString (0));
        // Single name only
        aEntity.names ().add (new SMPBusinessCardName (aRow.getAsString (2), null));
        aEntity.setCountryCode (aRow.getAsString (3));
        aEntity.setGeographicalInformation (aRow.getAsString (4));
        aEntity.identifiers ().setAll (getJsonAsBCI (aRow.getAsString (5)));
        aEntity.websiteURIs ().setAll (getJsonAsString (aRow.getAsString (6)));
        aEntity.contacts ().setAll (getJsonAsBCC (aRow.getAsString (7)));
        aEntity.setAdditionalInformation (aRow.getAsString (8));
        aEntity.setRegistrationDate (aRow.get (9).getAsLocalDate ());
        aEntityMap.putSingle (SimpleIdentifierFactory.INSTANCE.parseParticipantIdentifier (aRow.getAsString (1)),
                              aEntity);
      }

      // Convert
      for (final Map.Entry <IParticipantIdentifier, ICommonsList <SMPBusinessCardEntity>> aEntry : aEntityMap.entrySet ())
      {
        final IParticipantIdentifier aPID = aEntry.getKey ();
        final ISMPServiceGroup aServiceGroup = m_aServiceGroupMgr.getSMPServiceGroupOfID (aPID);
        if (aServiceGroup == null)
        {
          // Can happen if there is an inconsistency between BCE and SG tables
          if (LOGGER.isWarnEnabled ())
            LOGGER.warn ("Failed to resolve service group " + aPID.getURIEncoded ());
          return null;
        }
        ret.add (new SMPBusinessCard (aServiceGroup, aEntry.getValue ()));
      }
    }
    return ret;
  }

  @Nullable
  public ISMPBusinessCard getSMPBusinessCardOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return null;

    final IParticipantIdentifier aParticipantID = aServiceGroup.getParticpantIdentifier ();
    final Optional <ICommonsList <DBResultRow>> aDBResult = executor ().queryAll ("SELECT id, name, country, geoinfo, identifiers, websites, contacts, addon, regdate" +
                                                                                  " FROM smp_bce" +
                                                                                  " WHERE pid=?",
                                                                                  new ConstantPreparedStatementDataProvider (aParticipantID.getURIEncoded ()));
    if (!aDBResult.isPresent ())
    {
      return null;
    }

    if (aDBResult.get ().isEmpty ())
      return null;

    final ICommonsList <SMPBusinessCardEntity> aEntities = new CommonsArrayList <> ();
    for (final DBResultRow aRow : aDBResult.get ())
    {
      final SMPBusinessCardEntity aEntity = new SMPBusinessCardEntity (aRow.getAsString (0));
      // Single name only
      aEntity.names ().add (new SMPBusinessCardName (aRow.getAsString (1), null));
      aEntity.setCountryCode (aRow.getAsString (2));
      aEntity.setGeographicalInformation (aRow.getAsString (3));
      aEntity.identifiers ().setAll (getJsonAsBCI (aRow.getAsString (4)));
      aEntity.websiteURIs ().setAll (getJsonAsString (aRow.getAsString (5)));
      aEntity.contacts ().setAll (getJsonAsBCC (aRow.getAsString (6)));
      aEntity.setAdditionalInformation (aRow.getAsString (7));
      aEntity.setRegistrationDate (aRow.get (8).getAsLocalDate ());
      aEntities.add (aEntity);
    }
    return new SMPBusinessCard (aServiceGroup, aEntities);
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
  public long getSMPBusinessCardCount ()
  {
    return executor ().queryCount ("SELECT COUNT (DISTINCT pid) FROM smp_bce");
  }
}
