package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.scope.mgr.ScopeManager;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.impl.GlobalWebScope;
import junit.framework.TestCase;

public abstract class MongoBaseTest extends TestCase
{
  static
  {
    if (!ScopeManager.isGlobalScopePresent ())
    {
      ScopeManager.setGlobalScope (new GlobalWebScope (MockServletContext.create ()));

    }
  }


}