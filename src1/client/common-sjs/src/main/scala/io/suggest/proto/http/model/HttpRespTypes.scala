package io.suggest.proto.http.model

import enumeratum._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 12:44
  * Description: Модель типов данных возвращаемых http-ответов XHR.
  */

object HttpRespTypes extends Enum[HttpRespType] {

  case object Default extends HttpRespType {
    override def xhrResponseType = ""
  }

  case object ArrayBuffer extends HttpRespType {
    override def xhrResponseType = "arraybuffer"
  }

  case object Blob extends HttpRespType {
    override def xhrResponseType = "blob"
  }

  override def values = findValues
}

sealed abstract class HttpRespType extends EnumEntry {
  /** Значение .responseType для Ajax()/XHR. */
  def xhrResponseType: String
}
object HttpRespType {
  @inline implicit def univEq: UnivEq[HttpRespType] = UnivEq.derive
}
