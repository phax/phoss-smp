package com.helger.phoss.smp.backend.mongodb.security;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.misc.DevelopersNote;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
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

public class UserTokenManagerMongoDB extends AbstractBusinessObjectManagerMongoDB <IUserToken, UserToken> implements
                                     IUserTokenManager
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
  private final IUserManager m_aUserMgr;

  public UserTokenManagerMongoDB (@NonNull final IUserManager aUserMgr)
  {
    super (TOKEN_COLLECTION_NAME);
    ValueEnforcer.notNull (aUserMgr, "UserMgr");
    m_aUserMgr = aUserMgr;
  }

  @Override
  protected @NonNull Document toBson (@NonNull final IUserToken aUserToken)
  {
    return getDefaultBusinessDocument (aUserToken).append (BSON_USER_TOKEN_USER_ID, aUserToken.getUserID ())
                                                  .append (BSON_USER_TOKEN_TOKENS,
                                                           _userTokenToDocument (aUserToken.getAccessTokenList ()))
                                                  .append (BSON_USER_TOKEN_DESCRIPTION, aUserToken.getDescription ());
  }

  @NonNull
  private List <Document> _userTokenToDocument (@NonNull final IAccessTokenList aAccessTokenList)
  {
    return new CommonsArrayList <> (aAccessTokenList.getAllAccessTokens (),
                                    aItem -> new Document ().append (BSON_USER_TOKEN_TOKEN_STRING,
                                                                     aItem.getTokenString ())
                                                            .append (BSON_USER_TOKEN_TOKEN_NOT_BEORE,
                                                                     TypeConverter.convert (aItem.getNotBefore (),
                                                                                            Date.class))
                                                            .append (BSON_USER_TOKEN_TOKEN_NOT_AFTER,
                                                                     TypeConverter.convert (aItem.getNotAfter (),
                                                                                            Date.class))
                                                            .append (BSON_USER_TOKEN_TOKEN_REVOCATION,
                                                                     _revocationToDocument (aItem.getRevocationStatus ())));
  }

  @NonNull
  private Document _revocationToDocument (@NonNull final IRevocationStatus revocationStatus)
  {
    return new Document ().append (BSON_USER_TOKEN_REVOCATION_REVOKED, Boolean.valueOf (revocationStatus.isRevoked ()))
                          .append (BSON_USER_TOKEN_REVOCATION_USER_ID, revocationStatus.getRevocationUserID ())
                          .append (BSON_USER_TOKEN_REVOCATION_TIME,
                                   TypeConverter.convert (revocationStatus.getRevocationDateTime (), Date.class))
                          .append (BSON_USER_TOKEN_REVOCATION_REASON, revocationStatus.getRevocationReason ());
  }

  @Nullable
  private static RevocationStatus _readRevocationFromDocument (@Nullable final Document aDocument)
  {
    if (aDocument == null)
      return null;

    return new RevocationStatus (aDocument.getBoolean (BSON_USER_TOKEN_REVOCATION_REVOKED).booleanValue (),
                                 aDocument.getString (BSON_USER_TOKEN_REVOCATION_USER_ID),
                                 TypeConverter.convert (aDocument.getDate (BSON_USER_TOKEN_REVOCATION_TIME),
                                                        LocalDateTime.class),
                                 aDocument.getString (BSON_USER_TOKEN_REVOCATION_REASON));
  }

  @Nullable
  private static ICommonsList <AccessToken> _readAccessTokenFromDocument (@Nullable final List <Document> aAccessTokens)
  {
    if (aAccessTokens == null)
      return null;

    return new CommonsArrayList <> (aAccessTokens,
                                    aItem -> new AccessToken (aItem.getString (BSON_USER_TOKEN_TOKEN_STRING),
                                                              TypeConverter.convert (aItem.getDate (BSON_USER_TOKEN_TOKEN_NOT_BEORE),
                                                                                     LocalDateTime.class),
                                                              TypeConverter.convert (aItem.getDate (BSON_USER_TOKEN_TOKEN_NOT_AFTER),
                                                                                     LocalDateTime.class),
                                                              _readRevocationFromDocument (aItem.get (BSON_USER_TOKEN_TOKEN_REVOCATION,
                                                                                                      Document.class))));
  }

  @Override
  protected @NonNull UserToken toEntity (@NonNull final Document aDocument)
  {
    return new UserToken (populateStubObject (aDocument),
                          _readAccessTokenFromDocument (aDocument.getList (BSON_USER_TOKEN_TOKENS, Document.class)),
                          Objects.requireNonNull (m_aUserMgr.getUserOfID (aDocument.getString (BSON_USER_TOKEN_USER_ID))),
                          aDocument.getString (BSON_USER_TOKEN_DESCRIPTION));
  }

  @Override
  @ReturnsMutableObject
  public @NonNull CallbackList <IUserTokenModificationCallback> userTokenModificationCallbacks ()
  {
    return m_aCallbacks;
  }

  private @Nullable UserToken _internalCreateUserToken (@NonNull final UserToken aUserToken)
  {
    if (!getCollection ().insertOne (toBson (aUserToken)).wasAcknowledged ())
    {
      AuditHelper.onAuditCreateFailure (UserToken.OT,
                                        aUserToken.getID (),
                                        aUserToken.attrs (),
                                        aUserToken.getUserID (),
                                        aUserToken.getDescription (),
                                        "database-error");
      return null;
    }

    AuditHelper.onAuditCreateSuccess (UserToken.OT,
                                      aUserToken.getID (),
                                      aUserToken.attrs (),
                                      aUserToken.getUserID (),
                                      aUserToken.getDescription ());

    m_aCallbacks.forEach (aCB -> aCB.onUserTokenCreated (aUserToken));

    return aUserToken;
  }

  @Override
  public @Nullable UserToken createUserToken (@Nullable final String sTokenString,
                                              @Nullable final Map <String, String> aCustomAttrs,
                                              @NonNull final IUser aUser,
                                              @Nullable final String sDescription)
  {
    final UserToken aUserToken = new UserToken (sTokenString, aCustomAttrs, aUser, sDescription);
    return _internalCreateUserToken (aUserToken);
  }

  @DevelopersNote ("For internal use only")
  public @Nullable UserToken internalCreateMigrationUserToken (@NonNull final UserToken aSrcUserToken)
  {
    return _internalCreateUserToken (aSrcUserToken);
  }

  @Override
  public @NonNull EChange updateUserToken (@Nullable final String sUserTokenID,
                                           @Nullable final Map <String, String> aNewCustomAttrs,
                                           @Nullable final String sNewDescription)
  {
    if (StringHelper.isEmpty (sUserTokenID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sUserTokenID,
                                              addLastModToUpdate (Updates.combine (Updates.set (BSON_ATTRIBUTES,
                                                                                                aNewCustomAttrs),
                                                                                   Updates.set (BSON_USER_TOKEN_DESCRIPTION,
                                                                                                sNewDescription))));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (UserToken.OT, "set-all", sUserTokenID, aNewCustomAttrs, sNewDescription);

      m_aCallbacks.forEach (aCB -> aCB.onUserTokenUpdated (sUserTokenID));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (UserToken.OT, "set-all", sUserTokenID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange deleteUserToken (@Nullable final String sUserTokenID)
  {
    if (StringHelper.isEmpty (sUserTokenID))
      return EChange.UNCHANGED;

    final EChange eChange = deleteEntity (sUserTokenID);
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditDeleteSuccess (UserToken.OT, sUserTokenID);

      m_aCallbacks.forEach (aCB -> aCB.onUserTokenDeleted (sUserTokenID));
    }
    else
    {
      AuditHelper.onAuditDeleteFailure (UserToken.OT, sUserTokenID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange createNewAccessToken (@Nullable final String sUserTokenID,
                                                @NonNull @Nonempty final String sRevocationUserID,
                                                @NonNull final LocalDateTime aRevocationDT,
                                                @NonNull @Nonempty final String sRevocationReason,
                                                @Nullable final String sTokenString)
  {
    final UserToken aUserToken = findByID (sUserTokenID);
    if (aUserToken == null)
      return EChange.UNCHANGED;

    final AccessTokenList aAccessTokenList = aUserToken.getAccessTokenList ();
    aAccessTokenList.revokeActiveAccessToken (sRevocationUserID, aRevocationDT, sRevocationReason);
    final AccessToken aNewAccessToken = aAccessTokenList.createNewAccessToken (sTokenString);

    final EChange eChange = genericUpdateOne (sUserTokenID,
                                              addLastModToUpdate (Updates.set (BSON_USER_TOKEN_TOKENS,
                                                                               _userTokenToDocument (aAccessTokenList))));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (UserToken.OT,
                                        "create-new-access-token",
                                        sUserTokenID,
                                        sRevocationUserID,
                                        aRevocationDT,
                                        sRevocationReason,
                                        sTokenString);

      m_aCallbacks.forEach (aCB -> aCB.onUserTokenCreateAccessToken (sUserTokenID, aNewAccessToken));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (UserToken.OT, "create-new-access-token", sUserTokenID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange revokeAccessToken (@Nullable final String sUserTokenID,
                                             @NonNull @Nonempty final String sRevocationUserID,
                                             @NonNull final LocalDateTime aRevocationDT,
                                             @NonNull @Nonempty final String sRevocationReason)
  {
    final UserToken aUserToken = findByID (sUserTokenID);
    if (aUserToken == null)
      return EChange.UNCHANGED;

    final AccessTokenList aAccessTokenList = aUserToken.getAccessTokenList ();
    if (aAccessTokenList.revokeActiveAccessToken (sRevocationUserID, aRevocationDT, sRevocationReason).isUnchanged ())
    {
      AuditHelper.onAuditModifyFailure (UserToken.OT, "revoke-access-token", sUserTokenID, "already-revoked");
      return EChange.UNCHANGED;
    }

    final EChange eChange = genericUpdateOne (sUserTokenID,
                                              addLastModToUpdate (Updates.set (BSON_USER_TOKEN_TOKENS,
                                                                               _userTokenToDocument (aAccessTokenList))));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (UserToken.OT,
                                        "revoke-access-token",
                                        sUserTokenID,
                                        sRevocationUserID,
                                        aRevocationDT,
                                        sRevocationReason);

      m_aCallbacks.forEach (aCB -> aCB.onUserTokenRevokeAccessToken (sUserTokenID));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (UserToken.OT, "revoke-access-token", sUserTokenID, "no-such-id");
    }
    return eChange;
  }

  @Override
  @ReturnsMutableCopy
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
    final Document aDoc = getCollection ().find (Filters.eq (BSON_USER_TOKEN_TOKENS +
                                                             "." +
                                                             BSON_USER_TOKEN_TOKEN_STRING,
                                                             sTokenString)).first ();
    if (aDoc == null)
      return null;

    return toEntity (aDoc);
  }

  @Override
  public boolean isAccessTokenUsed (@Nullable final String sTokenString)
  {
    return getUserTokenOfTokenString (sTokenString) != null;
  }
}
