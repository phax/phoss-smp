package com.helger.peppol.smpserver.data.sql.spi;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.factory.FactoryNewInstance;
import com.helger.peppol.smpserver.backend.ISMPBackendRegistrarSPI;
import com.helger.peppol.smpserver.backend.ISMPBackendRegistry;
import com.helger.peppol.smpserver.data.sql.mgr.SQLManagerProvider;

/**
 * Register the SQL backend to the global SMP backend registry.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class SQLSMPBackendRegistrarSPI implements ISMPBackendRegistrarSPI
{
  public void registerSMPBackend (@Nonnull final ISMPBackendRegistry aRegistry)
  {
    aRegistry.registerSMPBackend ("sql", FactoryNewInstance.create (SQLManagerProvider.class));
  }
}
