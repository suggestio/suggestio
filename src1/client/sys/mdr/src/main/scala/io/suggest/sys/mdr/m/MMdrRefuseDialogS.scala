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

  def empty = apply()

  implicit object MMdrRefuseDialogSFastEq extends FastEq[MMdrRefuseDialogS] {
    override def eqv(a: MMdrRefuseDialogS, b: MMdrRefuseDialogS): Boolean = {
      (a.reason ===* b.reason) &&
      (a.actionInfo ===* b.actionInfo)
    }
  }

  @inline implicit def univEq: UnivEq[MMdrRefuseDialogS] = UnivEq.derive

}


/**
  * Контейнер данных диалога отказа в размещении.
  *
  * @param actionInfo Набор параметров, описывающих модерируемое размещение.
  * @param reason Причина, набираемая модератором в окошке диалога.
  */
case class MMdrRefuseDialogS(
                              reason      : String                    = "",
                              actionInfo  : Option[MMdrActionInfo]    = None,
                            ) {

  def withActionInfo(actionInfo: Option[MMdrActionInfo]) = copy(actionInfo = actionInfo)
  def withReason(reason: String) = copy(reason = reason)

}
