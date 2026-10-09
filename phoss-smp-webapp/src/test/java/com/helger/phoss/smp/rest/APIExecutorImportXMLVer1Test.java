/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.json.IJson;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.mime.CMimeType;
import com.helger.phoss.smp.exchange.EImportSummaryAction;
import com.helger.phoss.smp.exchange.ImportActionItem;
import com.helger.phoss.smp.exchange.ImportSummary;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.security.user.User;
import com.helger.security.password.hash.PasswordHash;
import com.helger.security.password.salt.PasswordSalt;
import com.helger.servlet.ServletContextPathHolder;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.servlet.mock.MockHttpServletResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.web.scope.impl.RequestWebScope;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.serialize.MicroReader;

/**
 * Test class for class {@link APIExecutorImportXMLVer1}.
 */
public final class APIExecutorImportXMLVer1Test
{
  private static final String JSON = CMimeType.APPLICATION_JSON.getAsString ();
  private static final String XML = CMimeType.APPLICATION_XML.getAsString ();

  @Before
  public void before ()
  {
    ServletContextPathHolder.clearContextPath ();
    ServletContextPathHolder.setCustomContextPath ("/smp");
  }

  @After
  public void after ()
  {
    ServletContextPathHolder.clearContextPath ();
  }

  @NonNull
  private static MockHttpServletResponse _getResponse (@Nullable final String sAccept) throws IOException
  {
    final MockHttpServletRequest aHttpRequest = new MockHttpServletRequest ();
    final MockHttpServletResponse aHttpResponse = new MockHttpServletResponse ();
    final IRequestWebScopeWithoutResponse aRequestScope = new RequestWebScope (aHttpRequest, aHttpResponse);
    final PhotonUnifiedResponse aUnifiedResponse = PhotonUnifiedResponse.createSimple (aRequestScope);

    final ICommonsList <ImportActionItem> aActionList = new CommonsArrayList <> ();
    aActionList.add (ImportActionItem.createSuccess ("participant1", "Created"));
    aActionList.add (ImportActionItem.createWarning ("participant2", "Careful"));
    final ImportSummary aSummary = new ImportSummary ();
    aSummary.onSuccess (EImportSummaryAction.CREATE_SG);
    aSummary.onError (EImportSummaryAction.CREATE_SG);

    final User aOwner = User.createdPredefinedUser ("owner-id",
                                                         "owner@example.org",
                                                         null,
                                                         new PasswordHash ("dummy",
                                                                           PasswordSalt.createRandom (),
                                                                           "dummy"),
                                                         null,
                                                         null,
                                                         null,
                                                         Locale.ENGLISH,
                                                         null,
                                                         false);

    APIExecutorImportXMLVer1.fillResponse (APIExecutorImportXMLVer1.isResponseAsXML (sAccept),
                                           ZonedDateTime.now (),
                                           true,
                                           false,
                                           aOwner,
                                           aActionList,
                                           aSummary,
                                           42,
                                           aUnifiedResponse);
    final MockHttpServletResponse ret = new MockHttpServletResponse ();
    aUnifiedResponse.applyToResponse (ret);
    return ret;
  }

  private static void _assertXML (@Nullable final String sAccept) throws IOException
  {
    final MockHttpServletResponse aResponse = _getResponse (sAccept);
    assertEquals (200, aResponse.getStatus ());
    assertTrue (aResponse.getContentType (), aResponse.getContentType ().startsWith (XML));
    final IMicroDocument aDoc = MicroReader.readMicroXML (aResponse.getContentAsString (StandardCharsets.UTF_8));
    assertNotNull (aDoc);
    final IMicroElement eRoot = aDoc.getDocumentElement ();
    assertEquals ("importResult", eRoot.getTagName ());
    assertEquals ("1", eRoot.getAttributeValue ("version"));
    assertEquals (2, eRoot.getAllChildElements ("action").size ());
  }

  private static void _assertJSON (@Nullable final String sAccept) throws IOException
  {
    final MockHttpServletResponse aResponse = _getResponse (sAccept);
    assertEquals (200, aResponse.getStatus ());
    assertTrue (aResponse.getContentType (), aResponse.getContentType ().startsWith (JSON));
    final IJson aJson = JsonReader.builder ().source (aResponse.getContentAsString (StandardCharsets.UTF_8)).read ();
    assertNotNull (aJson);
    assertTrue (aJson.isObject ());

    final IJsonObject aRoot = aJson.getAsObject ();
    assertEquals ("1", aRoot.getAsString ("version"));
    assertTrue (aRoot.containsKey ("importStartDateTime"));

    final IJsonObject aSettings = aRoot.getAsObject ("settings");
    assertNotNull (aSettings);
    assertTrue (aSettings.getAsBoolean ("overwriteExisting"));
    assertFalse (aSettings.getAsBoolean ("dryRun"));
    assertEquals ("owner-id", aSettings.getAsString ("defaultOwnerID"));
    assertEquals ("owner@example.org", aSettings.getAsString ("defaultOwnerLoginName"));

    final IJsonArray aActions = aRoot.getAsArray ("actions");
    assertNotNull (aActions);
    assertEquals (2, aActions.size ());
    assertEquals ("info", aActions.get ((0)).getAsObject ().getAsString ("level"));
    assertEquals ("participant1", aActions.get ((0)).getAsObject ().getAsString ("participantID"));
    assertEquals ("Created", aActions.get ((0)).getAsObject ().getAsString ("message"));
    assertEquals ("warning", aActions.get ((1)).getAsObject ().getAsString ("level"));

    final IJsonObject aSummary = aRoot.getAsObject ("summary");
    assertNotNull (aSummary);
    assertEquals (42, aSummary.getAsInt ("durationMillis"));
    final IJsonArray aLevels = aSummary.getAsArray ("errorlevels");
    assertEquals (2, aLevels.size ());
    final IJsonArray aSummaryActions = aSummary.getAsArray ("actions");
    assertEquals (1, aSummaryActions.size ());
    assertEquals ("create-servicegroup", aSummaryActions.get ((0)).getAsObject ().getAsString ("id"));
    assertEquals (1, aSummaryActions.get ((0)).getAsObject ().getAsInt ("success"));
    assertEquals (1, aSummaryActions.get ((0)).getAsObject ().getAsInt ("error"));
  }

  @Test
  public void testJson () throws IOException
  {
    _assertJSON ("application/json");
  }

  @Test
  public void testXml () throws IOException
  {
    _assertXML ("application/xml");
    _assertXML ("text/xml");
    _assertXML ("application/*");
  }

  @Test
  public void testWildcardOrMissing () throws IOException
  {
    _assertXML ("*/*");
    _assertXML (null);
    _assertXML ("");
  }

  @Test
  public void testUnsupportedFallsBackToXml () throws IOException
  {
    // Must never be a 406 - the import was already executed
    _assertXML ("text/csv");
  }

  @Test
  public void testQualityValues () throws IOException
  {
    _assertXML ("application/json;q=0.5, application/xml;q=0.9");
    _assertJSON ("application/xml;q=0.5, application/json;q=0.9");
    // Tie: XML is the default
    _assertXML ("application/json, application/xml");
    // A naive contains ("json") check gets this wrong
    _assertXML ("application/xml, application/json;q=0.1");
  }
}
