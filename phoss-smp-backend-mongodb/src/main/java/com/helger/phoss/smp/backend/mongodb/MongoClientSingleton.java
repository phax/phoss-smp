package com.helger.phoss.smp.backend.mongodb;

import javax.annotation.Nonnull;

import org.bson.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.scope.IScope;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;
import com.mongodb.client.MongoCollection;

public class MongoClientSingleton extends AbstractGlobalWebSingleton
{
  private MongoClientProvider m_aProvider;

  @Deprecated
  @UsedViaReflection
  public MongoClientSingleton ()
  {}

  @Override
  protected void onAfterInstantiation (final IScope aScope)
  {
    // TODO make customizable
    final String sConnectionString = "mongodb://localhost";
    final String sDBName = "phoss-smp";
    m_aProvider = new MongoClientProvider (sConnectionString, sDBName);
  }

  @Nonnull
  public static MongoClientSingleton getInstance ()
  {
    return getGlobalSingleton (MongoClientSingleton.class);
  }

  @Nonnull
  public MongoCollection <Document> getCollection (@Nonnull @Nonempty final String sCollectionName)
  {
    return m_aProvider.getCollection (sCollectionName);
  }
}
