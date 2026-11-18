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
package com.helger.phoss.smp.backend.sql.mgr;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.base.exception.InitializationException;
import com.helger.config.ConfigFactory;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.source.EConfigSourceType;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.resource.properties.ConfigurationSourceProperties;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.api.config.IJdbcConfiguration;
import com.helger.db.flyway.FlywayConfiguration;
import com.helger.io.resource.ClassPathResource;
import com.helger.phoss.smp.backend.sql.SMPFlywayConfigurationBuilder;
import com.helger.phoss.smp.backend.sql.SMPJdbcConfiguration;
import com.helger.phoss.smp.config.SMPConfig;
import com.helger.phoss.smp.config.SMPConfigProvider;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.mock.SMPServerTestRule;

/**
 * Test the read-only mode of the SQL backend. Requires the PostgreSQL database from
 * <code>unittest-db-docker-compose.yml</code> and is skipped if it is not available.
 *
 * @author Philip Helger
 */
public final class SMPReadOnlySQLTest
{
  private static final String PROPERTIES = "test-smp-server-sql-read-only.properties";
  private static final String PROPERTIES_EMPTY_SCHEMA = "test-smp-server-sql-read-only-empty.properties";

  private IConfigWithFallback m_aOldConfig;

  @NonNull
  private static IConfigWithFallback _createConfig (@NonNull final String sPath)
  {
    final MultiConfigurationValueProvider aVP = ConfigFactory.createDefaultValueProvider ();
    aVP.addConfigurationSource (new ConfigurationSourceProperties (new ClassPathResource (sPath),
                                                                   StandardCharsets.UTF_8),
                                EConfigSourceType.RESOURCE.getDefaultPriority () + 1);
    return new SMPConfig (aVP);
  }

  private static void _startAndStop ()
  {
    final SMPServerTestRule aRule = new SMPServerTestRule ();
    try
    {
      aRule.before ();
      assertNotNull (SMPMetaManager.getTransportProfileMgr ().getAllSMPTransportProfiles ());
    }
    finally
    {
      aRule.after ();
    }
  }

  @BeforeClass
  public static void beforeClass ()
  {
    final IConfigWithFallback aConfig = _createConfig (PROPERTIES);
    try (final Connection aConn = DriverManager.getConnection (aConfig.getAsString ("jdbc.url"),
                                                              aConfig.getAsString ("jdbc.user"),
                                                              aConfig.getAsString ("jdbc.password")))
    {
      // Database is available
    }
    catch (final SQLException ex)
    {
      Assume.assumeNoException ("PostgreSQL test database is not available", ex);
    }
  }

  @Before
  public void before ()
  {
    m_aOldConfig = SMPConfigProvider.getConfig ();
  }

  @After
  public void after ()
  {
    SMPMetaManager.setReadOnly (false);
    SMPConfigProvider.setConfig (m_aOldConfig);
  }

  @Test
  public void testReadOnlyStartupOnMigratedSchema ()
  {
    SMPConfigProvider.setConfig (_createConfig (PROPERTIES));

    // Start writable first, so that all migrations were executed
    _startAndStop ();

    // Read-only start must work on the migrated schema
    SMPMetaManager.setReadOnly (true);
    _startAndStop ();
  }

  @Test
  public void testPendingMigrationsOnEmptySchema ()
  {
    // The JDBC configuration of SMPDataSourceSingleton is static, so the check is invoked directly
    final IConfigWithFallback aConfig = _createConfig (PROPERTIES_EMPTY_SCHEMA);
    final IJdbcConfiguration aJdbcConfig = new SMPJdbcConfiguration (aConfig);
    final FlywayConfiguration aFlywayConfig = new SMPFlywayConfigurationBuilder (aConfig, aJdbcConfig).build ();

    try
    {
      FlywayMigrator.Singleton.INSTANCE.checkNoPendingMigrations (EDatabaseSystemType.POSTGRESQL,
                                                                  aJdbcConfig,
                                                                  aFlywayConfig);
      fail ("Pending migrations on an empty schema must be detected");
    }
    catch (final InitializationException ex)
    {
      assertTrue (ex.getMessage (), ex.getMessage ().contains ("Flyway migration(s) are pending"));
    }
  }
}
