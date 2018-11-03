/**
 * Copyright (C) 2014-2018 Philip Helger and contributors
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
package com.helger.peppol.smpserver.exceptionmapper;

import javax.annotation.Nonnull;
import javax.ws.rs.ext.ExceptionMapper;

import com.helger.commons.lang.StackTraceHelper;

/**
 * @author Philip Helger
 * @param <E>
 *        exception type to be mapped
 */
public abstract class AbstractExceptionMapper <E extends Throwable> implements ExceptionMapper <E>
{
  @Nonnull
  public static String getResponseEntityWithoutStackTrace (@Nonnull final Throwable ex)
  {
    // The class name does not really matter
    return ex.getMessage ();
  }

  @Nonnull
  public static String getResponseEntityWithStackTrace (@Nonnull final Throwable ex)
  {
    // Includes class name and message
    return StackTraceHelper.getStackAsString (ex);
  }
}
