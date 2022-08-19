package com.helger.phoss.smp.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.string.StringHelper;
import com.helger.config.Config;
import com.helger.config.value.ConfiguredValue;
import com.helger.config.value.IConfigurationValueProvider;

public class SMPConfig extends Config implements ISMPConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPConfig.class);

  public SMPConfig (@Nonnull final IConfigurationValueProvider aValueProvider)
  {
    super (aValueProvider);
  }

  private static void _logRenamedConfig (@Nonnull final String sOld, @Nonnull final String sNew)
  {
    if (LOGGER.isWarnEnabled ())
      LOGGER.warn ("Please rename the configuration property '" +
                   sOld +
                   "' to '" +
                   sNew +
                   "'. Support for the old property name will be removed in the next major release.");
  }

  @Nullable
  public ConfiguredValue getConfiguredValueOrFallback (@Nonnull final String sPrimary,
                                                       @Nonnull final String... aOldOnes)
  {
    ConfiguredValue ret = getConfiguredValue (sPrimary);
    if (ret == null)
    {
      // Try the old names
      for (final String sOld : aOldOnes)
      {
        ret = getConfiguredValue (sOld);
        if (ret != null)
        {
          // Notify on old name usage
          _logRenamedConfig (sOld, sPrimary);
          break;
        }
      }
    }
    return ret;
  }

  @Nullable
  public String getAsStringOrFallback (@Nonnull final String sPrimary, @Nonnull final String... aOldOnes)
  {
    String ret = getAsString (sPrimary);
    if (StringHelper.hasNoText (ret))
    {
      // Try the old names
      for (final String sOld : aOldOnes)
      {
        ret = getAsString (sOld);
        if (StringHelper.hasText (ret))
        {
          // Notify on old name usage
          _logRenamedConfig (sOld, sPrimary);
          break;
        }
      }
    }
    return ret;
  }

  public int getAsIntOrFallback (@Nonnull final String sPrimary,
                                 final int nBogus,
                                 final int nDefault,
                                 @Nonnull final String... aOldOnes)
  {
    int ret = getAsInt (sPrimary, nBogus);
    if (ret == nBogus)
    {
      // Try the old names
      for (final String sOld : aOldOnes)
      {
        ret = getAsInt (sOld, nBogus);
        if (ret != nBogus)
        {
          // Notify on old name usage
          _logRenamedConfig (sOld, sPrimary);
          break;
        }
      }
    }
    return ret == nBogus ? nDefault : ret;
  }

  public long getAsLongOrFallback (@Nonnull final String sPrimary,
                                   final long nBogus,
                                   final long nDefault,
                                   @Nonnull final String... aOldOnes)
  {
    long ret = getAsLong (sPrimary, nBogus);
    if (ret == nBogus)
    {
      // Try the old names
      for (final String sOld : aOldOnes)
      {
        ret = getAsLong (sOld, nBogus);
        if (ret != nBogus)
        {
          // Notify on old name usage
          _logRenamedConfig (sOld, sPrimary);
          break;
        }
      }
    }
    return ret == nBogus ? nDefault : ret;
  }
}
