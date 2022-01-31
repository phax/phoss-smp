/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.CGlobal;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.error.IError;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.error.level.IErrorLevel;
import com.helger.commons.http.CHttp;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.timing.StopWatch;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exchange.CSMPExchange;
import com.helger.phoss.smp.exchange.ServiceGroupImport;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;

/**
 * REST API to import Service Groups from XML v1
 *
 * @author Philip Helger
 * @since 6.0.0
 */
public final class APIExecutorImportXMLVer1 extends AbstractSMPAPIExecutor
{
  private static final boolean DEFAULT_OVERWRITE_EXISTING = false;

  public static final String PARAM_OVERVWRITE_EXISTING = "overwrite-existing";

  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorImportXMLVer1.class);

  @Nonnull
  @Nonempty
  private static String _getErrorLevelName (@Nonnull final IErrorLevel aErrorLevel)
  {
    if (aErrorLevel.isGE (EErrorLevel.ERROR))
      return "error";
    if (aErrorLevel.isGE (EErrorLevel.WARN))
      return "warning";
    return "info";
  }

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. importServiceGroups will not be executed.");
      aUnifiedResponse.setStatus (CHttp.HTTP_PRECONDITION_FAILED);
    }
    else
    {
      final String sPathUserLoginName = aPathVariables.get (SMPRestFilter.PARAM_USER_ID);

      final String sLogPrefix = "[Import-XML-V1] ";
      LOGGER.info (sLogPrefix + "Starting Import");

      // Only authenticated user may do so
      final BasicAuthClientCredentials aBasicAuth = SMPRestRequestHelper.getMandatoryAuth (aRequestScope.headers ());
      SMPUserManagerPhoton.validateUserCredentials (aBasicAuth);

      // Start action after authentication
      final Locale aDisplayLocale = CSMPServer.DEFAULT_LOCALE;
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
        LOGGER.warn (sLogPrefix + "The user ID or login name '" + sPathUserLoginName + "' does not exist.");
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
      }
      else
      {
        LOGGER.info (sLogPrefix + "Using '" + aDefaultOwner.getID () + "' / '" + aDefaultOwner.getLoginName () + "' as the default owner");

        final boolean bOverwriteExisting = aRequestScope.params ().getAsBoolean (PARAM_OVERVWRITE_EXISTING, DEFAULT_OVERWRITE_EXISTING);

        final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
        final IMicroDocument aDoc = MicroReader.readMicroXML (aPayload);
        if (aDoc == null || aDoc.getDocumentElement () == null)
        {
          // Cannot parse
          LOGGER.warn (sLogPrefix + "Failed to parse XML payload.");
          aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        }
        else
        {
          final String sVersion = aDoc.getDocumentElement ().getAttributeValue (CSMPExchange.ATTR_VERSION);
          if (!CSMPExchange.VERSION_10.equals (sVersion))
          {
            LOGGER.warn (sLogPrefix + "The provided payload is not an XML file version 1.0.");
            aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
          }
          else
          {
            // Version 1.0
            LOGGER.info (sLogPrefix + "The provided payload is an XML file version 1.0");

            final ZonedDateTime aQueryDT = PDTFactory.getCurrentZonedDateTimeUTC ();
            final StopWatch aSW = StopWatch.createdStarted ();

            // Start the import
            final ICommonsList <IError> aActionList = new CommonsArrayList <> ();
            ServiceGroupImport.importXMLVer10 (aDoc.getDocumentElement (),
                                               bOverwriteExisting,
                                               aDefaultOwner,
                                               aAllServiceGroupIDs,
                                               aAllBusinessCardIDs,
                                               aActionList);

            aSW.stop ();
            LOGGER.info (sLogPrefix + "Finished import after " + aSW.getMillis () + " milliseconds");

            // Everything added to the action list is already logged
            final IJsonObject aJson = new JsonObject ();
            aJson.add ("importStartDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME.format (aQueryDT));
            aJson.add ("durationMillis", aSW.getMillis ());
            aJson.add ("settings",
                       new JsonObject ().add ("overwriteExisting", bOverwriteExisting)
                                        .add ("defaultOwnerID", aDefaultOwner.getID ())
                                        .add ("defaultOwnerLoginName", aDefaultOwner.getLoginName ()));
            final IJsonArray aActions = new JsonArray ();
            for (final IError aError : aActionList)
            {
              aActions.add (new JsonObject ().add ("level", _getErrorLevelName (aError.getErrorLevel ()))
                                             .add ("message", aError.getErrorText (aDisplayLocale)));
            }
            aJson.add ("actions", aActions);

            final String sRet = new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeAsString (aJson);
            aUnifiedResponse.setContentAndCharset (sRet, StandardCharsets.UTF_8)
                            .setMimeType (CMimeType.APPLICATION_JSON)
                            .enableCaching (3 * CGlobal.SECONDS_PER_HOUR);
          }
        }
      }
    }
  }
}
