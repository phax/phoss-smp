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
package com.helger.phoss.smp.exchange;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.rt.StackTraceHelper;
import com.helger.base.string.StringHelper;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.diagnostics.error.level.EErrorLevel;
import com.helger.diagnostics.error.level.IErrorLevel;
import com.helger.diagnostics.error.level.IHasErrorLevel;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

@Immutable
public final class ImportActionItem implements IHasErrorLevel
{
  private final LocalDateTime m_aDT;
  private final EErrorLevel m_eLevel;
  private final String m_sPI;
  private final String m_sMsg;
  private final Exception m_aLinkedException;

  private ImportActionItem (@NonNull final EErrorLevel eLevel,
                            @Nullable final String sPI,
                            @NonNull @Nonempty final String sMsg,
                            @Nullable final Exception aLinkedException)
  {
    ValueEnforcer.notNull (eLevel, "Level");
    ValueEnforcer.notEmpty (sMsg, "Message");
    m_aDT = PDTFactory.getCurrentLocalDateTime ();
    m_eLevel = eLevel;
    m_sPI = sPI;
    m_sMsg = sMsg;
    m_aLinkedException = aLinkedException;
  }

  @NonNull
  public LocalDateTime getDateTime ()
  {
    return m_aDT;
  }

  @NonNull
  public EErrorLevel getErrorLevel ()
  {
    return m_eLevel;
  }

  @Nullable
  public String getParticipantID ()
  {
    return m_sPI;
  }

  public boolean hasParticipantID ()
  {
    return StringHelper.isNotEmpty (m_sPI);
  }

  @NonNull
  @Nonempty
  public String getMessage ()
  {
    return m_sMsg;
  }

  @Nullable
  public Exception getLinkedException ()
  {
    return m_aLinkedException;
  }

  public boolean hasLinkedException ()
  {
    return m_aLinkedException != null;
  }

  @NonNull
  @Nonempty
  private static String _getErrorLevelName (@NonNull final IErrorLevel aErrorLevel)
  {
    if (aErrorLevel.isGE (EErrorLevel.ERROR))
      return "error";
    if (aErrorLevel.isGE (EErrorLevel.WARN))
      return "warning";
    return "info";
  }

  @NonNull
  @Nonempty
  public String getErrorLevelName ()
  {
    return _getErrorLevelName (m_eLevel);
  }

  @NonNull
  public IMicroElement getAsMicroElement (@NonNull @Nonempty final String sElementName)
  {
    final IMicroElement eAction = new MicroElement (sElementName);
    eAction.setAttribute ("datetime", PDTWebDateHelper.getAsStringXSD (m_aDT));
    eAction.setAttribute ("level", getErrorLevelName ());
    eAction.setAttribute ("participantID", m_sPI);
    eAction.addElement ("message").addText (m_sMsg);
    if (m_aLinkedException != null)
      eAction.addElement ("exception").addText (StackTraceHelper.getStackAsString (m_aLinkedException));
    return eAction;
  }

  @NonNull
  public IJsonObject getAsJsonObject ()
  {
    return new JsonObject ().add ("datetime", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format (m_aDT))
                            .add ("level", getErrorLevelName ())
                            .addIfNotNull ("participantID", m_sPI)
                            .add ("message", m_sMsg)
                            .addIfNotNull ("exception",
                                           m_aLinkedException != null ? StackTraceHelper.getStackAsString (
                                                                                                           m_aLinkedException)
                                                                      : null);
  }

  @NonNull
  public static ImportActionItem createSuccess (@NonNull @Nonempty final String sPI,
                                                @NonNull @Nonempty final String sMsg)
  {
    return new ImportActionItem (EErrorLevel.SUCCESS, sPI, sMsg, null);
  }

  @NonNull
  public static ImportActionItem createInfo (@Nullable final String sPI, @NonNull @Nonempty final String sMsg)
  {
    return new ImportActionItem (EErrorLevel.INFO, sPI, sMsg, null);
  }

  @NonNull
  public static ImportActionItem createWarning (@Nullable final String sPI, @NonNull @Nonempty final String sMsg)
  {
    return new ImportActionItem (EErrorLevel.WARN, sPI, sMsg, null);
  }

  @NonNull
  public static ImportActionItem createError (@Nullable final String sPI,
                                              @NonNull @Nonempty final String sMsg,
                                              @Nullable final Exception ex)
  {
    return new ImportActionItem (EErrorLevel.ERROR, sPI, sMsg, ex);
  }
}
