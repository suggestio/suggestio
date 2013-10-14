package util

import org.joda.time.DateTime
import play.api.i18n.Lang
import play.api.mvc.Request
import play.api.Play.current
import util.acl._, PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 12:10
 * Description: Объект контекста запроса для прозрачной передачи состояния запроса из контроллеров в шаблоны
 * и между шаблонами. Контроллеры просто расширяют этот трайт, а шаблоны просто объявляют тип возвращаемого значения
 * метода в implicit-списке параметров.
 */

object Context {
  val LANG_DFLT = Lang("ru")
}


trait ContextT {

  /**
   * Выдать контекст. Неявно вызывается при вызове шаблона из контроллера.
   * @return
   */
  implicit final def getContext2(implicit req:AbstractRequestWithPwOpt[_], lang:Lang = Context.LANG_DFLT): Context = {
    Context2()
  }

}


/** Базовый трейт контекста. Используется всеми шаблонами и везде. Переименовывать и менять нельзя.
  * Интерфейс можно только расширять и аккуратно рефакторить, иначе хана.
  */
trait Context {

  implicit def pw_opt: PwOpt_t
  implicit def request: Request[_]
  implicit def lang: Lang

  implicit lazy val now : DateTime = DateTime.now

  def isAuth:  Boolean = pw_opt.isDefined
  def isAdmin: Boolean = pw_opt.exists(_.isAdmin)

  def lang_str = lang.language

  lazy val canAddSites: Boolean = current.configuration.getBoolean("can_add_sites") getOrElse true
  lazy val isDebug: Boolean     = request.getQueryString("debug").isDefined

  lazy val timestamp: Long = now.toInstant.getMillis
}


// Непосредственные реализации контекстов. Расширять их API в обход trait Context не имеет смысла.

/** Контекст времён комбинируемых ActionBuilder'ов. */
case class Context2(
  implicit val request: AbstractRequestWithPwOpt[_],
  implicit val lang: Lang
) extends Context {

  implicit def pw_opt = request.pwOpt
}

