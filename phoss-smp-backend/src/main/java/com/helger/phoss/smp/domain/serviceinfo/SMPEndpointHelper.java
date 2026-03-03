package com.helger.phoss.smp.domain.serviceinfo;

import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.datetime.format.PDTToString;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.period.LocalDatePeriod;

/**
 * Helper class to deal with specific
 * 
 * @author Philip Helger
 */
@Immutable
public final class SMPEndpointHelper
{
  private static final LocalDate DATE_MIN = PDTFactory.createLocalDate (0, Month.JANUARY, 1);
  private static final LocalDate DATE_MAX = PDTFactory.createLocalDate (9999, Month.DECEMBER, 31);

  private SMPEndpointHelper ()
  {}

  @NonNull
  public static LocalDatePeriod createSafePeriod (@Nullable final LocalDate aNotBeforeDate,
                                                  @Nullable final LocalDate aNotAfterDate)
  {
    // The LocalDatePeriod handles null differently from what we expect
    return new LocalDatePeriod (aNotBeforeDate != null ? aNotBeforeDate : DATE_MIN,
                                aNotAfterDate != null ? aNotAfterDate : DATE_MAX);
  }

  @Nullable
  public static String getAsValidityString (@Nullable final LocalDate aNotBefore,
                                            @Nullable final LocalDate aNotAfter,
                                            @NonNull final Locale aDisplayLocale)
  {
    if (aNotBefore == null && aNotAfter == null)
      return null;

    String ret;
    if (aNotBefore == null)
      ret = "[since forever]";
    else
      ret = PDTToString.getAsString (aNotBefore, aDisplayLocale);

    if (aNotBefore != null && aNotAfter != null && aNotBefore.equals (aNotAfter))
    {
      // Only valid on that one day - no need to add an end date
    }
    else
    {
      ret += " - ";
      if (aNotAfter == null)
        ret += "[until eternity]";
      else
        ret += PDTToString.getAsString (aNotAfter, aDisplayLocale);
    }
    return ret;
  }

  @NonNull
  @Nonempty
  public static String createUniqueEndpointID ()
  {
    return GlobalIDFactory.getNewPersistentStringID ();
  }
}
