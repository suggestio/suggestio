package models.adv.js

import io.suggest.model.EnumMaybeWithName
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
object AnswerStatuses extends Enumeration with EnumMaybeWithName {

  /** Класс-заготовка одного инстанса модели. */
  protected abstract sealed class Val(val jsStr: String) extends super.Val(jsStr) {
    def isSuccess: Boolean
    def isError: Boolean
  }

  type AnswerStatus = Val
  override type T = AnswerStatus

  val Success: AnswerStatus = new Val("success") {
    override def isSuccess  = true
    override def isError    = false
  }

  val Error: AnswerStatus   = new Val("error") {
    override def isSuccess  = false
    override def isError    = true
  }

  /** Статус означает, что в контексте недостаточно данных для выполнения публикации.
    * Возможно не хватает картинки, нужны доп.карточки или ещё чего-то. */
  val FillContext: AnswerStatus = new Val("fillCtx") {
    override def isSuccess  = false
    override def isError    = false
  }


  /** mapper в JSON. */
  implicit def reads: Reads[AnswerStatus] = {
    __.read[String]
      .map(withName)
  }

}

