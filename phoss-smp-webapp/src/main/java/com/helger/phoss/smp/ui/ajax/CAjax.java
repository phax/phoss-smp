/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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
package com.helger.phoss.smp.ui.ajax;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.http.EHttpMethod;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.photon.ajax.IAjaxRegistry;
import com.helger.photon.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTables;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTablesI18N;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * This class defines the available ajax functions for the public application.
 *
 * @author Philip Helger
 */
@Immutable
public final class CAjax
{
  public static final IAjaxFunctionDeclaration DATATABLES = AjaxFunctionDeclaration.builder ("dataTables")
                                                                                   .executor (AjaxExecutorDataTables.class)
                                                                                   .build ();
  public static final IAjaxFunctionDeclaration DATATABLES_I18N = AjaxFunctionDeclaration.builder ("datatables-i18n")
                                                                                        .executor (new AjaxExecutorDataTablesI18N (CSMPServer.DEFAULT_LOCALE))
                                                                                        .build ();
  private static final Predicate <? super IRequestWebScopeWithoutResponse> FILTER_HTTP_POST = x -> x.getHttpMethod () == EHttpMethod.POST;
  public static final IAjaxFunctionDeclaration LOGIN = AjaxFunctionDeclaration.builder ("login")
                                                                              .filter (FILTER_HTTP_POST.and (x -> SMPWebAppConfiguration.isPublicLoginEnabled ()))
                                                                              .executor (AjaxExecutorPublicLogin.class)
                                                                              .build ();
  private static final Predicate <? super IRequestWebScopeWithoutResponse> FILTER_LOGIN = x -> LoggedInUserManager.getInstance ()
                                                                                                                  .isUserLoggedInInCurrentSession ();
  public static final IAjaxFunctionDeclaration FUNCTION_EXPORT_ALL_SERVICE_GROUPS = AjaxFunctionDeclaration.builder ("exportAllServiceGroups")
                                                                                                           .executor (AjaxExecutorSecureExportAllServiceGroups.class)
                                                                                                           .filter (FILTER_LOGIN)
                                                                                                           .build ();
  public static final IAjaxFunctionDeclaration FUNCTION_BACKEND_CONNECTION_RESET = AjaxFunctionDeclaration.builder ("backendConnectionReset")
                                                                                                          .executor (AjaxExecutorSecureBackendConnectionReset.class)
                                                                                                          .filter (FILTER_LOGIN)
                                                                                                          .build ();

  private static final Logger LOGGER = LoggerFactory.getLogger (CAjax.class);

  private CAjax ()
  {}

  public static void init (@Nonnull final IAjaxRegistry aAjaxRegistry)
  {
    aAjaxRegistry.registerFunction (DATATABLES);
    aAjaxRegistry.registerFunction (DATATABLES_I18N);
    aAjaxRegistry.registerFunction (LOGIN);
    aAjaxRegistry.registerFunction (FUNCTION_EXPORT_ALL_SERVICE_GROUPS);
    aAjaxRegistry.registerFunction (FUNCTION_BACKEND_CONNECTION_RESET);
    LOGGER.info ("Successfully registered the Ajax functions");
  }
}
