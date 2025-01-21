/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.config;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.value.IConfigurationValueProvider;

/**
 * Special SMP configuration with fallback
 *
 * @author Philip Helger
 */
public class SMPConfig extends ConfigWithFallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPConfig.class);

  public SMPConfig (@Nonnull final IConfigurationValueProvider aValueProvider)
  {
    super (aValueProvider);
    setReplaceVariables (true);
    setOutdatedNotifier ( (sOld, sNew) -> {
      LOGGER.warn ("Please rename the configuration property '" +
                   sOld +
                   "' to '" +
                   sNew +
                   "'. Support for the old property name will be removed in the next major release.");
    });
    if (LOGGER.isDebugEnabled ())
    {
      // Print details on every lookup
      setFoundKeyConsumer ( (k, v) -> LOGGER.debug ("Found Configuration key '" +
                                                    k +
                                                    "' with value '" +
                                                    v.getValue () +
                                                    "' and prio " +
                                                    v.getConfigurationSource ().getPriority () +
                                                    " in " +
                                                    v.getConfigurationSource ().getSourceType ()));
      setKeyNotFoundConsumer (k -> LOGGER.debug ("Failed to find Configuration key '" + k + "'"));
    }
  }
}
