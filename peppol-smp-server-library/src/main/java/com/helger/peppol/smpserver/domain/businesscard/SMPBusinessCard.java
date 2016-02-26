
package com.helger.peppol.smpserver.domain.businesscard;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.pd.businesscard.PDBusinessCardType;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * A single business card.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPBusinessCard implements ISMPBusinessCard
{
  private final String m_sID;
  private final ISMPServiceGroup m_aServiceGroup;
  private final List <SMPBusinessCardEntity> m_aEntities = new ArrayList <> ();

  public SMPBusinessCard (@Nonnull final ISMPServiceGroup aServiceGroup)
  {
    m_aServiceGroup = ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    m_sID = m_aServiceGroup.getID ();
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public ISMPServiceGroup getServiceGroup ()
  {
    return m_aServiceGroup;
  }

  @Nonnull
  @Nonempty
  public String getServiceGroupID ()
  {
    return m_aServiceGroup.getID ();
  }

  /**
   * @return A mutable list with all {@link SMPBusinessCardEntity} objects.
   *         Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject ("design")
  public List <SMPBusinessCardEntity> getEntities ()
  {
    return m_aEntities;
  }

  @Nonnull
  public PDBusinessCardType getAsJAXBObject ()
  {
    final PDBusinessCardType ret = new PDBusinessCardType ();
    ret.setParticipantIdentifier (SMPBusinessCardIdentifier.getAsJAXBObject (m_aServiceGroup.getParticpantIdentifier ()
                                                                                            .getScheme (),
                                                                             m_aServiceGroup.getParticpantIdentifier ()
                                                                                            .getValue ()));
    for (final SMPBusinessCardEntity aItem : m_aEntities)
      ret.addBusinessEntity (aItem.getAsJAXBObject ());
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPBusinessCard rhs = (SMPBusinessCard) o;
    return m_aServiceGroup.equals (rhs.m_aServiceGroup) && m_aEntities.equals (rhs.m_aEntities);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aServiceGroup).append (m_aEntities).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("serviceGroup", m_aServiceGroup)
                                       .append ("entities", m_aEntities)
                                       .toString ();
  }
}
