/*
 * Copyright (C) 2015-2024 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.exchange;

import javax.annotation.Nonnegative;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A single item for the Import summary
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class ImportSummaryItem
{
  private int m_nSuccess = 0;
  private int m_nError = 0;

  public ImportSummaryItem ()
  {}

  @Nonnegative
  public int getSuccessCount ()
  {
    return m_nSuccess;
  }

  public void incSuccess ()
  {
    m_nSuccess++;
  }

  @Nonnegative
  public int getErrorCount ()
  {
    return m_nError;
  }

  public void incError ()
  {
    m_nError++;
  }
}
