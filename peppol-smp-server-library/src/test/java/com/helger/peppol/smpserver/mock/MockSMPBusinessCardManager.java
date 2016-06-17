package com.helger.peppol.smpserver.mock;

import java.util.List;

import com.helger.commons.collection.ext.ICommonsCollection;
import com.helger.commons.state.EChange;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardEntity;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * Mock implementation of {@link ISMPBusinessCardManager}.
 * 
 * @author Philip Helger
 */
final class MockSMPBusinessCardManager implements ISMPBusinessCardManager
{
  public ISMPBusinessCard getSMPBusinessCardOfServiceGroup (final ISMPServiceGroup aServiceGroup)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPBusinessCard getSMPBusinessCardOfID (final String sID)
  {
    throw new UnsupportedOperationException ();
  }

  public int getSMPBusinessCardCount ()
  {
    return 0;
  }

  public ICommonsCollection <? extends ISMPBusinessCard> getAllSMPBusinessCards ()
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteSMPBusinessCard (final ISMPBusinessCard aSMPBusinessCard)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPBusinessCard createOrUpdateSMPBusinessCard (final ISMPServiceGroup aServiceGroup,
                                                         final List <SMPBusinessCardEntity> aEntities)
  {
    throw new UnsupportedOperationException ();
  }
}
