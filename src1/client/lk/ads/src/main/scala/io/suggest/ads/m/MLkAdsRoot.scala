package io.suggest.ads.m

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:22
  * Description: Корневая модель circuit-состояния [[io.suggest.ads.LkAdsCircuit]].
  */
object MLkAdsRoot {

  implicit object MLkAdsRootFastEq extends FastEq[MLkAdsRoot] {
    override def eqv(a: MLkAdsRoot, b: MLkAdsRoot): Boolean = {
      a.currNode ===* b.currNode
    }
  }

  implicit def univEq: UnivEq[MLkAdsRoot] = UnivEq.derive

}


/** Класс root-модели состояния react-формы управления карточками.
  *
  * @param currNode Модель данных состояния по текущему узлу.
  */
case class MLkAdsRoot(
                       currNode     : MCurrNodeS
                     ) {

  def withCurrNode(currNode: MCurrNodeS) = copy(currNode = currNode)

}
