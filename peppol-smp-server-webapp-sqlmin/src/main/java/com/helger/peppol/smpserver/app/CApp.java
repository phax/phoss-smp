/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.app;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.locale.LocaleCache;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.smpserver.CSMPServer;

/**
 * Contains application wide constants.
 *
 * @author Philip Helger
 */
@Immutable
public final class CApp
{
  public static final Locale DEFAULT_LOCALE = LocaleCache.getInstance ().getLocale ("en", "US");

  private static final String APPLICATION_TITLE = "ph-peppol-smp-server [sqlmin]";

  private CApp ()
  {}

  @Nonnull
  @Nonempty
  public static String getApplicationTitle ()
  {
    return APPLICATION_TITLE + (AppSettings.isTestVersion () ? " [TEST]" : "");
  }

  @Nonnull
  @Nonempty
  public static String getApplicationTitleAndVersion ()
  {
    return StringHelper.getConcatenatedOnDemand (getApplicationTitle (), " ", CSMPServer.getVersionNumber ());
  }
}
