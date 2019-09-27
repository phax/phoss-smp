/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
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
package com.helger.phoss.smp.nicename;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.string.StringHelper;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.serialize.MicroReader;

public final class NiceNameHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (NiceNameHandler.class);
  private static final SimpleReadWriteLock RWLOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RWLOCK")
  private static ICommonsOrderedMap <String, NiceNameEntry> DOCTYPE_IDS = new CommonsLinkedHashMap <> ();
  @GuardedBy ("RWLOCK")
  private static ICommonsOrderedMap <String, NiceNameEntry> PROCESS_IDS = new CommonsLinkedHashMap <> ();

  static
  {
    reloadNames ();
  }

  private NiceNameHandler ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsOrderedMap <String, NiceNameEntry> readEntries (@Nonnull final IReadableResource aRes)
  {
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Trying to read nice name entries from " + aRes.getPath ());

    final ICommonsOrderedMap <String, NiceNameEntry> ret = new CommonsLinkedHashMap <> ();
    final IMicroDocument aDoc = MicroReader.readMicroXML (aRes);
    if (aDoc != null && aDoc.getDocumentElement () != null)
    {
      for (final IMicroElement eChild : aDoc.getDocumentElement ().getAllChildElements ("item"))
      {
        final String sID = eChild.getAttributeValue ("id");
        final String sName = eChild.getAttributeValue ("name");
        final boolean bDeprecated = eChild.getAttributeValueAsBool ("deprecated", false);
        ret.put (sID, new NiceNameEntry (sName, bDeprecated));
      }
    }
    return ret;
  }

  public static void reloadNames ()
  {
    // Doc types
    {
      IReadableResource aDocTypeIDRes = null;
      final String sPath = SMPWebAppConfiguration.getConfigFile ().getAsString ("webapp.nicename.doctypes.path");
      if (StringHelper.hasText (sPath))
      {
        aDocTypeIDRes = new FileSystemResource (sPath);
        if (!aDocTypeIDRes.exists ())
          LOGGER.warn ("The configured nice name file '" + sPath + "' does not exist");
      }
      if (aDocTypeIDRes == null)
        aDocTypeIDRes = new ClassPathResource ("codelists/doctypeid-mapping.xml");
      final ICommonsOrderedMap <String, NiceNameEntry> aDocTypeIDs = readEntries (aDocTypeIDRes);
      RWLOCK.writeLocked ( () -> DOCTYPE_IDS = aDocTypeIDs);
    }

    // Processes
    {
      IReadableResource aProcessIDRes = null;
      final String sPath = SMPWebAppConfiguration.getConfigFile ().getAsString ("webapp.nicename.processes.path");
      if (StringHelper.hasText (sPath))
      {
        aProcessIDRes = new FileSystemResource (sPath);
        if (!aProcessIDRes.exists ())
          LOGGER.warn ("The configured nice name file '" + sPath + "' does not exist");
      }
      if (aProcessIDRes == null)
        aProcessIDRes = new ClassPathResource ("codelists/processid-mapping.xml");
      final ICommonsOrderedMap <String, NiceNameEntry> aProcessIDs = readEntries (aProcessIDRes);
      RWLOCK.writeLocked ( () -> PROCESS_IDS = aProcessIDs);
    }
  }

  @Nullable
  public static NiceNameEntry getDocTypeNiceName (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;
    RWLOCK.readLock ().lock ();
    try
    {
      return DOCTYPE_IDS.get (sID);
    }
    finally
    {
      RWLOCK.readLock ().unlock ();
    }
  }

  @Nullable
  public static NiceNameEntry getProcessNiceName (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;
    RWLOCK.readLock ().lock ();
    try
    {
      return PROCESS_IDS.get (sID);
    }
    finally
    {
      RWLOCK.readLock ().unlock ();
    }
  }
}
