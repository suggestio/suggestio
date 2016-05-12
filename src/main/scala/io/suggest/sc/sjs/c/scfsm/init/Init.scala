package io.suggest.sc.sjs.c.scfsm.init

import io.suggest.common.event.WndEvents
import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.m.magent.{OrientationChange, WndResize}
import io.suggest.sc.sjs.v.global.DocumentView
import io.suggest.sc.sjs.vm.SafeWnd

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.15 17:25
 * Description: Поддержка состояний инициализации выдачи.
 * Это обычно синхронные состояния, которые решают на какое состояние переключаться при запуске.
 */
trait Init extends ScFsmStub {

  /** Трейт для сборки состояния самой первой инициализации.
    * Тут происходит normal-init, но дополнительно может быть строго одноразовая логика.
    * Состояние случается только один раз и синхронно. */
  protected trait FirstInitStateT extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      // TODO Это нужно вообще или нет?
      DocumentView.initDocEvents()

      // Добавляем реакцию на изменение размера окна/экрана.
      val w = SafeWnd
      w.addEventListener(WndEvents.RESIZE)( _signalCallbackF(WndResize) )
      w.addEventListener(WndEvents.ORIENTATION_CHANGE)( _signalCallbackF(OrientationChange) )

      // Провоцируем сохранение в состояние FSM текущих параметров экрана.
      _viewPortChanged()
    }
  }


  /** Трейт синхронной инициализации выдачи и ScFsm.
    * От First-init отличается тем, что логика тут только повторяемая.
    * Эта инициализация может вызываться более одного раза для в случае подавления ошибок. */
  protected trait NormalInitStateT extends FsmState {

    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // TODO Десериализовывать состояние из URL и выбирать состояние.
      // Сразу переключаемся на новое состояние.
      become(_geoScInitState)
    }

    /** Состояние инициализации решает, что необходимо запросить геолокацию у браузера.
      * Такое происходит, когда нет данных состояния в URL. */
    protected def _geoScInitState: FsmState

  }

}
