/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.app;

import org.jspecify.annotations.NonNull;

import com.helger.pd.client.PDClient;

/**
 * This exception is thrown if the {@link PDClient} (Peppol Directory client) cannot be created,
 * e.g. because the PD client configuration is incomplete or invalid.
 *
 * @author Philip Helger
 * @since 8.7.1
 */
public class PDClientCreationException extends Exception
{
  public PDClientCreationException (@NonNull final String sMsg, @NonNull final Throwable aCause)
  {
    super (sMsg, aCause);
  }
}
