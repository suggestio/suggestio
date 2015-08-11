package io.suggest.sc.sjs.m.msc.fsm.state

import io.suggest.sc.sjs.c.{HeaderCtl, GridCtl}
import io.suggest.sc.sjs.m.msc.fsm.{MCatMeta, IScState}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.06.15 18:48
 * Description: Аддон для модели MScState для добавления поддержки поля cat с опциональными
 * данными по текущей категории.
 */
@deprecated("FSM-MVM", "2015.aug.11")
trait CatT extends IScState {

  override type T <: CatT

  /** Состояние текущей категории, если есть. */
  def cat       : Option[MCatMeta]

  def applyCatChanges(oldState: CatT): Unit = {
    val _cat = cat
    val _oldCat = oldState.cat
    if (_cat != _oldCat) {
      // Изменилась категория. Нужно обновить плитку.
      GridCtl.reFindAds()
      // Выставить или убрать cat class из header.
      HeaderCtl.changeGlobalCat(_cat, prevCatMeta = _oldCat)
    }
  }

  override def applyChangesSince(oldState: T): Unit = {
    super.applyChangesSince(oldState)
    applyCatChanges(oldState)
  }

}
