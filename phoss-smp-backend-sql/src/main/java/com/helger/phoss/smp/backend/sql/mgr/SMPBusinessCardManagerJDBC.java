/*
 * Copyright (C) 2019-2022 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.sql.mgr;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.mutable.MutableBoolean;
import com.helger.commons.state.EChange;
import com.helger.commons.state.EContinue;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.json.IJson;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.IJsonWriterSettings;
import com.helger.json.serialize.JsonReader;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardCallback;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardContact;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardIdentifier;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardName;
import com.helger.photon.audit.AuditHelper;

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
  private static final IJsonWriterSettings JWS = JsonWriterSettings.DEFAULT_SETTINGS;

  private final CallbackList <ISMPBusinessCardCallback> m_aCBs = new CallbackList <> ();

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be
   *        <code>null</code>.
   */
  public SMPBusinessCardManagerJDBC (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier)
  {
    super (aDBExecSupplier);
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
  public ISMPBusinessCard createOrUpdateSMPBusinessCard (@Nonnull final IParticipantIdentifier aParticipantID,
                                                         @Nonnull final Collection <SMPBusinessCardEntity> aEntities)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aEntities, "Entities");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createOrUpdateSMPBusinessCard (" +
                    aParticipantID.getURIEncoded () +
                    ", " +
                    aEntities.size () +
                    " entities" +
                    ")");

    final MutableBoolean aUpdated = new MutableBoolean (false);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSucces = aExecutor.performInTransaction ( () -> {
      // Delete all existing entities
      final String sPID = aParticipantID.getURIEncoded ();
      final long nDeleted = aExecutor.insertOrUpdateOrDelete ("DELETE FROM smp_bce" + " WHERE pid=?",
                                                              new ConstantPreparedStatementDataProvider (sPID));
      if (nDeleted > 0)
      {
        aUpdated.set (true);
        if (LOGGER.isDebugEnabled ())
          LOGGER.info ("Deleted " + nDeleted + " existing DBBusinessCardEntity rows");
      }

      for (final SMPBusinessCardEntity aEntity : aEntities)
      {
        // Single name only
        final String sName;
        final String sNames;
        if (aEntity.isSingleNameWithoutLanguage ())
        {
          sName = aEntity.names ().getFirst ().getName ();
          sNames = null;
        }
        else
        {
          sName = null;
          sNames = aEntity.getNamesAsJson ().getAsJsonString ();
        }
        aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_bce (id, pid, name, names, country, geoinfo, identifiers, websites, contacts, addon, regdate)" +
                                          " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                          new ConstantPreparedStatementDataProvider (aEntity.getID (),
                                                                                     sPID,
                                                                                     sName,
                                                                                     sNames,
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
    {
      if (aUpdated.booleanValue ())
        AuditHelper.onAuditModifyFailure (SMPBusinessCard.OT, "set-all", aParticipantID.getURIEncoded ());
      else
        AuditHelper.onAuditCreateFailure (SMPBusinessCard.OT, aParticipantID.getURIEncoded ());

      return null;
    }

    final SMPBusinessCard aNewBusinessCard = new SMPBusinessCard (aParticipantID, aEntities);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Finished createOrUpdateSMPBusinessCard");

    if (aUpdated.booleanValue ())
    {
      AuditHelper.onAuditModifySuccess (SMPBusinessCard.OT,
                                        "set-all",
                                        aParticipantID.getURIEncoded (),
                                        Integer.valueOf (aEntities.size ()));
    }
    else
    {
      AuditHelper.onAuditCreateSuccess (SMPBusinessCard.OT,
                                        aParticipantID.getURIEncoded (),
                                        Integer.valueOf (aEntities.size ()));
    }

    // Invoke generic callbacks
    m_aCBs.forEach (x -> x.onSMPBusinessCardCreatedOrUpdated (aNewBusinessCard));

    return aNewBusinessCard;
  }

  @Nonnull
  public EChange deleteSMPBusinessCard (@Nullable final ISMPBusinessCard aSMPBusinessCard)
  {
    if (aSMPBusinessCard == null)
      return EChange.UNCHANGED;

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPBusinessCard (" + aSMPBusinessCard.getID () + ")");

    final long nCount = newExecutor ().insertOrUpdateOrDelete ("DELETE FROM smp_bce" + " WHERE pid=?",
                                                               new ConstantPreparedStatementDataProvider (aSMPBusinessCard.getID ()));
    if (nCount <= 0)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Finished deleteSMPBusinessCard. Change=false");

      AuditHelper.onAuditDeleteFailure (SMPBusinessCard.OT, aSMPBusinessCard.getID (), "no-such-id");
      return EChange.UNCHANGED;
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Finished deleteSMPBusinessCard. Change=true");

    AuditHelper.onAuditDeleteSuccess (SMPBusinessCard.OT,
                                      aSMPBusinessCard.getID (),
                                      Integer.valueOf (aSMPBusinessCard.getEntityCount ()));

    // Invoke generic callbacks
    m_aCBs.forEach (x -> x.onSMPBusinessCardDeleted (aSMPBusinessCard));
    return EChange.CHANGED;
  }

  @Nonnull
  private static EContinue _addNames (@Nonnull final SMPBusinessCardEntity aEntity,
                                      @Nullable final String sName,
                                      @Nullable final String sNames)
  {
    if (StringHelper.hasText (sNames))
    {
      // Eventually more then one name - parse JSON
      final IJsonArray aJsonArray = JsonReader.builder ().source (sNames).readAsArray ();
      if (aJsonArray != null)
        for (final IJsonObject aJsonObj : aJsonArray.iteratorObjects ())
        {
          final SMPBusinessCardName aBCName = SMPBusinessCardName.createFromJson (aJsonObj);
          if (aBCName != null)
            aEntity.names ().add (aBCName);
        }

      // Check only once at the end
      if (aEntity.names ().isEmpty ())
      {
        LOGGER.error ("The names of a Business Entity retrieved from the DB (" +
                      sNames +
                      ") could not be parsed properly to a JSON array of JSON objects. Ignoring Business Entity.");
        return EContinue.BREAK;
      }
    }
    else
    {
      // Single name
      aEntity.names ().add (new SMPBusinessCardName (sName, null));
    }
    return EContinue.CONTINUE;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards ()
  {
    final ICommonsList <ISMPBusinessCard> ret = new CommonsArrayList <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT id, pid, name, names, country, geoinfo, identifiers, websites, contacts, addon, regdate" +
                                                                          " FROM smp_bce");
    if (aDBResult != null)
    {
      final IIdentifierFactory aIF = SMPMetaManager.getIdentifierFactory ();

      // Group by ID
      final ICommonsMap <IParticipantIdentifier, ICommonsList <SMPBusinessCardEntity>> aEntityMap = new CommonsHashMap <> ();
      for (final DBResultRow aRow : aDBResult)
      {
        final SMPBusinessCardEntity aEntity = new SMPBusinessCardEntity (aRow.getAsString (0));
        final String sPID = aRow.getAsString (1);
        final IParticipantIdentifier aPID = aIF.parseParticipantIdentifier (sPID);
        if (aPID == null)
        {
          LOGGER.error ("The participant identifier of a Business Entity retrieved from the DB (" +
                        sPID +
                        ") cannot be parsed properly. Ignoring Business Entity.");
          continue;
        }

        // Single name or multiple names?
        final String sName = aRow.getAsString (2);
        final String sNames = aRow.getAsString (3);
        _addNames (aEntity, sName, sNames);
        aEntity.setCountryCode (aRow.getAsString (4));
        aEntity.setGeographicalInformation (aRow.getAsString (5));
        aEntity.identifiers ().setAll (getJsonAsBCI (aRow.getAsString (6)));
        aEntity.websiteURIs ().setAll (getJsonAsString (aRow.getAsString (7)));
        aEntity.contacts ().setAll (getJsonAsBCC (aRow.getAsString (8)));
        aEntity.setAdditionalInformation (aRow.getAsString (9));
        aEntity.setRegistrationDate (aRow.get (10).getAsLocalDate ());
        aEntityMap.computeIfAbsent (aPID, k -> new CommonsArrayList <> ()).add (aEntity);
      }

      // Convert
      for (final Map.Entry <IParticipantIdentifier, ICommonsList <SMPBusinessCardEntity>> aEntry : aEntityMap.entrySet ())
      {
        final IParticipantIdentifier aPID = aEntry.getKey ();
        ret.add (new SMPBusinessCard (aPID, aEntry.getValue ()));
      }
    }
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllSMPBusinessCardIDs ()
  {
    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT pid" + " FROM smp_bce");
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
        ret.add (aRow.getAsString (0));
    return ret;
  }

  @Nullable
  public ISMPBusinessCard getSMPBusinessCardOfID (@Nullable final IParticipantIdentifier aID)
  {
    if (aID == null)
      return null;

    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT id, name, names, country, geoinfo, identifiers, websites, contacts, addon, regdate" +
                                                                          " FROM smp_bce" +
                                                                          " WHERE pid=?",
                                                                          new ConstantPreparedStatementDataProvider (aID.getURIEncoded ()));
    if (aDBResult == null)
      return null;

    if (aDBResult.isEmpty ())
      return null;

    final ICommonsList <SMPBusinessCardEntity> aEntities = new CommonsArrayList <> ();
    for (final DBResultRow aRow : aDBResult)
    {
      final SMPBusinessCardEntity aEntity = new SMPBusinessCardEntity (aRow.getAsString (0));
      final String sName = aRow.getAsString (1);
      final String sNames = aRow.getAsString (2);
      _addNames (aEntity, sName, sNames);
      aEntity.setCountryCode (aRow.getAsString (3));
      aEntity.setGeographicalInformation (aRow.getAsString (4));
      aEntity.identifiers ().setAll (getJsonAsBCI (aRow.getAsString (5)));
      aEntity.websiteURIs ().setAll (getJsonAsString (aRow.getAsString (6)));
      aEntity.contacts ().setAll (getJsonAsBCC (aRow.getAsString (7)));
      aEntity.setAdditionalInformation (aRow.getAsString (8));
      aEntity.setRegistrationDate (aRow.get (9).getAsLocalDate ());
      aEntities.add (aEntity);
    }
    return new SMPBusinessCard (aID, aEntities);
  }

  @Nonnegative
  public long getSMPBusinessCardCount ()
  {
    return newExecutor ().queryCount ("SELECT COUNT (DISTINCT pid) FROM smp_bce");
  }
}
