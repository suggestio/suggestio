package io.suggest.sc.sjs.m.msc.fsm.state

import io.suggest.sc.sjs.c.GridCtl
import io.suggest.sc.sjs.m.msc.fsm.{MCatMeta, IScState}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.06.15 18:48
 * Description: Аддон для модели MScState для добавления поддержки поля cat с опциональными
 * данными по текущей категории.
 */
trait CatT extends IScState {

  override type T <: CatT

  /** Состояние текущей категории, если есть. */
  def cat       : Option[MCatMeta]

  def applyCatChanges(oldState: CatT): Unit = {
    val _cat = cat
    if (_cat != oldState.cat) {
      // Изменилась категория. Нужно обновить плитку.
      // TODO Выставить или убрать cat class из header.
      GridCtl.reFindAds()
    }
  }

  override def applyChangesSince(oldState: T): Unit = {
    super.applyChangesSince(oldState)
    applyCatChanges(oldState)
  }

}
