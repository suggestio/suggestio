package models.usr

import securesocial.core.{IProfile, PasswordInfo}
import securesocial.core.providers.MailToken
import securesocial.core.services.{SaveMode, UserService}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.02.15 14:56
 * Description: Реализация над-модели прослойки между suggest.io и secure-social.
 */
object SsUserService extends UserService[SsUser] {

  /**
   * Finds a SocialUser that maches the specified id
   * Видимо, это поиск по id юзера в рамках внешнего сервиса.
   * @param providerId the provider id
   * @param userId the user id
   * @return an optional profile
   */
  override def find(providerId: String, userId: String): Future[Option[IProfile]] = {
    val prov = IdProviders.withName(providerId)
    MExtIdent.getByUserIdProv(prov, userId)
  }

  /**
   * Finds a profile by email and provider
   *
   * @param email - the user email
   * @param providerId - the provider id
   * @return an optional profile
   */
  override def findByEmailAndProvider(email: String, providerId: String): Future[Option[IProfile]] = ???

  /**
   * Deletes a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param uuid the token id
   */
  override def deleteToken(uuid: String): Future[Option[MailToken]] = ???

  /**
   * Returns an optional PasswordInfo instance for a given user
   *
   * @param user a user instance
   * @return returns an optional PasswordInfo
   */
  override def passwordInfoFor(user: SsUser): Future[Option[PasswordInfo]] = ???

  /**
   * Saves a profile.  This method gets called when a user logs in, registers or changes his password.
   * This is your chance to save the user information in your backing store.
   *
   * @param profile the user profile
   * @param mode a mode that tells you why the save method was called
   */
  override def save(profile: IProfile, mode: SaveMode): Future[SsUser] = ???

  /**
   * Links the current user to another profile
   *
   * @param current The current user instance
   * @param to the profile that needs to be linked to
   */
  override def link(current: SsUser, to: IProfile): Future[SsUser] = ???

  /**
   * Finds a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token the token id
   * @return
   */
  override def findToken(token: String): Future[Option[MailToken]] = ???

  /**
   * Deletes all expired tokens
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   */
  override def deleteExpiredTokens(): Unit = ???

  /**
   * Updates the PasswordInfo for a given user
   *
   * @param user a user instance
   * @param info the password info
   * @return
   */
  override def updatePasswordInfo(user: SsUser, info: PasswordInfo): Future[Option[IProfile]] = ???

  /**
   * Saves a mail token.  This is needed for users that
   * are creating an account in the system or trying to reset a password
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token The token to save
   */
  override def saveToken(token: MailToken): Future[MailToken] = ???
}
