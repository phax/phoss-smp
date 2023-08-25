/*
 * Copyright (C) 2014-2023 Philip Helger and contributors
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
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.string.StringHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.peppol.doctype.IPeppolDocumentTypeIdentifierParts;
import com.helger.peppolid.peppol.doctype.PeppolDocumentTypeIdentifierParts;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.serialize.MicroReader;

public final class NiceNameHandler
{
  private static final String PREFIX_WILDCARD = PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_PEPPOL_DOCTYPE_WILDCARD +
                                                "::";
  private static final Logger LOGGER = LoggerFactory.getLogger (NiceNameHandler.class);

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static final ICommonsOrderedMap <String, NiceNameEntry> DOCTYPE_IDS = new CommonsLinkedHashMap <> ();
  @GuardedBy ("RW_LOCK")
  private static final ICommonsOrderedMap <String, NiceNameEntry> PROCESS_IDS = new CommonsLinkedHashMap <> ();

  static
  {
    reloadNames ();
  }

  private NiceNameHandler ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsOrderedMap <String, NiceNameEntry> readEntries (@Nonnull final IReadableResource aRes,
                                                                        final boolean bReadProcIDs)
  {
    LOGGER.info ("Trying to read nice name entries from '" + aRes.getPath () + "'");

    final ICommonsOrderedMap <String, NiceNameEntry> ret = new CommonsLinkedHashMap <> ();
    final IMicroDocument aDoc = MicroReader.readMicroXML (aRes);
    if (aDoc != null && aDoc.getDocumentElement () != null)
    {
      for (final IMicroElement eChild : aDoc.getDocumentElement ().getAllChildElements ("item"))
      {
        String sID = eChild.getAttributeValue ("id");
        final String sName = eChild.getAttributeValue ("name");
        final boolean bDeprecated = eChild.getAttributeValueAsBool ("deprecated", false);
        ICommonsList <IProcessIdentifier> aProcIDs = null;
        if (bReadProcIDs)
        {
          aProcIDs = new CommonsArrayList <> ();
          for (final IMicroElement eItem : eChild.getAllChildElements ("procid"))
            aProcIDs.add (new SimpleProcessIdentifier (eItem.getAttributeValue ("scheme"),
                                                       eItem.getAttributeValue ("value")));
        }

        String sSpecialLabel = null;
        if (sID.startsWith (PREFIX_WILDCARD))
        {
          // When loading wildcards, a special handling is needed
          // Because the identifiers in the codelist are without "*" we need to
          // add the "*" here, because the SMP entries need the "*" to be
          // correct
          sSpecialLabel = "Wildcard";

          final IDocumentTypeIdentifier aDT = SimpleIdentifierFactory.INSTANCE.parseDocumentTypeIdentifier (sID);
          final IPeppolDocumentTypeIdentifierParts aParts = PeppolDocumentTypeIdentifierParts.extractFromIdentifier (aDT);
          if (aParts != null)
          {
            // Add the "*" to the Customization ID for the SMP
            final PeppolDocumentTypeIdentifierParts aStarParts = new PeppolDocumentTypeIdentifierParts (aParts.getRootNS (),
                                                                                                        aParts.getLocalName (),
                                                                                                        aParts.getCustomizationID () +
                                                                                                                                "*",
                                                                                                        aParts.getVersion ());
            sID = PREFIX_WILDCARD + aStarParts.getAsDocumentTypeIdentifierValue ();
          }
        }

        ret.put (sID, new NiceNameEntry (sName, bDeprecated, sSpecialLabel, aProcIDs));
      }
    }
    return ret;
  }

  public static void reloadNames ()
  {
    // Doc types
    {
      IReadableResource aDocTypeIDRes = null;
      final String sPath = SMPWebAppConfiguration.getNiceNameDocTypesPath ();
      if (StringHelper.hasText (sPath))
      {
        aDocTypeIDRes = new FileSystemResource (sPath);
        if (!aDocTypeIDRes.exists ())
        {
          LOGGER.warn ("The configured document type nice name file '" + sPath + "' does not exist");
          // Enforce defaults
          aDocTypeIDRes = null;
        }
      }
      // Use defaults
      if (aDocTypeIDRes == null)
        aDocTypeIDRes = new ClassPathResource ("codelists/smp/doctypeid-mapping.xml");

      final ICommonsOrderedMap <String, NiceNameEntry> aDocTypeIDs = readEntries (aDocTypeIDRes, true);
      RW_LOCK.writeLocked ( () -> DOCTYPE_IDS.setAll (aDocTypeIDs));
      LOGGER.info ("Loaded " + aDocTypeIDs.size () + " document type nice name entries");
    }
    // Processes
    {
      IReadableResource aProcessIDRes = null;
      final String sPath = SMPWebAppConfiguration.getNiceNameProcessesPath ();
      if (StringHelper.hasText (sPath))
      {
        aProcessIDRes = new FileSystemResource (sPath);
        if (!aProcessIDRes.exists ())
        {
          LOGGER.warn ("The configured process nice name file '" + sPath + "' does not exist");
          // Enforce defaults
          aProcessIDRes = null;
        }
      }
      // Use defaults
      if (aProcessIDRes == null)
        aProcessIDRes = new ClassPathResource ("codelists/smp/processid-mapping.xml");

      final ICommonsOrderedMap <String, NiceNameEntry> aProcessIDs = readEntries (aProcessIDRes, false);
      RW_LOCK.writeLocked ( () -> PROCESS_IDS.setAll (aProcessIDs));
      LOGGER.info ("Loaded " + aProcessIDs.size () + " process nice name entries");
    }
  }

  @Nullable
  public static NiceNameEntry getDocTypeNiceName (@Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    return aDocTypeID == null ? null : getDocTypeNiceName (aDocTypeID.getURIEncoded ());
  }

  @Nullable
  public static NiceNameEntry getDocTypeNiceName (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;

    RW_LOCK.readLock ().lock ();
    try
    {
      return DOCTYPE_IDS.get (sID);
    }
    finally
    {
      RW_LOCK.readLock ().unlock ();
    }
  }

  @Nullable
  public static NiceNameEntry getProcessNiceName (@Nullable final IProcessIdentifier aProcessID)
  {
    return aProcessID == null ? null : getProcessNiceName (aProcessID.getURIEncoded ());
  }

  @Nullable
  public static NiceNameEntry getProcessNiceName (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;

    RW_LOCK.readLock ().lock ();
    try
    {
      return PROCESS_IDS.get (sID);
    }
    finally
    {
      RW_LOCK.readLock ().unlock ();
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsOrderedMap <String, NiceNameEntry> getAllDocumentTypeMappings ()
  {
    return RW_LOCK.readLockedGet (DOCTYPE_IDS::getClone);
  }

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsOrderedMap <String, NiceNameEntry> getAllProcessMappings ()
  {
    return RW_LOCK.readLockedGet (PROCESS_IDS::getClone);
  }
}
