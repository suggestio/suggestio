package io.suggest.lk.m

import diode.FastEq
import diode.data.Pot
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.17 18:03
  * Description: Модель состояния компонента удаления узла.
  */
object MDeleteConfirmPopupS {

  implicit object MDeleteConfirmPopupSFastEq extends FastEq[MDeleteConfirmPopupS] {
    override def eqv(a: MDeleteConfirmPopupS, b: MDeleteConfirmPopupS): Boolean = {
      a.request ===* b.request
    }
  }

  @inline implicit def univEq: UnivEq[MDeleteConfirmPopupS] = UnivEq.force

}


case class MDeleteConfirmPopupS(
                                 request: Pot[AnyRef] = Pot.empty
                               ) {

  def withRequest(req2: Pot[AnyRef]) = copy(request = req2)

}
