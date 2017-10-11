package io.suggest.ws.pool.m

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 18:06
  * Description: Состояние пула WS-коннекшенов.
  */
object MWsPoolS {

  def empty = apply()

  /** Поддержка FastEq для инстансов [[MWsPoolS]]. */
  implicit object MWsPoolSFastEq extends FastEq[MWsPoolS] {
    override def eqv(a: MWsPoolS, b: MWsPoolS): Boolean = {
      a.conns ===* b.conns
    }
  }

  implicit def univEq: UnivEq[MWsPoolS] = UnivEq.derive

}


/** Класс-контейнер данных состояния пула websocket-коннекшенов.
  *
  * @param conns Карта коннекшенов, где ключ -- это данные ws/wss-url.
  */
case class MWsPoolS(
                     conns        : Map[MWsConnTg, MWsConnS]     = Map.empty
                   ) {

  def withConns(conns: Map[MWsConnTg, MWsConnS])       = copy(conns = conns)

}
