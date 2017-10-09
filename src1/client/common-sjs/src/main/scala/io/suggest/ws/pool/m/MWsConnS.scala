package io.suggest.ws.pool.m

import diode.FastEq
import diode.data.Pot
import org.scalajs.dom.WebSocket
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.UnivEqJsUtil._
import io.suggest.url.MHostUrl
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
      (a.hostUrl ===* b.hostUrl) &&
        (a.onMessageF ===* b.onMessageF) &&
        (a.conn ===* b.conn) &&
        (a.closeTimer ===* b.closeTimer)
    }
  }

  implicit def univEq: UnivEq[MWsConnS] = UnivEq.derive

}


/** Класс-контейнер данных по одному websocket-коннекшену.
  *
  * @param hostUrl Данные для сборки ссылки.
  * @param onMessageF Callback при получении сообщения из сокета.
  * @param conn WebSocket-коннекшен.
  * @param closeTimer Таймер автозакрытия коннекшена.
  */
case class MWsConnS(
                     hostUrl        : MHostUrl,
                     onMessageF     : WsCallbackF,
                     conn           : Pot[WebSocket]    = Pot.empty,
                     closeTimer     : Option[Int]       = None
                   ) {

  def withOnMessageF(onMessageF: WsCallbackF) = copy(onMessageF = onMessageF)
  def withConn(conn: Pot[WebSocket])          = copy(conn = conn)
  def withCloseTimer(closeTimer: Option[Int]) = copy(closeTimer = closeTimer)

}