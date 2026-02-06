package com.helger.phoss.smp.backend.mongodb.security;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
import com.helger.typeconvert.impl.TypeConverter;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

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

  public MongoUserTokenManager (@NonNull final IUserManager aUserMgr)
  {
    super (TOKEN_COLLECTION_NAME);
    ValueEnforcer.notNull (aUserMgr, "UserMgr");
    this.aUserMgr = aUserMgr;
  }

  @Override
  protected @NonNull Document toBson (@NonNull final IUserToken aUserToken)
  {
    return getDefaultBusinessDocument (aUserToken).append (BSON_USER_TOKEN_USER_ID, aUserToken.getUserID ())
                                                  .append (BSON_USER_TOKEN_TOKENS,
                                                           userTokenToDocument (aUserToken.getAccessTokenList ()))
                                                  .append (BSON_USER_TOKEN_DESCRIPTION, aUserToken.getDescription ());
  }

  private List <Document> userTokenToDocument (@NonNull final IAccessTokenList accessTokenList)
  {
    return accessTokenList.getAllAccessTokens ()
                          .stream ()
                          .map (iAccessToken -> new Document ().append (BSON_USER_TOKEN_TOKEN_STRING,
                                                                        iAccessToken.getTokenString ())
                                                               .append (BSON_USER_TOKEN_TOKEN_NOT_BEORE,
                                                                        TypeConverter.convert (iAccessToken.getNotBefore (),
                                                                                               Date.class))
                                                               .append (BSON_USER_TOKEN_TOKEN_NOT_AFTER,
                                                                        TypeConverter.convert (iAccessToken.getNotAfter (),
                                                                                               Date.class))
                                                               .append (BSON_USER_TOKEN_TOKEN_REVOCATION,
                                                                        _revocationToDocument (iAccessToken.getRevocationStatus ())))
                          .toList ();
  }

  private Document _revocationToDocument (@NonNull final IRevocationStatus revocationStatus)
  {
    return new Document ().append (BSON_USER_TOKEN_REVOCATION_REVOKED, Boolean.valueOf (revocationStatus.isRevoked ()))
                          .append (BSON_USER_TOKEN_REVOCATION_USER_ID, revocationStatus.getRevocationUserID ())
                          .append (BSON_USER_TOKEN_REVOCATION_TIME,
                                   TypeConverter.convert (revocationStatus.getRevocationDateTime (), Date.class))
                          .append (BSON_USER_TOKEN_REVOCATION_REASON, revocationStatus.getRevocationReason ());
  }

  @Override
  protected @NonNull IUserToken toEntity (@NonNull final Document document)
  {
    return new UserToken (populateStubObject (document),
                          readAccessTokenFromDocument (document.getList (BSON_USER_TOKEN_TOKENS, Document.class)),
                          Objects.requireNonNull (aUserMgr.getUserOfID (document.getString (BSON_USER_TOKEN_USER_ID))),
                          document.getString (BSON_USER_TOKEN_DESCRIPTION));
  }

  private static List <AccessToken> readAccessTokenFromDocument (final List <Document> sAccessTokens)
  {
    if (sAccessTokens == null)
      return null;

    return sAccessTokens.stream ()
                        .map (itemDoc -> new AccessToken (itemDoc.getString (BSON_USER_TOKEN_TOKEN_STRING),
                                                          TypeConverter.convert (itemDoc.getDate (BSON_USER_TOKEN_TOKEN_NOT_BEORE),
                                                                                 LocalDateTime.class),
                                                          TypeConverter.convert (itemDoc.getDate (BSON_USER_TOKEN_TOKEN_NOT_AFTER),
                                                                                 LocalDateTime.class),
                                                          readRevocationFromDocument (itemDoc.get (BSON_USER_TOKEN_TOKEN_REVOCATION,
                                                                                                   Document.class))))
                        .toList ();
  }

  private static RevocationStatus readRevocationFromDocument (final Document aDocument)
  {
    if (aDocument == null)
      return null;

    return new RevocationStatus (aDocument.getBoolean (BSON_USER_TOKEN_REVOCATION_REVOKED).booleanValue (),
                                 aDocument.getString (BSON_USER_TOKEN_REVOCATION_USER_ID),
                                 TypeConverter.convert (aDocument.getDate (BSON_USER_TOKEN_REVOCATION_TIME),
                                                        LocalDateTime.class),
                                 aDocument.getString (BSON_USER_TOKEN_REVOCATION_REASON));
  }

  @Override
  public @NonNull CallbackList <IUserTokenModificationCallback> userTokenModificationCallbacks ()
  {
    return this.m_aCallbacks;
  }

  @Override
  public @Nullable UserToken createUserToken (@Nullable final String sTokenString,
                                              @Nullable final Map <String, String> aCustomAttrs,
                                              @NonNull final IUser aUser,
                                              @Nullable final String sDescription)
  {
    final UserToken aUserToken = new UserToken (sTokenString, aCustomAttrs, aUser, sDescription);
    try
    {
      getCollection ().insertOne (toBson (aUserToken));
      m_aCallbacks.forEach (aCB -> aCB.onUserTokenCreated (aUserToken));
      return aUserToken;
    }
    catch (final Exception e)
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
  public @NonNull EChange updateUserToken (@Nullable final String sUserTokenID,
                                           @Nullable final Map <String, String> aNewCustomAttrs,
                                           @Nullable final String sNewDescription)
  {
    return genericUpdate (sUserTokenID,
                          Updates.combine (Updates.set (BSON_ATTRIBUTES, aNewCustomAttrs),
                                           Updates.set (BSON_USER_TOKEN_DESCRIPTION, sNewDescription)),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserTokenUpdated (sUserTokenID)));
  }

  @Override
  public @NonNull EChange deleteUserToken (@Nullable final String sUserTokenID)
  {
    return deleteEntity (sUserTokenID, () -> m_aCallbacks.forEach (aCB -> aCB.onUserTokenDeleted (sUserTokenID)));
  }

  @Override
  public @NonNull EChange createNewAccessToken (@Nullable final String sUserTokenID,
                                                @NonNull @Nonempty final String sRevocationUserID,
                                                @NonNull final LocalDateTime aRevocationDT,
                                                @NonNull @Nonempty final String sRevocationReason,
                                                @Nullable final String sTokenString)
  {
    final UserToken userToken = (UserToken) findByID (sUserTokenID);

    if (userToken == null)
      return EChange.UNCHANGED;

    final AccessTokenList aAccessTokenList = userToken.getAccessTokenList ();
    aAccessTokenList.revokeActiveAccessToken (sRevocationUserID, aRevocationDT, sRevocationReason);
    final AccessToken newAccessToken = aAccessTokenList.createNewAccessToken (sTokenString);

    return genericUpdate (sUserTokenID,
                          Updates.set (BSON_USER_TOKEN_TOKENS, userTokenToDocument (aAccessTokenList)),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserTokenCreateAccessToken (sUserTokenID,
                                                                                               newAccessToken)));
  }

  @Override
  public @NonNull EChange revokeAccessToken (@Nullable final String sUserTokenID,
                                             @NonNull @Nonempty final String sRevocationUserID,
                                             @NonNull final LocalDateTime aRevocationDT,
                                             @NonNull @Nonempty final String sRevocationReason)
  {
    final UserToken userToken = (UserToken) findByID (sUserTokenID);

    if (userToken == null)
      return EChange.UNCHANGED;

    final AccessTokenList aAccessTokenList = userToken.getAccessTokenList ();
    aAccessTokenList.revokeActiveAccessToken (sRevocationUserID, aRevocationDT, sRevocationReason);
    return genericUpdate (sUserTokenID,
                          Updates.set (BSON_USER_TOKEN_TOKENS, userTokenToDocument (aAccessTokenList)),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserTokenRevokeAccessToken (sUserTokenID)));
  }

  @Override
  public @NonNull ICommonsList <IUserToken> getAllActiveUserTokens ()
  {
    return getAllActive ();
  }

  @Override
  public @Nullable IUserToken getUserTokenOfID (@Nullable final String sUserTokenID)
  {
    return findByID (sUserTokenID);
  }

  @Override
  public @Nullable IUserToken getUserTokenOfTokenString (@Nullable final String sTokenString)
  {
    final ArrayList <Document> into = getCollection ().find (Filters.eq (BSON_USER_TOKEN_TOKENS +
                                                                         "." +
                                                                         BSON_USER_TOKEN_TOKEN_STRING,
                                                                         sTokenString)).into (new ArrayList <> ());

    if (into.isEmpty ())
      return null;

    return toEntity (into.get (0));
  }

  @Override
  public boolean isAccessTokenUsed (@Nullable final String sTokenString)
  {
    return getUserTokenOfTokenString (sTokenString) != null;
  }
}
