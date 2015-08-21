package io.suggest.sc.sjs.m.mfoc

import io.suggest.model.IVeryLightEnumeration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.15 11:29
 * Description: Режимы свайпа в focused-выдаче.
 */
object MFocTouchModes extends IVeryLightEnumeration {

  /** Класс-реализация элементов модели. */
  sealed protected[this] abstract class Val extends ValT {
    def isVScroll: Boolean
    def isShift: Boolean = !isVScroll
  }

  override type T = Val

  /** Режим вертикального скроллинга. */
  val Scroll: T = new Val {
    override def isVScroll = true
    override def toString = "o"
  }

  /** Режим горизонтального переключения карточек. */
  val Shift: T = new Val {
    override def isVScroll = false
    override def toString = "i"
  }

}
