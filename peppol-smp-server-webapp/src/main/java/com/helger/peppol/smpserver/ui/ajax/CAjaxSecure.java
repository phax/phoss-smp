/**
 * Copyright (C) 2012-2016 winenet GmbH - www.winenet.at
 * All Rights Reserved
 *
 * This file is part of the winenet-Kellerbuch software.
 *
 * Proprietary and confidential.
 *
 * Unauthorized copying of this file, via any medium is
 * strictly prohibited.
 */
package com.helger.peppol.smpserver.ui.ajax;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.photon.core.ajax.IAjaxFunctionDeclaration;
import com.helger.photon.core.ajax.IAjaxInvoker;
import com.helger.photon.core.ajax.decl.SecureApplicationAjaxFunctionDeclaration;

/**
 * This class registers the available ajax functions
 *
 * @author Philip Helger
 */
@Immutable
public final class CAjaxSecure
{
  public static final IAjaxFunctionDeclaration FUNCTION_CREATE_BUSINESS_CARD_ENTITY_INPUT = new SecureApplicationAjaxFunctionDeclaration ("createBusinessCardEntityInput",
                                                                                                                                          AjaxExecutorSecureCreateBusinessCardEntityInput.class);
  public static final IAjaxFunctionDeclaration FUNCTION_CREATE_BUSINESS_CARD_IDENTIFIER_INPUT = new SecureApplicationAjaxFunctionDeclaration ("createBusinessCardIdentifierInput",
                                                                                                                                              AjaxExecutorSecureCreateBusinessCardIdentifierInput.class);

  private CAjaxSecure ()
  {}

  public static void init (@Nonnull final IAjaxInvoker aAjaxInvoker)
  {
    aAjaxInvoker.registerFunction (FUNCTION_CREATE_BUSINESS_CARD_ENTITY_INPUT);
    aAjaxInvoker.registerFunction (FUNCTION_CREATE_BUSINESS_CARD_IDENTIFIER_INPUT);
  }
}
