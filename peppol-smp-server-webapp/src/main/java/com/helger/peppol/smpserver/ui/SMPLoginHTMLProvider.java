package com.helger.peppol.smpserver.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.css.ECSSUnit;
import com.helger.css.property.CCSSProperties;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.textlevel.HCSmall;
import com.helger.peppol.smpserver.app.CApp;
import com.helger.photon.basic.auth.credentials.ICredentialValidationResult;
import com.helger.photon.bootstrap3.base.BootstrapContainer;
import com.helger.photon.bootstrap3.grid.BootstrapRow;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapLoginHTMLProvider;
import com.helger.photon.core.app.context.ISimpleWebExecutionContext;

public final class SMPLoginHTMLProvider extends BootstrapLoginHTMLProvider
{
  public SMPLoginHTMLProvider (final boolean bLoginError,
                               @Nonnull final ICredentialValidationResult aLoginResult,
                               @Nullable final IHCNode aPageTitle)
  {
    super (bLoginError, aLoginResult, aPageTitle);
  }

  @Override
  protected void onAfterContainer (@Nonnull final ISimpleWebExecutionContext aSWEC,
                                   @Nonnull final BootstrapContainer aContainer,
                                   @Nonnull final BootstrapRow aRow,
                                   @Nonnull final HCDiv aContentCol)
  {
    // Add the version number in the login screen
    aContentCol.addChild (new HCDiv ().addStyle (CCSSProperties.MARGIN_TOP.newValue (ECSSUnit.em (1)))
                                      .addChild (new HCSmall ().addChild (CApp.getApplicationTitleAndVersion ())));
  }
}
