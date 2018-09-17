package io.suggest.ws.pool.m

import diode.FastEq
import diode.data.Pot
import org.scalajs.dom.WebSocket
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 18:07
  * Description: Модель состояния одного websocket-коннекшена.
  */
object MWsConnS {

  /** Поддержка FastEq для инстансов [[MWsConnS]]. */
  implicit object MWsConnSFastEq extends FastEq[MWsConnS] {
    override def eqv(a: MWsConnS, b: MWsConnS): Boolean = {
      (a.target ===* b.target) &&
        (a.conn ===* b.conn) &&
        (a.closeTimer ===* b.closeTimer)
    }
  }

  @inline implicit def univEq: UnivEq[MWsConnS] = UnivEq.derive

}


/** Класс-контейнер данных по одному websocket-коннекшену.
  *
  * @param target Данные для сборки ссылки.
  * @param conn WebSocket-коннекшен.
  * @param closeTimer Таймер автозакрытия коннекшена.
  */
case class MWsConnS(
                     target         : MWsConnTg,
                     conn           : Pot[WebSocket]    = Pot.empty,
                     closeTimer     : Option[Int]       = None
                   ) {

  def withConn(conn: Pot[WebSocket])          = copy(conn = conn)
  def withCloseTimer(closeTimer: Option[Int]) = copy(closeTimer = closeTimer)

}
