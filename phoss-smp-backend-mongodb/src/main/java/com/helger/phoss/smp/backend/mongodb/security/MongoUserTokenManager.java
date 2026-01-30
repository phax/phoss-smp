package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.annotation.Nonempty;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.backend.mongodb.MongoClientSingleton;
import com.helger.photon.security.token.user.IUserToken;
import com.helger.photon.security.token.user.IUserTokenManager;
import com.helger.photon.security.token.user.IUserTokenModificationCallback;
import com.helger.photon.security.token.user.UserToken;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Map;

public class MongoUserTokenManager implements IUserTokenManager
{

  public static final String TOKEN_COLLECTION_NAME = "user-tokens";

  private final MongoCollection <Document> m_tokens;
  private final IUserManager m_aUserMgr;

  public MongoUserTokenManager (@NonNull IUserManager aUserMgr)
  {
    this.m_aUserMgr = aUserMgr;
    this.m_tokens = MongoClientSingleton.getInstance ().getCollection (TOKEN_COLLECTION_NAME);
  }


  @Override
  public @NonNull CallbackList <IUserTokenModificationCallback> userTokenModificationCallbacks ()
  {
    return null;
  }

  @Override
  public @Nullable UserToken createUserToken (@Nullable String sTokenString, @Nullable Map <String, String> aCustomAttrs, @NonNull IUser aUser, @Nullable String sDescription)
  {
    return null;
  }

  @Override
  public @NonNull EChange updateUserToken (@Nullable String sUserTokenID, @Nullable Map <String, String> aNewCustomAttrs, @Nullable String sNewDescription)
  {
    return null;
  }

  @Override
  public @NonNull EChange deleteUserToken (@Nullable String sUserTokenID)
  {
    return null;
  }

  @Override
  public @NonNull EChange createNewAccessToken (@Nullable String sUserTokenID, @NonNull @Nonempty String sRevocationUserID, @NonNull LocalDateTime aRevocationDT, @NonNull @Nonempty String sRevocationReason, @Nullable String sTokenString)
  {
    return null;
  }

  @Override
  public @NonNull EChange revokeAccessToken (@Nullable String sUserTokenID, @NonNull @Nonempty String sRevocationUserID, @NonNull LocalDateTime aRevocationDT, @NonNull @Nonempty String sRevocationReason)
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <IUserToken> getAllActiveUserTokens ()
  {
    return null;
  }

  @Override
  public @Nullable IUserToken getUserTokenOfID (@Nullable String sUserTokenID)
  {
    return null;
  }

  @Override
  public @Nullable IUserToken getUserTokenOfTokenString (@Nullable String sTokenString)
  {
    return null;
  }

  @Override
  public boolean isAccessTokenUsed (@Nullable String sTokenString)
  {
    return false;
  }

  @Override
  public @NonNull <T> ICommonsList <T> getNone ()
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <IUserToken> getAll ()
  {
    return null;
  }

  @Override
  public boolean containsWithID (@Nullable String sID)
  {
    return false;
  }

  @Override
  public boolean containsAllIDs (@Nullable Iterable <String> aIDs)
  {
    return false;
  }
}
