package com.helger.phoss.smp.backend.sql.mgr;

import javax.annotation.Nonnegative;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.id.factory.AbstractPersistingLongIDFactory;
import com.helger.commons.mutable.MutableLong;
import com.helger.commons.string.StringParser;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.phoss.smp.backend.sql.SMPDBExecutor;

public class SMPIDFactoryJDBC extends AbstractPersistingLongIDFactory
{
  /** The default number of values to reserve with a single IO action */
  public static final int DEFAULT_RESERVE_COUNT = 20;

  /** The ID of the key column in the "smp-settings" table */
  public static final String SETTINGS_KEY_LATEST_ID = "latest-id";

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPIDFactoryJDBC.class);

  private final long m_nInitialCount;

  /**
   * Constructor
   *
   * @param nInitialCount
   *        The count to be used if no database entry is available. This is
   *        purely for migrating existing counter from file based to DB based.
   */
  public SMPIDFactoryJDBC (@Nonnegative final long nInitialCount)
  {
    super (DEFAULT_RESERVE_COUNT);
    ValueEnforcer.isGE0 (nInitialCount, "InitialCount");
    m_nInitialCount = nInitialCount;
  }

  @Override
  protected long readAndUpdateIDCounter (@Nonnegative final int nReserveCount)
  {
    final MutableLong aReadValue = new MutableLong (0);

    final DBExecutor aExecutor = new SMPDBExecutor ();
    aExecutor.performInTransaction ( () -> {
      // Read existing value
      final String sExistingValue = SMPSettingsManagerJDBC.getSettingsValue (aExecutor, SETTINGS_KEY_LATEST_ID);
      final long nRead = StringParser.parseLong (sExistingValue, m_nInitialCount);
      aReadValue.set (nRead);

      // Write new value
      final long nNewValue = nRead + nReserveCount;
      SMPSettingsManagerJDBC.setSettingsValue (aExecutor, SETTINGS_KEY_LATEST_ID, Long.toString (nNewValue));

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Updated SQL ID from " + sExistingValue + " to " + nNewValue);
    });

    return aReadValue.longValue ();
  }
}
