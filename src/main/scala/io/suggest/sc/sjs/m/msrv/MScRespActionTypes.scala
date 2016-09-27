package io.suggest.sc.sjs.m.msrv

import io.suggest.common.menum.{LightEnumeration, StrIdValT}
import io.suggest.sc.ScConstants

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.16 23:29
  * Description: Модель типов sc-resp-экшенов сервера.
  */
object MScRespActionTypes extends LightEnumeration with StrIdValT {

  sealed protected[this] class Val(override val strId: String) extends ValT

  override type T = Val

  override def maybeWithName(n: String): Option[T] = {
    n match {
      case Index.strId => Some(Index)
      case _           => None
    }
  }

  val Index: T = new Val(ScConstants.Resp.INDEX_RESP_ACTION)

}
