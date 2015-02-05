package models.usr

import securesocial.core.{IProfile, PasswordInfo}
import securesocial.core.providers.MailToken
import securesocial.core.services.{SaveMode, UserService}
import util.PlayMacroLogsImpl

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.02.15 14:56
 * Description: Реализация над-модели прослойки между suggest.io и secure-social.
 */
object SsUserService extends UserService[SsUser] with PlayMacroLogsImpl {

  import LOGGER._

  /**
   * Finds a SocialUser that maches the specified id
   * Видимо, это поиск по id юзера в рамках внешнего сервиса.
   * @param providerId the provider id
   * @param userId the user id
   * @return an optional profile
   */
  override def find(providerId: String, userId: String): Future[Option[MExtIdent]] = {
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
  override def deleteToken(uuid: String): Future[Option[MailToken]] = {
    Future successful None
  }

  /**
   * Returns an optional PasswordInfo instance for a given user
   *
   * @param user a user instance
   * @return returns an optional PasswordInfo
   */
  override def passwordInfoFor(user: SsUser): Future[Option[PasswordInfo]] = {
    Future successful None
  }

  /**
   * Saves a profile.  This method gets called when a user logs in, registers or changes his password.
   * This is your chance to save the user information in your backing store.
   *
   * @param profile the user profile
   * @param mode a mode that tells you why the save method was called
   */
  override def save(profile: IProfile, mode: SaveMode): Future[SsUser] = {
    if (mode is SaveMode.SignUp) {
      // Зарегать нового юзера
      MPerson(lang = "ru").save.flatMap { personId =>
        // Сохранить данные идентификации через соц.сеть.
        val mei = MExtIdent(
          personId  = personId,
          provider  = IdProviders.withName(profile.providerId),
          userId    = profile.userId,
          email     = profile.email
        )
        mei.save
          .map { savedId => mei }
      }

    } else if (mode is SaveMode.LoggedIn) {
      // Юзер уже был, как бы.
      // TODO повторно обращаемся к find! Нужно пропатчить secureSocial, чтобы передавала some-результат find() внутри mode.LoggedIn()
      find(profile.providerId, profile.userId)
        .map { _.get }

    } else {
      // Смена пароля или что-то другое, чего не должно происходить через securesocial
      throw new UnsupportedOperationException(s"save(mode = $mode) not implemented")
    }
  }

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
