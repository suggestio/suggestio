package models.usr

import io.suggest.es.model.EsModel
import javax.inject.Inject
import io.suggest.model.n2.node.MNodes
import io.suggest.model.n2.node.meta.MPersonMeta
import io.suggest.util.logs.MacroLogsImpl
import models.mext.ILoginProvider
import models.mproj.ICommonDi
import securesocial.core.{IProfile, PasswordInfo}
import securesocial.core.services.{SaveMode, UserService}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.02.15 14:56
 * Description: Реализация над-модели прослойки между suggest.io и secure-social.
 */
class SsUserService @Inject() (
                                esModel     : EsModel,
                                mNodes      : MNodes,
                                mExtIdents  : MExtIdents,
                                mCommonDi   : ICommonDi
                              )
  extends UserService[SsUser]
  with MacroLogsImpl
{

  import mCommonDi._
  import LOGGER._
  import esModel.api._

  /**
   * Finds a SocialUser that maches the specified id
   * Видимо, это поиск по id юзера в рамках внешнего сервиса.
   * @param providerId the provider id
   * @param userId the user id
   * @return an optional profile
   */
  override def find(providerId: String, userId: String): Future[Option[MExtIdent]] = {
    val prov = ILoginProvider.maybeWithName(providerId).get
    mExtIdents.getByUserIdProv(prov, userId)
  }


  /**
   * Returns an optional PasswordInfo instance for a given user
   *
   * @param user a user instance
   * @return returns an optional PasswordInfo
   */
  override def passwordInfoFor(user: SsUser): Future[Option[PasswordInfo]] = {
    warn(s"passwordInfoFor($user): unsupported, returning None.")
    Future.successful( None )
  }


  /**
   * Saves a profile.  This method gets called when a user logs in, registers or changes his password.
   * This is your chance to save the user information in your backing store.
   *
   * @param profile the user profile
   * @param mode a mode that tells you why the save method was called
   */
  override def save(profile: IProfile, mode: SaveMode): Future[SsUser] = {
    trace(s"save($profile, $mode): Starting...")
    // TODO Скорее всего этот метод никогда не вызвается, т.к. его логика заинлайнилась в ExternalLogin.handleAuth1().
    if (mode is SaveMode.SignUp) {
      // Зарегать нового юзера
      val mnode = mNodes.applyPerson(
        lang    = "ru",   // TODO Определять язык надо бы. Из сессии например, которая здесь пока недоступна.
        nameOpt = profile.fullName,
        mpm     = MPersonMeta(
          nameFirst     = profile.firstName,
          nameLast      = profile.lastName,
          extAvaUrls    = profile.avatarUrl.toList
        )
      )
      for {
        personId <- mNodes.save(mnode)
        mei = MExtIdent(
          personId  = personId,
          provider  = ILoginProvider.maybeWithName(profile.providerId).get,
          userId    = profile.userId,
          email     = profile.email
        )
        _ <- mExtIdents.save(mei)
      } yield {
        mei
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

}
