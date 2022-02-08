package com.helger.phoss.smp.rest;

import java.net.URI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.peppol.sml.ESMPAPIType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.smpclient.url.BDXLURLProvider;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.smpclient.url.PeppolURLProvider;
import com.helger.smpclient.url.SMPDNSResolutionException;

abstract class AbstractSMPAPIExecutorQuery extends AbstractSMPAPIExecutor
{
  protected static final class SMPQueryParams
  {
    private URI m_aSMPHostURI;

    private SMPQueryParams ()
    {}

    @Nonnull
    public URI getSMPHostURI ()
    {
      return m_aSMPHostURI;
    }

    @Nonnull
    private static ISMPURLProvider _getURLProvider (@Nonnull final ESMPAPIType eAPIType)
    {
      return eAPIType == ESMPAPIType.PEPPOL ? PeppolURLProvider.INSTANCE : BDXLURLProvider.INSTANCE;
    }

    @Nullable
    public static SMPQueryParams create (@Nonnull final ESMPAPIType eAPIType, @Nullable final IParticipantIdentifier aParticipantID)
    {
      final SMPQueryParams ret = new SMPQueryParams ();
      try
      {
        ret.m_aSMPHostURI = _getURLProvider (eAPIType).getSMPURIOfParticipant (aParticipantID,
                                                                               SMPMetaManager.getSettings ().getSMLDNSZone ());
      }
      catch (final SMPDNSResolutionException ex)
      {
        // For BDXL lookup -> no such participant
        return null;
      }
      return ret;
    }
  }

}
