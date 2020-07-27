package io.suggest.sys.mdr.m

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.18 18:45
  * Description: Модель состояния диалогов/попапов формы SysMdr.
  */
object MMdrDialogs {

  def empty = apply()

  implicit object MMdrDialogsFastEq extends FastEq[MMdrDialogs] {
    override def eqv(a: MMdrDialogs, b: MMdrDialogs): Boolean = {
      (a.refuse ===* b.refuse)
    }
  }

  @inline implicit def univEq: UnivEq[MMdrDialogs] = UnivEq.derive

  def refuse = GenLens[MMdrDialogs](_.refuse)

}


/** Контейнер состояний диалогов модерации.
  *
  * @param refuse Состояние refuse-диалога, даже если он закрыт.
  */
case class MMdrDialogs(
                        refuse: MMdrRefuseDialogS = MMdrRefuseDialogS.empty
                      )
