/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend.mongodb.mgr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.bson.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.peppol.identifier.IIdentifier;
import com.helger.peppol.identifier.IParticipantIdentifier;
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
  private static final String BSON_SCHEME = "scheme";
  private static final String BSON_VALUE = "value";

  private final String m_sCollectionName;
  private final MongoCollection <Document> m_aCollection;

  public AbstractManagerMongoDB (@Nonnull @Nonempty final String sCollectionName)
  {
    ValueEnforcer.notNull (sCollectionName, "CollectionName");
    m_sCollectionName = sCollectionName;
    m_aCollection = MongoClientSingleton.getInstance ().getCollection (sCollectionName);
  }

  @OverridingMethodsMustInvokeSuper
  public void close ()
  {}

  /**
   * @return The name of the collection as provided in the constructor. Neither
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public final String getCollectionName ()
  {
    return m_sCollectionName;
  }

  @Nonnull
  protected final MongoCollection <Document> getCollection ()
  {
    return m_aCollection;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final IIdentifier aValue)
  {
    return new Document ().append (BSON_SCHEME, aValue.getScheme ()).append (BSON_VALUE, aValue.getValue ());
  }

  @Nullable
  @ReturnsMutableCopy
  public static IParticipantIdentifier toParticipantID (@Nullable final Document aDoc)
  {
    if (aDoc == null)
      return null;
    return SMPMetaManager.getIdentifierFactory ()
                         .createParticipantIdentifier (aDoc.getString (BSON_SCHEME), aDoc.getString (BSON_VALUE));
  }
}
