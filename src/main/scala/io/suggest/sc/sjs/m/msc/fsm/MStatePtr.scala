package io.suggest.sc.sjs.m.msc.fsm

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 11:22
 * Description: Модель сохраняемой в History API информации: порядковый номер в _states с конца.
 */

sealed trait MStatePtr extends js.Object {

  /** Номер состояния в стеке состояний модели MScFsm. */
  // TODO Изначально это называлось "index", но почему-то JSName() не отрабатывало.
  var i: Int = js.native

}


object MStatePtr {
  def apply(index: Int): MStatePtr = {
    val d = js.Dictionary[js.Any](
      "i" -> index
    )
    d.asInstanceOf[MStatePtr]
  }
}
