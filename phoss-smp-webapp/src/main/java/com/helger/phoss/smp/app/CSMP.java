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
package com.helger.phoss.smp.app;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.CodingStyleguideUnaware;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.CSMPServer;
import com.helger.photon.security.CSecurity;

/**
 * Contains application wide constants.
 *
 * @author Philip Helger
 */
@Immutable
public final class CSMP
{
  public static final String APPLICATION_TITLE = "phoss SMP";
  public static final String AUTHOR = "Philip Helger";
  public static final String AUTHOR_EMAIL = "philip@helger.com";

  // Security roles
  public static final String ROLE_CONFIG_ID = "config";
  public static final String ROLE_CONFIG_NAME = "Config user";
  public static final String ROLE_CONFIG_DESCRIPTION = null;
  public static final ICommonsMap <String, String> ROLE_CONFIG_CUSTOMATTRS = null;

  public static final String ROLE_WRITABLERESTAPI_ID = "writablerestapi";
  public static final String ROLE_WRITABLERESTAPI_NAME = "Writable REST API access";
  public static final String ROLE_WRITABLERESTAPI_DESCRIPTION = null;
  public static final ICommonsMap <String, String> ROLE_WRITABLERESTAPI_CUSTOMATTRS = null;

  @CodingStyleguideUnaware
  public static final List <String> REQUIRED_ROLE_IDS_CONFIG = new CommonsArrayList <> (ROLE_CONFIG_ID).getAsUnmodifiable ();
  @CodingStyleguideUnaware
  public static final List <String> REQUIRED_ROLE_IDS_WRITABLERESTAPI = new CommonsArrayList <> (ROLE_WRITABLERESTAPI_ID).getAsUnmodifiable ();

  // User groups
  public static final String USERGROUP_ADMINISTRATORS_ID = CSecurity.USERGROUP_ADMINISTRATORS_ID;
  public static final String USERGROUP_ADMINISTRATORS_NAME = CSecurity.USERGROUP_ADMINISTRATORS_NAME;
  public static final String USERGROUP_ADMINISTRATORS_DESCRIPTION = null;
  public static final ICommonsMap <String, String> USERGROUP_ADMINISTRATORS_CUSTOMATTRS = null;

  public static final String USERGROUP_CONFIG_ID = "ugconfig";
  public static final String USERGROUP_CONFIG_NAME = "Config user";
  public static final String USERGROUP_CONFIG_DESCRIPTION = null;
  public static final ICommonsMap <String, String> USERGROUP_CONFIG_CUSTOMATTRS = null;

  public static final String USERGROUP_WRITABLERESTAPI_ID = "ugwritablerestapi";
  public static final String USERGROUP_WRITABLERESTAPI_NAME = "Writable REST API users";
  public static final String USERGROUP_WRITABLERESTAPI_DESCRIPTION = null;
  public static final ICommonsMap <String, String> USERGROUP_WRITABLERESTAPI_CUSTOMATTRS = null;

  // User ID
  public static final String USER_ADMINISTRATOR_ID = CSecurity.USER_ADMINISTRATOR_ID;
  public static final String USER_ADMINISTRATOR_LOGINNAME = CSecurity.USER_ADMINISTRATOR_EMAIL;
  public static final String USER_ADMINISTRATOR_EMAIL = CSecurity.USER_ADMINISTRATOR_EMAIL;
  public static final String USER_ADMINISTRATOR_PASSWORD = CSecurity.USER_ADMINISTRATOR_PASSWORD;
  public static final String USER_ADMINISTRATOR_FIRSTNAME = null;
  public static final String USER_ADMINISTRATOR_LASTNAME = CSecurity.USER_ADMINISTRATOR_NAME;
  public static final String USER_ADMINISTRATOR_DESCRIPTION = null;
  public static final Locale USER_ADMINISTRATOR_LOCALE = CSMPServer.DEFAULT_LOCALE;
  public static final ICommonsMap <String, String> USER_ADMINISTRATOR_CUSTOMATTRS = null;

  public static final int TEXT_AREA_CERT_ROWS = 10;
  public static final int TEXT_AREA_CERT_EXTENSION = 6;

  public static final boolean ENABLE_ISSUE_56 = false;

  private static String s_sAppTitle = APPLICATION_TITLE + getApplicationSuffix ();
  private static String s_sAppVersion = CSMPServer.getVersionNumber ();
  private static String s_sAuthor = AUTHOR;
  private static String s_sAuthorEmail = AUTHOR_EMAIL;

  private CSMP ()
  {}

  @NonNull
  public static String getApplicationSuffix ()
  {
    return SMPWebAppConfiguration.isTestVersion () ? " [Test version]" : "";
  }

  @NonNull
  @Nonempty
  public static String getApplicationTitle ()
  {
    return s_sAppTitle;
  }

  public static void setApplicationTitle (@NonNull @Nonempty final String s)
  {
    s_sAppTitle = s;
  }

  @NonNull
  @Nonempty
  public static String getApplicationVersion ()
  {
    return s_sAppVersion;
  }

  public static void setApplicationVersion (@NonNull @Nonempty final String s)
  {
    s_sAppVersion = s;
  }

  @NonNull
  @Nonempty
  public static String getAuthor ()
  {
    return s_sAuthor;
  }

  public static void setAuthor (@NonNull @Nonempty final String s)
  {
    s_sAuthor = s;
  }

  @NonNull
  @Nonempty
  public static String getAuthorEmail ()
  {
    return s_sAuthorEmail;
  }

  public static void setAuthorEmail (@NonNull @Nonempty final String s)
  {
    s_sAuthorEmail = s;
  }

  @NonNull
  @Nonempty
  public static String getApplicationTitleAndVersion ()
  {
    return StringHelper.getConcatenatedOnDemand (getApplicationTitle (), " ", getApplicationVersion ());
  }

  public static boolean isOldPeppolSml (@Nullable final ISMLInfo aSmlInfo)
  {
    // Old European Commission URLs - works for Prod and Test
    return aSmlInfo != null && aSmlInfo.getDNSZone ().toLowerCase (Locale.US).contains ("edelivery.tech.ec.europa.eu");
  }

  public static boolean isNewPeppolSml (@Nullable final ISMLInfo aSmlInfo)
  {
    if (aSmlInfo == null)
      return false;
    // New OpenPeppol SML - Prod and Test
    final String sDnsZone = StringHelper.trimEnd (aSmlInfo.getDNSZone ().toLowerCase (Locale.US), '.');
    return sDnsZone.equals ("participant.sml.prod.tech.peppol.org") ||
           sDnsZone.equals ("participant.sml.test.tech.peppol.org");
  }
}
