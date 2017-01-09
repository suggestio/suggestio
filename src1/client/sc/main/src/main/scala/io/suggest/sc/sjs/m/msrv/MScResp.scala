package io.suggest.sc.sjs.m.msrv

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import io.suggest.sc.ScConstants.Resp.RESP_ACTIONS_FN
import io.suggest.sjs.common.model.{Timestamped, TimestampedCompanion}

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.16 22:20
  * Description: JSON-модель и JSON-интерфейс абстрактного ответа сервера на тему чего-то там в выдаче.
  */
object MScResp {

  def apply(json: js.Any): MScResp = {
    MScResp( json.asInstanceOf[MScRespJson] )
  }

}


case class MScResp(json: MScRespJson) {

  lazy val actions: Seq[MScRespAction] = {
    json.actions
      .iterator
      .map(MScRespAction.apply)
      .toSeq
  }

}

@js.native
sealed trait MScRespJson extends js.Object {

  @JSName( RESP_ACTIONS_FN )
  var actions: js.Array[MScRespActionJson] = js.native

}


case class MScRespTimestamped(
  override val result: Try[MScResp],
  override val timestamp: Long
)
  extends Timestamped[MScResp]
object MScRespTimestamped extends TimestampedCompanion[MScResp]
