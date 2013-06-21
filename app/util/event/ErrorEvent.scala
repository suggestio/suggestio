package util.event

import io.suggest.util.event.subscriber.SioEventTJSable
import play.api.libs.json._
import io.suggest.event.SioNotifier

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.06.13 15:15
 * Description: События различных ошибок, возвращаемые в виде JSON юзерам.
 */

class ErrorEvent(msg:String, severity:String) extends SioEventTJSable {
  def getClassifier: SioNotifier.Classifier = ???

  def toJson: JsValue = {
    JsObject(Seq(
      "type"       -> JsString("problem"),
      "severity"   -> JsString(severity),
      "error_msg"  -> JsString(msg)
    ))
  }
}

// Ошибка доступа.
case class AccessErrorEvent(msg:String) extends ErrorEvent(msg, "CRIT")

// Ошибка suggest.io.
case class InternalServerErrorEvent(msg:String) extends ErrorEvent(msg, "CRIT")

// Возможная внутренняя проблема. Нет уверенности, что ошибка нанесет вред пользователю.
case class MaybeErrorEvent(msg:String) extends ErrorEvent(msg, "WARN")