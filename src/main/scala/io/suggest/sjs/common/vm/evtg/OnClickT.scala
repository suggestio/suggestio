package io.suggest.sjs.common.vm.evtg

import io.suggest.sjs.common.util.TouchUtil
import org.scalajs.dom.Event

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.06.15 10:11
 * Description: Аддон для view'ов для упрощенного вешанья click-listener'ов на safe-элементы.
 */
trait OnClickT extends OnMouseClickT {

  override protected def _clickEvtNames: TraversableOnce[String] = {
    TouchUtil.clickEvtNames
  }

  /** Активна ли блокировка touch-событий?
    * Если да, то touchend события НЕ будут рассматриваться как клики. */
  protected def isTouchLocked: Boolean

  /**
   * Собрать функция-листенер click-событий.
   * @param f Ядро листенера.
   * @tparam T Тип события.
   * @return js.Function.
   */
  override protected def _getClickListener[T <: Event](f: T => _): js.Function1[T, _] = {
    if (TouchUtil.IS_TOUCH_DEVICE) {
      // На touch-девайсе нужно распознавать клики среди touch-событий
      { e: T =>
        val itl = isTouchLocked
        //println("evt: " + e + ", tg = " + e.currentTarget + "#" + e.currentTarget.asInstanceOf[HTMLElement].id + " , isTouchLocked = " + itl)
        if (!itl) {
          f(e)
        }
      }

    } else {
      // На обычном девайсе фильтрация кликов не требуется
      super._getClickListener[T](f)
    }
  }

}


/** Вешанье событий на только мышиные клики. */
trait OnMouseClickT extends EventTargetVmT {
  
  protected def _clickEvtNames: TraversableOnce[String] = {
    TouchUtil.EVT_NAMES_MOUSE_CLICK
  }
  
  /**
   * Сборка обработчика click-событий
   * @param f Обработчик события
   * @tparam T Тип события.
   */
  protected def onClick[T <: Event](f: T => _): Unit = {
    _onClickRaw( _getClickListener(f) )
  }
  
  /** Комбинация из onClick и _getClickListener. */
  protected def _onClickRaw[T <: Event](listener: js.Function1[T, _]): Unit = {
    for (evtName <- _clickEvtNames) {
      addEventListener(evtName)(listener)
    }
  }
 
  /**
   * Собрать функция-листенер click-событий.
   * @param f Ядро листенера.
   * @tparam T Тип события.
   * @return js.Function.
   */
  protected def _getClickListener[T <: Event](f: T => _): js.Function1[T, _] = {
    // На обычном девайсе фильтрация кликов не требуется
    f
  }
  
}
