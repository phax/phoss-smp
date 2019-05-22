package com.helger.phoss.smp.backend.mongodb.mgr;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.bson.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.phoss.smp.backend.mongodb.MongoClientSingleton;
import com.mongodb.client.MongoCollection;

public abstract class AbstractManagerMongoDB implements AutoCloseable
{
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
}
