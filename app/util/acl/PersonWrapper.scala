package util.acl

import models.usr.{MPersonLinks, MPerson}
import models.{MNodeTypes, MNode}
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.secure.SessionUtil
import scala.concurrent.Future
import play.api.Play.current

object PersonWrapper {

  private val sessionUtil = current.injector.instanceOf[SessionUtil]

  type PwOpt_t = Option[PersonWrapper]

  /**
   * Извечь из реквеста данные о залогиненности юзера. Функция враппер над getPersonWrapperFromSession().
   * @param request Реквест.
   * @return Option[PersonWrapper].
   */
  def getFromRequest(implicit request: RequestHeader) = getFromSession(request.session)

  /**
   * Извечь данные о залогиненности юзера из сессии.
   * @param session Сессия.
   * @return Option[PersonWrapper].
   */
  def getFromSession(implicit session: Session): PwOpt_t = {
    sessionUtil.getPersonId(session)
      .map { PersonWrapper.apply }
  }


  /** Статическая функция проверки на принадлежность к админам вынесена сюда.
    * Используется обычно напрямую, т.к. у нас нет возможности добавить её напрямую в PwOpt_t. */
  def isSuperuser(pwOpt: PwOpt_t): Boolean = {
    pwOpt.exists(_.isSuperuser)
  }


  /** Асинхронно найти имя для пользователя в кеше или в хранилище модели.
    * @param pwOpt Экземпляр PwOpt_t
    * @return Фьючерс с юзернеймом.
    */
  def findUserName(pwOpt: PwOpt_t): Future[Option[String]] = {
    if (pwOpt.isDefined) {
      MPerson.findUsernameCached(pwOpt.get.personId)
    } else {
      Future successful None
    }
  }
}

/**
 * PersonWrapper нужен для ленивого доступа к данным. Часто содержимое MPerson не нужно, поэтому зачем его читать сразу?
 * @param personId id юзера
 */
final case class PersonWrapper(personId: String) extends MPersonLinks {
  lazy val personOptFut = MNode.getByIdType(personId, MNodeTypes.Person)
}
