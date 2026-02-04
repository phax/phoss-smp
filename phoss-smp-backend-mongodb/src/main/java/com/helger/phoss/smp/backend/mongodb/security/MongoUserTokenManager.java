package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.annotation.Nonempty;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.security.token.accesstoken.AccessToken;
import com.helger.photon.security.token.object.AccessTokenList;
import com.helger.photon.security.token.object.IAccessTokenList;
import com.helger.photon.security.token.revocation.IRevocationStatus;
import com.helger.photon.security.token.revocation.RevocationStatus;
import com.helger.photon.security.token.user.IUserToken;
import com.helger.photon.security.token.user.IUserTokenManager;
import com.helger.photon.security.token.user.IUserTokenModificationCallback;
import com.helger.photon.security.token.user.UserToken;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MongoUserTokenManager extends AbstractMongoManager <IUserToken> implements IUserTokenManager
{

  public static final String TOKEN_COLLECTION_NAME = "user-tokens";

  private static final String BSON_USER_TOKEN_TOKENS = "tokens";
  private static final String BSON_USER_TOKEN_USER_ID = "userId";
  private static final String BSON_USER_TOKEN_DESCRIPTION = "description";

  private static final String BSON_USER_TOKEN_TOKEN_STRING = "token";
  private static final String BSON_USER_TOKEN_TOKEN_NOT_BEORE = "notBefore";
  private static final String BSON_USER_TOKEN_TOKEN_NOT_AFTER = "notAfter";
  private static final String BSON_USER_TOKEN_TOKEN_REVOCATION = "revocation";

  private static final String BSON_USER_TOKEN_REVOCATION_REVOKED = "revoked";
  private static final String BSON_USER_TOKEN_REVOCATION_USER_ID = "userId";
  private static final String BSON_USER_TOKEN_REVOCATION_TIME = "at";
  private static final String BSON_USER_TOKEN_REVOCATION_REASON = "reason";


  private final CallbackList <IUserTokenModificationCallback> m_aCallbacks = new CallbackList <> ();
  private final IUserManager aUserMgr;

  public MongoUserTokenManager (@NonNull IUserManager aUserMgr)
  {
    super (TOKEN_COLLECTION_NAME);
    ValueEnforcer.notNull (aUserMgr, "UserMgr");
    this.aUserMgr = aUserMgr;
  }

  @Override
  protected @NonNull Document toBson (@NonNull IUserToken aUserToken)
  {
    return getDefaultBusinessDocument (aUserToken)
                               .append (BSON_USER_TOKEN_USER_ID, aUserToken.getUserID ())
                               .append (BSON_USER_TOKEN_TOKENS, userTokenToDocument (aUserToken.getAccessTokenList ()))
                               .append (BSON_USER_TOKEN_DESCRIPTION, aUserToken.getDescription ());

  }

  private List <Document> userTokenToDocument (@NonNull IAccessTokenList accessTokenList)
  {
    return accessTokenList.getAllAccessTokens ().stream ().map (iAccessToken -> new Document ()
                               .append (BSON_USER_TOKEN_TOKEN_STRING, iAccessToken.getTokenString ())
                               .append (BSON_USER_TOKEN_TOKEN_NOT_BEORE, convertLocalDateTimeToDate (iAccessToken.getNotBefore ()))
                               .append (BSON_USER_TOKEN_TOKEN_NOT_AFTER, convertLocalDateTimeToDate (iAccessToken.getNotAfter ()))
                               .append (BSON_USER_TOKEN_TOKEN_REVOCATION, revocationToDocument (iAccessToken.getRevocationStatus ()))).toList ();
  }

  private Document revocationToDocument (@NonNull IRevocationStatus revocationStatus)
  {
    return new Document ().append (BSON_USER_TOKEN_REVOCATION_REVOKED, revocationStatus.isRevoked ())
                               .append (BSON_USER_TOKEN_REVOCATION_USER_ID, revocationStatus.getRevocationUserID ())
                               .append (BSON_USER_TOKEN_REVOCATION_TIME, convertLocalDateTimeToDate (revocationStatus.getRevocationDateTime ()))
                               .append (BSON_USER_TOKEN_REVOCATION_REASON, revocationStatus.getRevocationReason ());


  }

  @Override
  protected @NonNull IUserToken toEntity (@NonNull Document document)
  {
    return new UserToken (populateStubObject (document),
                               readAccessTokenFromDocument (document.getList (BSON_USER_TOKEN_TOKENS, Document.class)),
                               Objects.requireNonNull (aUserMgr.getUserOfID (document.getString (BSON_USER_TOKEN_USER_ID))),
                               document.getString (BSON_USER_TOKEN_DESCRIPTION)
    );
  }

  private static List <AccessToken> readAccessTokenFromDocument (List <Document> sAccessTokens)
  {
    if (sAccessTokens == null)
      return null;

    return sAccessTokens.stream ().map (itemDoc -> new AccessToken (
                               itemDoc.getString (BSON_USER_TOKEN_TOKEN_STRING),
                               convertDatenToLocalDateTime (itemDoc.getDate (BSON_USER_TOKEN_TOKEN_NOT_BEORE)),
                               convertDatenToLocalDateTime (itemDoc.getDate (BSON_USER_TOKEN_TOKEN_NOT_AFTER)),
                               readRevocationFromDocument (itemDoc.get (BSON_USER_TOKEN_TOKEN_REVOCATION, Document.class))
    )).toList ();
  }

  private static RevocationStatus readRevocationFromDocument (Document aDocument)
  {
    if (aDocument == null)
      return null;

    return new RevocationStatus (
                               aDocument.getBoolean (BSON_USER_TOKEN_REVOCATION_REVOKED),
                               aDocument.getString (BSON_USER_TOKEN_REVOCATION_USER_ID),
                               convertDatenToLocalDateTime (aDocument.getDate (BSON_USER_TOKEN_REVOCATION_TIME)),
                               aDocument.getString (BSON_USER_TOKEN_REVOCATION_REASON)
    );
  }


  @Override
  public @NonNull CallbackList <IUserTokenModificationCallback> userTokenModificationCallbacks ()
  {
    return this.m_aCallbacks;
  }

  @Override
  public @Nullable UserToken createUserToken (@Nullable String sTokenString,
                                              @Nullable Map <String, String> aCustomAttrs,
                                              @NonNull IUser aUser,
                                              @Nullable String sDescription)
  {
    final UserToken aUserToken = new UserToken (sTokenString, aCustomAttrs, aUser, sDescription);
    try
    {
      getCollection ().insertOne (toBson (aUserToken));
      m_aCallbacks.forEach (aCB -> aCB.onUserTokenCreated (aUserToken));
      return aUserToken;
    } catch (Exception e)
    {
      AuditHelper.onAuditCreateFailure (UserToken.OT,
                                 aUserToken.getID (),
                                 aUserToken.attrs (),
                                 aUserToken.getUserID (),
                                 aUserToken.getDescription (),
                                 "database-error");
    }
    return null;
  }

  @Override
  public @NonNull EChange updateUserToken (@Nullable String sUserTokenID,
                                           @Nullable Map <String, String> aNewCustomAttrs,
                                           @Nullable String sNewDescription)
  {
    return genericUpdate (sUserTokenID, Updates.combine (
                               Updates.set (BSON_ATTRIBUTES, aNewCustomAttrs),
                               Updates.set (BSON_USER_TOKEN_DESCRIPTION, sNewDescription)
    ), true, () -> m_aCallbacks.forEach (aCB -> aCB.onUserTokenUpdated (sUserTokenID)));
  }

  @Override
  public @NonNull EChange deleteUserToken (@Nullable String sUserTokenID)
  {
    return deleteEntity (sUserTokenID, () -> m_aCallbacks.forEach (aCB -> aCB.onUserTokenDeleted (sUserTokenID)));
  }

  @Override
  public @NonNull EChange createNewAccessToken (@Nullable String sUserTokenID,
                                                @NonNull @Nonempty String sRevocationUserID,
                                                @NonNull LocalDateTime aRevocationDT,
                                                @NonNull @Nonempty String sRevocationReason,
                                                @Nullable String sTokenString)
  {
    UserToken userToken = (UserToken) findById (sUserTokenID);

    if (userToken == null)
      return EChange.UNCHANGED;

    AccessTokenList aAccessTokenList = userToken.getAccessTokenList ();
    aAccessTokenList.revokeActiveAccessToken (sRevocationUserID, aRevocationDT, sRevocationReason);
    AccessToken newAccessToken = aAccessTokenList.createNewAccessToken (sTokenString);

    return genericUpdate (sUserTokenID, Updates.set (BSON_USER_TOKEN_TOKENS, userTokenToDocument (aAccessTokenList)), true,
                               () -> m_aCallbacks.forEach (aCB -> aCB.onUserTokenCreateAccessToken (sUserTokenID, newAccessToken)));

  }

  @Override
  public @NonNull EChange revokeAccessToken (@Nullable String sUserTokenID,
                                             @NonNull @Nonempty String sRevocationUserID,
                                             @NonNull LocalDateTime aRevocationDT,
                                             @NonNull @Nonempty String sRevocationReason)
  {
    UserToken userToken = (UserToken) findById (sUserTokenID);

    if (userToken == null)
      return EChange.UNCHANGED;

    AccessTokenList aAccessTokenList = userToken.getAccessTokenList ();
    aAccessTokenList.revokeActiveAccessToken (sRevocationUserID, aRevocationDT, sRevocationReason);
    return genericUpdate (sUserTokenID, Updates.set (BSON_USER_TOKEN_TOKENS, userTokenToDocument (aAccessTokenList)), true,
                               () -> m_aCallbacks.forEach (aCB -> aCB.onUserTokenRevokeAccessToken (sUserTokenID)));
  }

  @Override
  public @NonNull ICommonsList <IUserToken> getAllActiveUserTokens ()
  {
    return getAllActive ();
  }

  @Override
  public @Nullable IUserToken getUserTokenOfID (@Nullable String sUserTokenID)
  {
    return findById (sUserTokenID);
  }

  @Override
  public @Nullable IUserToken getUserTokenOfTokenString (@Nullable String sTokenString)
  {
    ArrayList <Document> into = getCollection ().find (Filters.eq (BSON_USER_TOKEN_TOKENS + "." +
                               BSON_USER_TOKEN_TOKEN_STRING, sTokenString)).into (new ArrayList <> ());

    if (into.isEmpty ())
      return null;

    return toEntity (into.get (0));
  }

  @Override
  public boolean isAccessTokenUsed (@Nullable String sTokenString)
  {
    return getUserTokenOfTokenString (sTokenString) == null;
  }
}
