package io.suggest.sc.sjs.m.msrv.foc.find

import scala.scalajs.js
import js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.06.15 17:31
 * Description: Интерфейс сырого json-ответа focused ads v2 API.
 */
sealed trait MRespJson extends js.Object {

  /** Доступ к FOCUSED_ADS_FN. */
  val fads: js.Array[Dictionary[Any]] = js.native

}

object MRespJson {
  def apply(json: Any): MRespJson = {
    json.asInstanceOf[MRespJson]
  }
}
