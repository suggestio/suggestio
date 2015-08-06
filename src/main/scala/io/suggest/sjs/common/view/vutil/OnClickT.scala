package io.suggest.sjs.common.view.vutil

import io.suggest.sjs.common.util.TouchUtil
import io.suggest.sjs.common.view.safe.evtg.SafeEventTargetT
import org.scalajs.dom.Event

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.06.15 10:11
 * Description: Аддон для view'ов для упрощенного вешанья click-listener'ов на safe-элементы.
 */
trait OnClickT {

  /** Активна ли блокировка touch-событий?
    * Если да, то touchend события НЕ будут рассматриваться как клики. */
  protected def isTouchLocked: Boolean

  /**
   * Сборка обработчика click-событий
   * @param el элемент.
   * @param f Обработчик события
   * @tparam T Тип события.
   */
  protected def onClick[T <: Event](el: SafeEventTargetT)(f: T => _): Unit = {
    _onClickRaw(el)( _getClickListener(f) )
  }

  // api разбито на части в целях оптимизации в реализациях. Т.е. когда использовать один инстанс листенера неск.раз.

  /** Комбинация из onClick и _getClickListener. */
  protected def _onClickRaw[T <: Event](el: SafeEventTargetT)(listener: js.Function1[T, _]): Unit = {
    for (evtName <- TouchUtil.clickEvtNames) {
      el.addEventListener(evtName)(listener)
    }
  }

  /**
   * Собрать функция-листенер click-событий.
   * @param f Ядро листенера.
   * @tparam T Тип события.
   * @return js.Function.
   */
  protected def _getClickListener[T <: Event](f: T => _): js.Function1[T, _] = {
    if (TouchUtil.isTouchDevice) {
      // На touch-девайсе нужно распознавать клики среди touch-событий
      { e: T =>
        if (!isTouchLocked) {
          f(e)
        }
      }

    } else {
      // На обычном девайсе фильтрация кликов не требуется
      f
    }
  }

}
