package models.adv.js

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 12:34
 * Description: Язык общения акторов, занимается описанием взаимодействия между акторами разных уровней:
 * На этом языке разговаривают ws-актор-супервизор с подчинёнными target-level-акторами.
 */

object JsCommand {

  val TYPE_FN = "type"
  val DATA_FN = "data"

  /** unmapper реализаций [[JsCommand]] в json. */
  implicit def writes: Writes[JsCommand] = (
    (__ \ DATA_FN).write[JsBuilder] and
    (__ \ TYPE_FN).write[String]
  ) { jsc => (jsc.jsBuilder, jsc.evalType) }

}


/** Контейнер js-кода, отправляемого через ws. */
case class JsCommand(
  jsBuilder : JsBuilder,
  sendMode  : CmdSendMode
) {

  /** Строка "js" тут означает, что мы отправляем голый js, который нужно вызвать через eval().
    * Другие варианты пока не поддерживаются. */
  def evalType: String = "js"

}



/** Допустимые режимы отправки js-кода в ws. */
object CmdSendModes extends Enumeration {
  type CmdSendMode = Value
  /** Асинхронный режим отправки. */
  val Async   = Value

  /** Отправка через очередь команд. */
  val Queued  = Value
}

