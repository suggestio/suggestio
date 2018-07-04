package io.suggest.sc.m.search

import diode.FastEq
import diode.data.Pot
import io.suggest.sc.search.MSc3NodeInfo
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.07.18 21:44
  * Description: Состояние поиска в гео-вкладке панели поиска.
  * Неявно-пустая модель.
  */
object MGeoTabSearchS {

  def empty = apply()

  implicit object MGeoTabSearchSFastEq extends FastEq[MGeoTabSearchS] {
    override def eqv(a: MGeoTabSearchS, b: MGeoTabSearchS): Boolean = {
      a.req ===* b.req
    }
  }

  implicit def univEq: UnivEq[MGeoTabSearchS] = UnivEq.derive

}


/** Контейнер модели состояния поиска узлов на карте.
  *
  * @param req Поисковый реквест к серверу.
  */
case class MGeoTabSearchS(
                           req    : Pot[Seq[MSc3NodeInfo]]    = Pot.empty
                         ) {

  def withReq(req: Pot[Seq[MSc3NodeInfo]]) = copy(req = req)

}
