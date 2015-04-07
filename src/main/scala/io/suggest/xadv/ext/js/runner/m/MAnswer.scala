package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.JsCommand

import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.15 18:56
 * Description: Обертка ответов на запросы. Этот ответ отправляется на сервер.
 */

trait IAnswer extends IToJsonDict {

  def replyTo : Option[String]

  override def toJson: Dictionary[Any] = {
    val d = Dictionary[Any]()
    val _replyTo = replyTo
    if (_replyTo.isDefined)
      d.update(JsCommand.REPLY_TO_FN, replyTo.get)
    d
  }
}


/** ws-ответы обычно содержат полезную нагрузку. Поэтому тут название поля и абстрактное значение. */
trait IValuedAnswer extends IAnswer {
  
  def value: Any
  def valueName: String

  override def toJson: Dictionary[Any] = {
    val d = super.toJson
    d.update(valueName, value)
    d
  }
  
}


/** Реализация [[IValuedAnswer]] для передачи нового контекста экшена на сервер. */
case class MAnswerCtx(
  replyTo   : Option[String],
  mctx      : MJsCtxT
) extends IValuedAnswer {

  override def value = mctx.toJson
  override def valueName = JsCommand.MCTX_FN
}


/** Дефолтовая реализация [[IValuedAnswer]]. */
case class MAnswer(
  replyTo   : Option[String],
  value     : Any,
  valueName : String = JsCommand.VALUE_FN
) extends IValuedAnswer
