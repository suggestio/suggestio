package io.suggest.sys.mdr.m

import diode.FastEq
import io.suggest.sys.mdr.MMdrActionInfo
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.18 18:46
  * Description: Модель состояния диалога отказа в размещении.
  */
object MMdrRefuseDialogS {

  implicit object MMdrRefuseDialogSFastEq extends FastEq[MMdrRefuseDialogS] {
    override def eqv(a: MMdrRefuseDialogS, b: MMdrRefuseDialogS): Boolean = {
      (a.actionInfo ===* b.actionInfo) &&
      (a.reason ===* b.reason)
    }
  }

  implicit def univEq: UnivEq[MMdrRefuseDialogS] = UnivEq.derive

}


/**
  * Контейнер данных диалога отказа в размещении.
  *
  * @param actionInfo Набор параметров, описывающих модерируемое размещение.
  * @param reason Причина, набираемая модератором в окошке диалога.
  */
case class MMdrRefuseDialogS(
                              actionInfo  : MMdrActionInfo,
                              reason      : String = ""
                            ) {

  def withReason(reason: String) = copy(reason = reason)

}
