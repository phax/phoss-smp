/*
 * Copyright (C) 2014-2024 Philip Helger and contributors
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
package com.helger.phoss.smp.rest;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsTreeMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mutable.MutableInt;
import com.helger.commons.timing.StopWatch;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.exchange.CSMPExchange;
import com.helger.phoss.smp.exchange.ImportActionItem;
import com.helger.phoss.smp.exchange.ImportSummary;
import com.helger.phoss.smp.exchange.ServiceGroupImport;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * REST API to import Service Groups from XML v1
 *
 * @author Philip Helger
 * @since 5.6.0
 */
public final class APIExecutorImportXMLVer1 extends AbstractSMPAPIExecutor
{
  private static final boolean DEFAULT_OVERWRITE_EXISTING = false;

  public static final String PARAM_OVERVWRITE_EXISTING = "overwrite-existing";

  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorImportXMLVer1.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope, null);

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. importServiceGroups will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    final String sLogPrefix = "[REST API Import-XML-V1] ";
    final String sPathUserLoginName = aPathVariables.get (SMPRestFilter.PARAM_USER_ID);

    LOGGER.info (sLogPrefix + "Starting Import");

    // Only authenticated user may do so
    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());
    SMPUserManagerPhoton.validateUserCredentials (aCredentials);

    // Start action after authentication
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();

    final ICommonsSet <String> aAllServiceGroupIDs = aServiceGroupMgr.getAllSMPServiceGroupIDs ();
    final ICommonsSet <String> aAllBusinessCardIDs = aBusinessCardMgr.getAllSMPBusinessCardIDs ();

    // Try to use ID or login name
    IUser aDefaultOwner = aUserMgr.getUserOfID (sPathUserLoginName);
    if (aDefaultOwner == null)
      aDefaultOwner = aUserMgr.getUserOfLoginName (sPathUserLoginName);

    if (aDefaultOwner == null || aDefaultOwner.isDeleted ())
    {
      // Cannot set the owner to a deleted user
      // Setting the owner to a disabled user might make sense
      throw new SMPBadRequestException (sLogPrefix +
                                        "The user ID or login name '" +
                                        sPathUserLoginName +
                                        "' does not exist",
                                        aDataProvider.getCurrentURI ());
    }

    LOGGER.info (sLogPrefix +
                 "Using '" +
                 aDefaultOwner.getID () +
                 "' / '" +
                 aDefaultOwner.getLoginName () +
                 "' as the default owner");

    final boolean bOverwriteExisting = aRequestScope.params ()
                                                    .getAsBoolean (PARAM_OVERVWRITE_EXISTING,
                                                                   DEFAULT_OVERWRITE_EXISTING);

    final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    final IMicroDocument aDoc = MicroReader.readMicroXML (aPayload);
    if (aDoc == null || aDoc.getDocumentElement () == null)
    {
      // Cannot parse
      throw new SMPBadRequestException ("Failed to parse XML payload", aDataProvider.getCurrentURI ());
    }

    final String sVersion = aDoc.getDocumentElement ().getAttributeValue (CSMPExchange.ATTR_VERSION);
    if (!CSMPExchange.VERSION_10.equals (sVersion))
    {
      throw new SMPBadRequestException ("The provided payload is not an XML file version 1.0",
                                        aDataProvider.getCurrentURI ());
    }

    // Version 1.0
    LOGGER.info (sLogPrefix + "The provided payload is an XML file version 1.0");

    final ZonedDateTime aQueryDT = PDTFactory.getCurrentZonedDateTimeUTC ();
    final StopWatch aSW = StopWatch.createdStarted ();

    // Start the import
    final ICommonsList <ImportActionItem> aActionList = new CommonsArrayList <> ();
    final ImportSummary aImportSummary = new ImportSummary ();
    ServiceGroupImport.importXMLVer10 (aDoc.getDocumentElement (),
                                       bOverwriteExisting,
                                       aDefaultOwner,
                                       aAllServiceGroupIDs,
                                       aAllBusinessCardIDs,
                                       aActionList,
                                       aImportSummary);

    aSW.stop ();
    LOGGER.info (sLogPrefix + "Finished import after " + aSW.getMillis () + " milliseconds");

    // Everything added to the action list is already logged
    final boolean bResponseAsXML = true;
    if (bResponseAsXML)
    {
      // Create XML version
      final IMicroDocument aResponseDoc = new MicroDocument ();
      final IMicroElement eRoot = aResponseDoc.appendElement ("importResult");
      eRoot.setAttribute ("version", "1");
      eRoot.setAttribute ("importStartDateTime", PDTWebDateHelper.getAsStringXSD (aQueryDT));

      final IMicroElement eSettings = eRoot.appendElement ("settings");
      eSettings.setAttribute ("overwriteExisting", bOverwriteExisting);
      eSettings.setAttribute ("defaultOwnerID", aDefaultOwner.getID ());
      eSettings.setAttribute ("defaultOwnerLoginName", aDefaultOwner.getLoginName ());

      final ICommonsMap <String, MutableInt> aErrorLevelCount = new CommonsTreeMap <> ();
      for (final ImportActionItem aAction : aActionList)
      {
        eRoot.appendChild (aAction.getAsMicroElement ("action"));
        aErrorLevelCount.computeIfAbsent (aAction.getErrorLevelName (), k -> new MutableInt (0)).inc ();
      }

      {
        final IMicroElement eSummary = eRoot.appendElement ("summary");
        eSummary.setAttribute ("durationMillis", aSW.getMillis ());
        for (final Map.Entry <String, MutableInt> aEntry : aErrorLevelCount.entrySet ())
          eSummary.appendElement ("errorlevel")
                  .setAttribute ("id", aEntry.getKey ())
                  .setAttribute ("count", aEntry.getValue ().intValue ());

        aImportSummary.appendTo (eSummary);
      }

      final XMLWriterSettings aXWS = new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN);
      aUnifiedResponse.setContentAndCharset (MicroWriter.getNodeAsString (aResponseDoc, aXWS), aXWS.getCharset ())
                      .setMimeType (new MimeType (CMimeType.APPLICATION_XML).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                           aXWS.getCharset ().name ()));
    }
    else
    {
      // Create JSON version
      final IJsonObject aJson = new JsonObject ();
      aJson.add ("version", "1");
      aJson.add ("importStartDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME.format (aQueryDT));
      aJson.addJson ("settings",
                     new JsonObject ().add ("overwriteExisting", bOverwriteExisting)
                                      .add ("defaultOwnerID", aDefaultOwner.getID ())
                                      .add ("defaultOwnerLoginName", aDefaultOwner.getLoginName ()));
      final IJsonArray aActions = new JsonArray ();
      final ICommonsMap <String, MutableInt> aLevelCount = new CommonsTreeMap <> ();
      for (final ImportActionItem aAction : aActionList)
      {
        aActions.add (aAction.getAsJsonObject ());
        aLevelCount.computeIfAbsent (aAction.getErrorLevelName (), k -> new MutableInt (0)).inc ();
      }
      aJson.addJson ("actions", aActions);

      {
        final IJsonObject aSummary = new JsonObject ();
        aSummary.add ("durationMillis", aSW.getMillis ());
        final IJsonArray aLevels = new JsonArray ();
        for (final Map.Entry <String, MutableInt> aEntry : aLevelCount.entrySet ())
          aLevels.add (new JsonObject ().add ("id", aEntry.getKey ()).add ("count", aEntry.getValue ().intValue ()));
        aSummary.addJson ("errorlevels", aLevels);

        aImportSummary.appendTo (aSummary);

        aJson.addJson ("summary", aSummary);
      }

      final String sRet = new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeAsString (aJson);
      aUnifiedResponse.setContentAndCharset (sRet, StandardCharsets.UTF_8).setMimeType (CMimeType.APPLICATION_JSON);
    }
    aUnifiedResponse.disableCaching ();
  }
}
