package com.helger.phoss.smp.exchange;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.collection.commons.ICommonsList;

@ThreadSafe
final class ImportLogger
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ImportLogger.class);

  private final List <ImportActionItem> m_aThreadSafeActionList;
  private final ImportSummary m_aImportSummary;
  private final String m_sLogPrefix;

  /**
   * Ctor
   * 
   * @param aActionList
   *        Action list to be filled. May not be <code>null</code>.
   * @param aImportSummary
   *        Summary object to be filled. May not be <code>null</code>.
   * @param nImportCount
   *        How many SG imports were run before? Each run gets its unique index
   */
  ImportLogger (@NonNull final ICommonsList <ImportActionItem> aActionList,
                @NonNull final ImportSummary aImportSummary,
                final int nImportCount)
  {
    // Make sure it is thread-safe
    m_aThreadSafeActionList = Collections.synchronizedList (aActionList);
    m_aImportSummary = aImportSummary;
    m_sLogPrefix = "[SG-IMPORT-" + nImportCount + "] ";
  }

  public void detail (@Nullable final String pi, @NonNull final String msg)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (m_sLogPrefix + (pi == null ? "" : "[" + pi + "] ") + msg);
    m_aThreadSafeActionList.add (ImportActionItem.createInfo (pi, msg));
  }

  public void success (@NonNull final String pi, @NonNull final String msg)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (m_sLogPrefix + "[" + pi + "] " + msg);
    m_aThreadSafeActionList.add (ImportActionItem.createSuccess (pi, msg));
  }

  public void info (@NonNull final String msg)
  {
    LOGGER.info (m_sLogPrefix + msg);
    m_aThreadSafeActionList.add (ImportActionItem.createInfo (null, msg));
  }

  public void warn (@NonNull final String msg)
  {
    LOGGER.info (m_sLogPrefix + msg);
    m_aThreadSafeActionList.add (ImportActionItem.createWarning (null, msg));
  }

  public void warn (@NonNull final String pi, @NonNull final String msg)
  {
    LOGGER.info (m_sLogPrefix + "[" + pi + "] " + msg);
    m_aThreadSafeActionList.add (ImportActionItem.createWarning (pi, msg));
  }

  public void error (@NonNull final String msg)
  {
    LOGGER.error (m_sLogPrefix + msg);
    m_aThreadSafeActionList.add (ImportActionItem.createError (null, msg, null));
  }

  public void error (@NonNull final String msg, @Nullable final Exception ex)
  {
    LOGGER.error (m_sLogPrefix + msg, ex);
    m_aThreadSafeActionList.add (ImportActionItem.createError (null, msg, ex));
  }

  public void error (@NonNull final String pi, @NonNull final String msg)
  {
    LOGGER.error (m_sLogPrefix + "[" + pi + "] " + msg);
    m_aThreadSafeActionList.add (ImportActionItem.createError (pi, msg, null));
  }

  public void error (@NonNull final String pi, @NonNull final String msg, @Nullable final Exception ex)
  {
    LOGGER.error (m_sLogPrefix + "[" + pi + "] " + msg, ex);
    m_aThreadSafeActionList.add (ImportActionItem.createError (pi, msg, ex));
  }

  public boolean containsAnyError ()
  {
    return m_aThreadSafeActionList.stream ().anyMatch (ImportActionItem::isError);
  }

  public void onSuccess (@NonNull final EImportSummaryAction eAction)
  {
    synchronized (m_aImportSummary)
    {
      m_aImportSummary.onSuccess (eAction);
    }
  }

  public void onError (@NonNull final EImportSummaryAction eAction)
  {
    synchronized (m_aImportSummary)
    {
      m_aImportSummary.onError (eAction);
    }
  }
}
