/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
