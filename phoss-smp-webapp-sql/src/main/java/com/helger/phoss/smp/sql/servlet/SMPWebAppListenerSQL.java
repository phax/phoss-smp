/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.sql.servlet;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.base.string.StringParser;
import com.helger.io.file.SimpleFileIO;
import com.helger.phoss.smp.backend.sql.mgr.SMPIDFactoryJDBC;
import com.helger.phoss.smp.servlet.SMPWebAppListener;
import com.helger.photon.io.WebFileIO;

/**
 * Special SMP web app listener for the SQL backend
 *
 * @author Philip Helger
 */
public class SMPWebAppListenerSQL extends SMPWebAppListener
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPWebAppListenerSQL.class);

  @Override
  protected void initGlobalIDFactory ()
  {
    // Check if an old value is present
    final long nInitialCount;
    final File aOldFile = WebFileIO.getDataIO ().getFile (ID_FILENAME);
    if (aOldFile.exists ())
    {
      final String sContent = SimpleFileIO.getFileAsString (aOldFile, StandardCharsets.ISO_8859_1);
      nInitialCount = sContent != null ? StringParser.parseLong (sContent.trim (), 0) : 0;
      LOGGER.info ("Using " + nInitialCount + " as the based ID for SMPIDFactoryJDBC");
    }
    else
      nInitialCount = 0;

    // Set persistent ID provider: SQL based
    GlobalIDFactory.setPersistentLongIDFactory (new SMPIDFactoryJDBC (nInitialCount));
    GlobalIDFactory.setPersistentIntIDFactory ( () -> (int) GlobalIDFactory.getNewPersistentLongID ());
  }
}
