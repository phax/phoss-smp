/*
 * Copyright (C) 2015-2022 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.exchange;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;

/**
 * Defines the potential actions, listed in the import summary
 *
 * @author Philip Helger
 */
public enum EImportSummaryAction implements IHasID <String>
{
  DELETE_SG ("delete-servicegroup"),
  CREATE_SG ("create-servicegroup"),
  CREATE_SI ("create-serviceinfo"),
  CREATE_REDIRECT ("create-redirect"),
  CREATE_BC ("create-business-card"),
  DELETE_BC ("delete-business-card");

  private final String m_sID;

  EImportSummaryAction (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }
}
