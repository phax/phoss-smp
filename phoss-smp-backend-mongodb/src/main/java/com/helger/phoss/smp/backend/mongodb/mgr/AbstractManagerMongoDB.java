/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.mgr;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.smp.backend.mongodb.MongoClientSingleton;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.mongodb.client.MongoCollection;

/**
 * Abstract base class for MongoDB backends
 *
 * @author Philip Helger
 */
public abstract class AbstractManagerMongoDB implements AutoCloseable
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractManagerMongoDB.class);

  private static final String BSON_SCHEME = "scheme";
  private static final String BSON_VALUE = "value";

  private final String m_sCollectionName;
  private final MongoCollection <Document> m_aCollection;

  public AbstractManagerMongoDB (@NonNull @Nonempty final String sCollectionName)
  {
    ValueEnforcer.notNull (sCollectionName, "CollectionName");
    m_sCollectionName = sCollectionName;
    m_aCollection = MongoClientSingleton.getInstance ().getCollection (sCollectionName);
  }

  @OverridingMethodsMustInvokeSuper
  public void close ()
  {}

  /**
   * @return The name of the collection as provided in the constructor. Neither <code>null</code>
   *         nor empty.
   */
  @NonNull
  @Nonempty
  public final String getCollectionName ()
  {
    return m_sCollectionName;
  }

  @NonNull
  protected final MongoCollection <Document> getCollection ()
  {
    return m_aCollection;
  }

  @NonNull
  @ReturnsMutableCopy
  public static Document toBson (@NonNull final IIdentifier aValue)
  {
    return new Document ().append (BSON_SCHEME, aValue.getScheme ()).append (BSON_VALUE, aValue.getValue ());
  }

  @Nullable
  @ReturnsMutableCopy
  public static IDocumentTypeIdentifier toDocumentTypeID (@Nullable final Document aDoc)
  {
    if (aDoc == null)
      return null;
    final String sScheme = aDoc.getString (BSON_SCHEME);
    final String sValue = aDoc.getString (BSON_VALUE);
    final var ret = SMPMetaManager.getIdentifierFactory ().createDocumentTypeIdentifier (sScheme, sValue);
    if (ret == null)
      LOGGER.warn ("Failed to parse '" + sScheme + "' and '" + sValue + "' to a document type ID");
    return ret;
  }

  @Nullable
  @ReturnsMutableCopy
  public static IParticipantIdentifier toParticipantID (@Nullable final Document aDoc)
  {
    if (aDoc == null)
      return null;
    final String sScheme = aDoc.getString (BSON_SCHEME);
    final String sValue = aDoc.getString (BSON_VALUE);
    final var ret = SMPMetaManager.getIdentifierFactory ().createParticipantIdentifier (sScheme, sValue);
    if (ret == null)
      LOGGER.warn ("Failed to parse '" + sScheme + "' and '" + sValue + "' to a participant ID");
    return ret;
  }

  @Nullable
  @ReturnsMutableCopy
  public static IProcessIdentifier toProcessID (@Nullable final Document aDoc)
  {
    if (aDoc == null)
      return null;
    final String sScheme = aDoc.getString (BSON_SCHEME);
    final String sValue = aDoc.getString (BSON_VALUE);
    final var ret = SMPMetaManager.getIdentifierFactory ().createProcessIdentifier (sScheme, sValue);
    if (ret == null)
      LOGGER.warn ("Failed to parse '" + sScheme + "' and '" + sValue + "' to a process ID");
    return ret;
  }
}
