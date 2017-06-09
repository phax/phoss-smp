package com.helger.peppol.smpserver.domain.smlaction;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.error.IError;
import com.helger.commons.state.ISuccessIndicator;

/**
 * Base interface for an action performed by the SMP at the SML.
 *
 * @author Philip Helger
 */
public interface ISMPSMLAction extends Serializable, ISuccessIndicator
{
  /**
   * @return The date and time when the action was performed. May not be
   *         <code>null</code>.
   */
  @Nonnull
  LocalDateTime getDateTime ();

  /**
   * @return The performed action. See the respective enum. May not be
   *         <code>null</code>.
   */
  @Nonnull
  ESMPSMLActionType getActionType ();

  /**
   * @return The ID of the performed action. See the respective enum. May
   *         neither be <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  default String getActionTypeID ()
  {
    return getActionType ().getID ();
  }

  /**
   * @return Optional error details. May be <code>null</code> for successful
   *         operations.
   * @see #isFailure()
   */
  @Nullable
  IError getError ();
}
