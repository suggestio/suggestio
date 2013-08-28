package util

import org.joda.time.DateTime
import play.api.i18n.Lang
import play.api.mvc.{Request, AnyContent}
import play.api.Play.current

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 12:10
 * Description: Объект контекста запроса для прозрачной передачи состояния запроса из контроллеров в шаблоны
 * и между шаблонами. Контроллеры просто расширяют этот трайт, а шаблоны просто объявляют тип возвращаемого значения
 * метода в implicit-списке параметров.
 */

object Context {
  val lang_default = "ru"
}


trait ContextT {

  /**
   * Выдать контекст. Неявно вызывается при вызове шаблона из контроллера.
   * @return
   */
  implicit def getContext(implicit pw_opt: Acl.PwOptT,
                          request: Request[AnyContent],
                          lang: Lang = Lang("ru")) : Context = {
    new Context
  }

}


case class Context(
  implicit val pw_opt  : Acl.PwOptT,    // Если юзер залогинен, то тут будет Some().
  implicit val request : Request[AnyContent],
  implicit val lang    : Lang
) {

  implicit lazy val now : DateTime = DateTime.now

  def isAuth:  Boolean = pw_opt.isDefined
  def isAdmin: Boolean = pw_opt.exists(_.isAdmin)

  def lang_str = lang.language

  lazy val canAddSites: Boolean = current.configuration.getBoolean("can_add_sites") getOrElse true
  lazy val isDebug: Boolean     = request.getQueryString("debug").isDefined

  lazy val timestamp: Long = now.toInstant.getMillis
}