package util.acl

import models.{MPerson, MPersonLinks}
import play.api.mvc._, Security.username
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

object PersonWrapper {

  type PwOpt_t = Option[PersonWrapper]

  /**
   * Извечь из реквеста данные о залогиненности юзера. Функция враппер над getPersonWrapperFromSession().
   * @param request Реквест.
   * @tparam A Подтип реквеста (не важен).
   * @return Option[PersonWrapper].
   */
  def getFromRequest(implicit request: RequestHeader) = getFromSession(request.session)

  /**
   * Извечь данные о залогиненности юзера из сессии.
   * @param session Сессия.
   * @return Option[PersonWrapper].
   */
  def getFromSession(implicit session: Session): PwOpt_t = session.get(username).map { new PersonWrapper(_) }


  /** Статическая функция проверки на принадлежность к админам вынесена сюда.
    * Используется обычно напрямую, т.к. у нас нет возможности добавить её напрямую в PwOpt_t. */
  def isSuperuser(pwOpt: PwOpt_t): Boolean = pwOpt.exists(_.isSuperuser)
}

/**
 * PersonWrapper нужен для ленивого доступа к данным. Часто содержимое MPerson не нужно, поэтому зачем его читать сразу?
 * @param personId id юзера
 */
case class PersonWrapper(personId: String) extends MPersonLinks {

  // TODO Надо будет это оптимизировать. Если .person будет нужен почти везде, то надо запрос фьючерса отделить от Await
  //      в отдельный val класса. На текущий момент этот вызов нигде не используется, поэтому целиком lazy.
  lazy val personOptFut = MPerson getById personId
}
