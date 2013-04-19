package util

import org.joda.time.DateTime
import play.api.i18n.Lang

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
  implicit def getContext : Context = {
    new Context
  }

}


case class Context(
  lang_str            : String = Context.lang_default,
  implicit val lang   : Lang = Lang(Context.lang_default),
  implicit val p_opt  : Acl.MPOptT = None    // Если юзер залогинен, то тут будет Some().
) {

  implicit lazy val now : DateTime = DateTime.now
}