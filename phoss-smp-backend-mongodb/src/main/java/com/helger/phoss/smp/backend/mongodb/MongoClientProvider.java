/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.numeric.mutable.MutableInt;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.event.ClusterDescriptionChangedEvent;
import com.mongodb.event.ClusterListener;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandSucceededEvent;

/**
 * A provider for {@link MongoCollection} instances. This class ensures, that the underlying
 * {@link MongoClient} instance is closed correctly in the {@link #close()} method. Use
 * {@link MongoClientSingleton#getInstance()} to create an instance of this class.
 *
 * @author Philip Helger
 */
public class MongoClientProvider implements AutoCloseable
{
  private static final class LoggingCommandListener implements CommandListener
  {
    private final AtomicInteger m_aTotalCount = new AtomicInteger (0);
    private final ICommonsMap <String, MutableInt> m_aCommands = new CommonsHashMap <> ();

    @Override
    public synchronized void commandSucceeded (final CommandSucceededEvent event)
    {
      final String sCommandName = event.getCommandName ();
      final int nCount = m_aCommands.computeIfAbsent (sCommandName, k -> new MutableInt (0)).inc ();
      final int nTotal = m_aTotalCount.incrementAndGet ();
      LOGGER.info ("Successfully executed '" + sCommandName + "' [" + nCount + "/" + nTotal + "]");
    }

    @Override
    public void commandFailed (final CommandFailedEvent event)
    {
      LOGGER.error ("Failed execution of command '" + event.getCommandName () + "' with id " + event.getRequestId ());
    }
  }

  private static class IsWriteable implements ClusterListener
  {
    private final AtomicBoolean m_aIsWritable = new AtomicBoolean (false);
    private final CountDownLatch m_aReadyLatch = new CountDownLatch (1);

    @Override
    public void clusterDescriptionChanged (final ClusterDescriptionChangedEvent aEvent)
    {
      if (!m_aIsWritable.get ())
      {
        if (aEvent.getNewDescription ().hasWritableServer ())
        {
          m_aIsWritable.set (true);
          m_aReadyLatch.countDown ();
          LOGGER.info ("Able to write to server");
        }
      }
      else
      {
        if (!aEvent.getNewDescription ().hasWritableServer ())
        {
          m_aIsWritable.set (false);
          LOGGER.error ("Unable to write to server");
        }
      }
    }

    boolean isWritable ()
    {
      return m_aIsWritable.get ();
    }

    void setWritable (final boolean bWritable)
    {
      m_aIsWritable.set (bWritable);
    }

    boolean awaitReady (final long nTimeoutMillis) throws InterruptedException
    {
      return m_aReadyLatch.await (nTimeoutMillis, TimeUnit.MILLISECONDS);
    }
  }

  /** Default timeout for waiting until the MongoDB server is writable (in milliseconds). */
  public static final long DEFAULT_READY_TIMEOUT_MILLIS = Duration.ofSeconds (10).toMillis ();

  public static final Integer SORT_ASCENDING = Integer.valueOf (1);
  public static final Integer SORT_DESCENDING = Integer.valueOf (-1);

  private static final Logger LOGGER = LoggerFactory.getLogger (MongoClientProvider.class);

  private final MongoClient m_aMongoClient;
  private final MongoDatabase m_aDatabase;
  private final IsWriteable m_aClusterListener = new IsWriteable ();

  public MongoClientProvider (@NonNull @Nonempty final String sConnectionString,
                              @NonNull @Nonempty final String sDBName)
  {
    this (sConnectionString, sDBName, DEFAULT_READY_TIMEOUT_MILLIS);
  }

  public MongoClientProvider (@NonNull @Nonempty final String sConnectionString,
                              @NonNull @Nonempty final String sDBName,
                              @CheckForSigned final long nReadyTimeoutMillis)
  {
    ValueEnforcer.notEmpty (sConnectionString, "ConnectionString");
    ValueEnforcer.notEmpty (sDBName, "DBName");

    final MongoClientSettings aClientSettings = MongoClientSettings.builder ()
                                                                   .applicationName ("phoss SMP")
                                                                   .applyConnectionString (new ConnectionString (sConnectionString))
                                                                   .addCommandListener (new LoggingCommandListener ())
                                                                   .applyToClusterSettings (x -> x.addClusterListener (m_aClusterListener))
                                                                   .build ();
    m_aMongoClient = MongoClients.create (aClientSettings);
    m_aDatabase = m_aMongoClient.getDatabase (sDBName);

    // Block until MongoDB is writable or timeout expires
    if (nReadyTimeoutMillis > 0)
    {
      try
      {
        if (!m_aClusterListener.awaitReady (nReadyTimeoutMillis))
        {
          LOGGER.warn ("MongoDB did not become writable within " + nReadyTimeoutMillis + " ms");
        }
      }
      catch (final InterruptedException ex)
      {
        Thread.currentThread ().interrupt ();
        LOGGER.error ("Interrupted while waiting for MongoDB to become writable");
      }
    }
  }

  public void close ()
  {
    m_aClusterListener.setWritable (false);
    StreamHelper.close (m_aMongoClient);
  }

  public boolean isDBWritable ()
  {
    return m_aClusterListener.isWritable ();
  }

  /**
   * Get the accessor to the MongoDB collection with the specified name
   *
   * @param sName
   *        Collection name. May neither be <code>null</code> nor empty.
   * @return The collection with the specified name.
   */
  @NonNull
  public MongoCollection <Document> getCollection (@NonNull @Nonempty final String sName)
  {
    ValueEnforcer.notEmpty (sName, "Name");

    return m_aDatabase.getCollection (sName);
  }
}
