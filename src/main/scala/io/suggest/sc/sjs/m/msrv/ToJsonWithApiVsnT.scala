package io.suggest.sc.sjs.m.msrv

import io.suggest.sjs.common.model.ToJsonDictDummyT
import io.suggest.sc.ScConstants.ReqArgs.VSN

import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.15 20:56
 * Description: common-trait для моделей, отсылающих на сервер запросы, содержащие поле версии API.
 */
trait ToJsonWithApiVsnT extends ToJsonDictDummyT {

  override def toJson: Dictionary[Any] = {
    val d = super.toJson
    d(VSN) = MSrv.API_VSN
    d
  }

}
