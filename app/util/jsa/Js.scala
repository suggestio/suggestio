package util.jsa

import play.api.http.{Writeable, ContentTypes, ContentTypeOf}
import play.api.mvc.Codec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.14 12:40
 * Description: Некое подобие js-action'ов зотоника, т.е. простого интерфейса для сборки js-действий на клиенте
 * на стороне сервера.
 */

/**
 * Контейнер для результата экшена в контроллере. Использовать так:
 * {{{
 *   def myService = Action {
 *     val action1 = ... : JsAction
 *     val action2 = ...
 *     Ok(Js(action1, action2))
 *   }
 * }}}
 * @param actions Js-экшены для рендера в прямом порядке.
 * @param sbInitLen Начальный разме аккамулятора.
 */
case class Js(sbInitLen: Int, actions: JsAction*)

object Js {

  def apply(actions: JsAction *): Js = Js(64, actions : _*)

  implicit def contentTypeOf_Js(implicit codec: Codec): ContentTypeOf[Js] = {
    ContentTypeOf[Js](Some(ContentTypes.JAVASCRIPT))
  }

  implicit def writeableOf_Js(implicit codec: Codec): Writeable[Js] = {
    Writeable { jsActions =>
      val sb = new StringBuilder(jsActions.sbInitLen)
      jsActions.actions
        .foreach { action =>
        action.renderJsAction(sb)
        if (sb.last != ';')
          sb.append(';')
      }
      codec.encode(sb.toString())
    }
  }

}


/** Интерфейс для js-экшенов. Экшен должен поддерживать рендер себя в голый js. */
trait JsAction {
  def sbInitSz: Int = 64
  def renderJsAction(sb: StringBuilder): StringBuilder = sb
  def renderToString(sb: StringBuilder = new StringBuilder(sbInitSz)): String = {
    renderJsAction(sb)
      .toString()
  }
}
