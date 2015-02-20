package models.adv.js

import io.suggest.adv.ext.model.MAnswerStatusesT
import play.api.libs.json._
import play.api.Play.{configuration, current}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 19:11
 * Description: Заготовки js-моделей протокола общения с adv-фронтендами.
 */

object JsProtoUtil {
  /** Название js-модуля для обращения к нему. */
  val EXT_ADV_MODULE = configuration.getString("adv.ext.js.module.name") getOrElse "SioPR"
}

/** Компаньон абстрактных ask-builder'ов. */
object JsBuilder {

  /** Рендер AskBuilder'а в JSON. */
  implicit def writes = new Writes[JsBuilder] {
    override def writes(o: JsBuilder): JsValue = {
      JsString(o.js)
    }
  }

}

/** Интерфейс билдеров js-кода. */
trait JsBuilder {
  def js: String
}


/** Статусы ответов js серверу. */
object AnswerStatuses extends MAnswerStatusesT {

  /** mapper в JSON. */
  implicit def reads: Reads[T] = {
    __.read[String]
      .map(withName)
  }

}

