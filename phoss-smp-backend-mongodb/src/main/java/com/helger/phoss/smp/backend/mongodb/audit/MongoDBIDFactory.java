/**
 * Copyright (C) 2019-2020 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.audit;

import java.util.Date;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.id.factory.AbstractPersistingLongIDFactory;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phoss.smp.backend.mongodb.MongoClientSingleton;
import com.mongodb.client.MongoCollection;

/**
 * Implementation of a long ID factory using MongoDB
 *
 * @author Philip Helger
 * @since 5.2.1
 */
public class MongoDBIDFactory extends AbstractPersistingLongIDFactory
{
  /** The default number of values to reserve with a single IO action */
  public static final int DEFAULT_RESERVE_COUNT = 20;

  /** The default collection name if none is provided */
  public static final String DEFAULT_COLLECTION_NAME = "smp-settings";

  private static final Logger LOGGER = LoggerFactory.getLogger (MongoDBIDFactory.class);

  private static final String BSON_ID = "id";
  private static final String BSON_LONG_VALUE = "longvalue";
  private static final String VALUE_ID_LONG_ID = "long-id";

  private final String m_sCollectionName;
  private final MongoCollection <Document> m_aCollection;
  private final long m_nInitialCount;

  /**
   * Default constructor using {@link #DEFAULT_COLLECTION_NAME} as the
   * collection name.
   * 
   * @param nInitialCount
   *        Initial count to be used, if no MongoDB document exists. Must be
   *        &ge; 0.
   */
  public MongoDBIDFactory (@Nonnegative final long nInitialCount)
  {
    this (DEFAULT_COLLECTION_NAME, DEFAULT_RESERVE_COUNT, nInitialCount);
  }

  /**
   * Constructor
   *
   * @param sCollectionName
   *        Collection name to use. May neither be <code>null</code> nor empty.
   * @param nReserveCount
   *        The number of IDs to reserve per persistence layer access. Must be
   *        &gt; 0.
   * @param nInitialCount
   *        Initial count to be used, if no MongoDB document exists. Must be
   *        &ge; 0.
   */
  public MongoDBIDFactory (@Nonnull @Nonempty final String sCollectionName,
                           @Nonnegative final int nReserveCount,
                           @Nonnegative final long nInitialCount)
  {
    super (nReserveCount);
    ValueEnforcer.notEmpty (sCollectionName, "CollectionName");
    ValueEnforcer.isGE0 (nInitialCount, "InitialCount");
    m_sCollectionName = sCollectionName;
    m_aCollection = MongoClientSingleton.getInstance ().getCollection (sCollectionName);
    m_nInitialCount = nInitialCount;
  }

  @Override
  protected long readAndUpdateIDCounter (@Nonnegative final int nReserveCount)
  {
    // Existing value
    final long nRead;
    final Document aFilter = new Document (BSON_ID, VALUE_ID_LONG_ID);
    Document aDoc = m_aCollection.find (aFilter).first ();
    if (aDoc != null)
    {
      final Long aLong = aDoc.getLong (BSON_LONG_VALUE);
      nRead = aLong != null ? aLong.longValue () : m_nInitialCount;
    }
    else
      nRead = m_nInitialCount;

    final boolean bCreate = aDoc == null;
    if (bCreate)
    {
      aDoc = new Document ();
      aDoc.append (BSON_ID, VALUE_ID_LONG_ID);
    }
    aDoc.remove (BSON_LONG_VALUE);
    final long nNewValue = nRead + nReserveCount;
    aDoc.append (BSON_LONG_VALUE, Long.valueOf (nNewValue));
    aDoc.append ("last-modification", new Date ());
    if (bCreate)
      m_aCollection.insertOne (aDoc);
    else
      m_aCollection.replaceOne (aFilter, aDoc);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Updated MongoDB ID to " + nNewValue);

    return nRead;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (!super.equals (o))
      return false;
    final MongoDBIDFactory rhs = (MongoDBIDFactory) o;
    return m_sCollectionName.equals (rhs.m_sCollectionName);
  }

  @Override
  public int hashCode ()
  {
    return HashCodeGenerator.getDerived (super.hashCode ()).append (m_sCollectionName).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("CollectionName", m_sCollectionName).getToString ();
  }
}
