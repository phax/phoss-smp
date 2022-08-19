package com.helger.phoss.smp.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.config.IConfig;
import com.helger.config.value.ConfiguredValue;

/**
 * Extended SMP Configuration interface.
 *
 * @author Philip Helger
 */
public interface ISMPConfig extends IConfig
{
  @Nullable
  ConfiguredValue getConfiguredValueOrFallback (@Nonnull String sPrimary, @Nonnull String... aOldOnes);

  @Nullable
  String getAsStringOrFallback (@Nonnull String sPrimary, @Nonnull String... aOldOnes);

  int getAsIntOrFallback (@Nonnull String sPrimary, int nBogus, int nDefault, @Nonnull String... aOldOnes);

  long getAsLongOrFallback (@Nonnull String sPrimary, long nBogus, long nDefault, @Nonnull String... aOldOnes);
}
