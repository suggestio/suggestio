package io.suggest.ads.m

import diode.FastEq
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:32
  * Description: Модель состояния текущего узла.
  */
object MCurrNodeS {

  implicit object MCurrNodeSFastEq extends FastEq[MCurrNodeS] {
    override def eqv(a: MCurrNodeS, b: MCurrNodeS): Boolean = {
      a.nodeKey ===* b.nodeKey
    }
  }

  implicit def univEq: UnivEq[MCurrNodeS] = UnivEq.derive

}


/** Контейнер данных по текущему узлу, на котором открыта форм.
  *
  * @param nodeKey Ключ до текущего узла.
  */
case class MCurrNodeS(
                       nodeKey: RcvrKey
                     )
