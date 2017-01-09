package io.suggest.sjs.common.tags.search

import io.suggest.sc.TagSearchConstants.Resp._
import io.suggest.sjs.common.model.FromJsonT

import scala.scalajs.js.{Any, Dictionary, WrappedDictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.15 21:19
 * Description: Модель ответа сервера на запрос поиска тегов.
 */
object MTagSearchResp extends FromJsonT {

  override type T = MTagSearchResp

  override def fromJson(raw: Any): MTagSearchResp = {
    val d = raw.asInstanceOf[Dictionary[Any]]
    apply(d)
  }

}


case class MTagSearchResp(d: WrappedDictionary[Any]) {

  lazy val foundCount: Int = {
    d(FOUND_COUNT_FN)
      .asInstanceOf[Double]
      .toInt
  }

  def render: Option[String] = {
    d.get(RENDERED_FN)
      .asInstanceOf[Option[String]]
  }

}
