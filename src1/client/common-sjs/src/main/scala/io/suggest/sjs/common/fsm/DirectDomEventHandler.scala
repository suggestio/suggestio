package io.suggest.sjs.common.fsm

import io.suggest.fsm.AbstractFsm
import org.scalajs.dom.{MouseEvent, TouchEvent}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.15 9:33
 * Description: Прямое получение UI-событий в FSM -- это когда вместо заворачивания в сигнал и
 * отправки в _state.receiver используется пробрасывание событий через статическое public API напрямую.
 * Иными словами, некоторые DOM-события можно обрабатывать по кратчайшему пути с помощью этих трейтов.
 *
 * Это полезно для высокочастотных событий, таких как mousemove, touchmove, которые могут заваливать
 * js heap частыми аллокациями классов-контейнеров очень сильно, а _receiver является комбинированной
 * PartialFunction, производительность которой имеет свойство проседать. На слабых мобильных устройствах
 * оптимизация может существенно улучшить производительность выдачи на длинных receiver'ах.
 */
trait IDirectDomEventHandler {

  /** Быстрый приём высокочастотных событий mousemove. */
  def onMouseMove(event: MouseEvent): Unit

  /** Быстрый приём высокочастотных событий touchmove. */
  def onTouchMove(event: TouchEvent): Unit
}


/** Пустая реализация [[IDirectDomEventHandler]]. */
trait DirectDomEventHandlerDummy extends IDirectDomEventHandler {
  override def onMouseMove(event: MouseEvent): Unit = {}
  override def onTouchMove(event: TouchEvent): Unit = {}
}


/** Wrapper trait для [[IDirectDomEventHandler]]. */
trait DirectDomEventHandlerWrapper extends IDirectDomEventHandler {

  /** Завёрнутая реализация [[IDirectDomEventHandler]]. */
  protected def _dEvtHandler: IDirectDomEventHandler

  override def onMouseMove(event: MouseEvent) = _dEvtHandler.onMouseMove(event)
  override def onTouchMove(event: TouchEvent) = _dEvtHandler.onTouchMove(event)
}


/**
 * FSM-аддон для API с реализацией вышеуказанного функционала..
 * По задумке -- реализация FSM -- это враппер, а сам обработчик с аналогичным API лежит в _stateData.
 * В FsmState подмешивается [[DirectDomEventHandlerDummy]] или реализуется [[IDirectDomEventHandler]].
 */
trait DirectDomEventHandlerFsm extends AbstractFsm with DirectDomEventHandlerWrapper {

  override type State_t <: FsmState with IDirectDomEventHandler

  @inline
  override protected def _dEvtHandler = _state

}
