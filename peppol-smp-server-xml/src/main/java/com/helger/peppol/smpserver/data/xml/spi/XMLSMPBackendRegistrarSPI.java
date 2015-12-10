package com.helger.peppol.smpserver.data.xml.spi;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.factory.FactoryNewInstance;
import com.helger.peppol.smpserver.backend.ISMPBackendRegistrarSPI;
import com.helger.peppol.smpserver.backend.ISMPBackendRegistry;
import com.helger.peppol.smpserver.data.xml.mgr.XMLManagerProvider;

/**
 * Register the XML backend to the global SMP backend registry.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class XMLSMPBackendRegistrarSPI implements ISMPBackendRegistrarSPI
{
  public void registerSMPBackend (@Nonnull final ISMPBackendRegistry aRegistry)
  {
    aRegistry.registerSMPBackend ("xml", FactoryNewInstance.create (XMLManagerProvider.class));
  }
}
