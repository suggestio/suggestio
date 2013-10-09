package util.acl

import models.{MPerson, MPersonLinks}
import play.api.mvc._, Security.username

object PersonWrapper {

  type PwOpt_t = Option[PersonWrapper]

  /**
   * Извечь из реквеста данные о залогиненности юзера. Функция враппер над getPersonWrapperFromSession().
   * @param request Реквест.
   * @tparam A Подтип реквеста (не важен).
   * @return Option[PersonWrapper].
   */
  def getFromRequest[A](implicit request: RequestHeader) = getFromSession(request.session)

  /**
   * Извечь данные о залогиненности юзера из сессии.
   * @param session Сессия.
   * @return Option[PersonWrapper].
   */
  def getFromSession(implicit session: Session): PwOpt_t = session.get(username).map { new PersonWrapper(_) }

}

/**
 * PersonWrapper нужен для ленивого доступа к данным. Часто содержимое MPerson не нужно, поэтому зачем его читать сразу?
 * @param id id юзера
 */
case class PersonWrapper(id: String) extends MPersonLinks {
  lazy val person = MPerson.getById(id).get
}
