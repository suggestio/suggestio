package io.suggest.sc.sjs.m.msrv

import scala.scalajs.js
import io.suggest.sc.ScConstants.Resp._
import io.suggest.sc.sjs.m.msrv.index.{MScRespIndex, MScRespIndexJson}

import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.16 22:29
  * Description: Модель ответа сервера с одним sc-resp-экшеном.
  */
case class MScRespAction(json: MScRespActionJson) {

  def action: MScRespActionType = {
    MScRespActionTypes.withName( json.action )
  }

  def index: Option[MScRespIndex] = {
    json.index
      .toOption
      .map( MScRespIndex.apply )
  }

}


@js.native
sealed trait MScRespActionJson extends js.Object {

  @JSName( ACTION_FN )
  var action: String = js.native

  @JSName( INDEX_RESP_ACTION )
  var index: UndefOr[MScRespIndexJson] = js.native

}
