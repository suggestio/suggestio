package io.suggest.sc.sjs.m.msrv.tags.find

import io.suggest.sjs.common.model.FromJsonT
import io.suggest.sc.TagSearchConstants.Resp._

import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.15 21:19
 * Description: Модель ответа сервера на запрос поиска тегов.
 */
object MftResp extends FromJsonT {

  override type T = MftResp

  override def fromJson(raw: Any): MftResp = {
    val d = raw.asInstanceOf[Dictionary[Any]]
    apply(d)
  }

}


case class MftResp(d: Dictionary[Any]) {

  lazy val foundCount: Int = {
    d(FOUND_COUNT_FN).asInstanceOf[Double].toInt
  }

  def render: String = {
    d(RENDERED_FN).asInstanceOf[String]
  }

}
