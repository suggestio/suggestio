package io.suggest.sys.mdr.m

import diode.FastEq
import diode.data.Pot
import io.suggest.sys.mdr.MNodeMdrInfo
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.10.18 22:04
  * Description: Корневая модель состояния компонента модерации карточек.
  */
object MSysMdrRootS {

  implicit object MSysMdrRootSFastEq extends FastEq[MSysMdrRootS] {
    override def eqv(a: MSysMdrRootS, b: MSysMdrRootS): Boolean = {
      (a.info ===* b.info)
    }
  }

}

case class MSysMdrRootS(
                         info       : Pot[MNodeMdrInfo]       = Pot.empty
                       ) {

  def withInfo(info: Pot[MNodeMdrInfo]) = copy(info = info)

}
