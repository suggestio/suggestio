package io.suggest.sc.sjs.c.search.map

import io.suggest.sc.sjs.c.scfsm.ScFsm
import io.suggest.sc.sjs.m.mgeo.NewGeoLoc
import io.suggest.sc.sjs.vm.mapbox.GlMapVm
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.mapbox.gl.event.{MoveEnd, MoveStart, Moving}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 16:57
  * Description: Аддон для состояния перемещения карты пальцем/мышкой/колесиком и т.д.
  */
trait Drag extends Ready {

  /** Состояние, когда начато таскание карты.
    * В центре карты -- прицел, следующий за центром карты. */
  trait MapDragStateT extends HandleGeoLocStateT {

    private val _vm = GlMapVm( _stateData.mapInst.get.glmap )

    override def afterBecome(): Unit = {
      super.afterBecome()

      // Необходимо выставить прицел на центр карты.
      _setCenterCurrent()

      // Запустить таймер коррекции прицела на центр карты.
      // Используем таймер вместо MOVING event, т.к. толстый поток moving event'ов вызывает фактически
      // блокировку js-треда и все сопутствующие тормоза.
      val movEvt = Moving(null)
      val movingTimerId = DomQuick.setInterval(100) { () =>
        _sendEventSync( movEvt )
      }
      _setMovTimerId( Some(movingTimerId) )
    }


    protected[this] def _setMovTimerId( movTimerIdOpt: Option[Int], sd0: SD = _stateData ): Unit = {
      _stateData = sd0.copy(
        movingTimerId = movTimerIdOpt
      )
    }


    override def receiverPart: Receive = {
      val r: Receive = {
        // Самое частое событие: продолжается таскательство
        case _: Moving =>
          _moving()

        // Сигнал окончания таскания карты.
        case dge: MoveEnd =>
          _moveEnd(dge)

        // Во время незаконченного драга/скролла происходит ещё скролл, это нормально, игнорим.
        case _: MoveStart =>
          // do nothing
      }
      r.orElse( super.receiverPart )
    }


    /** Совместить текущую точку на центр карты. */
    def _setCenterCurrent(): Unit = {
      val vm = _vm
      val centerGp = vm.center
      vm.setCurrentPos(centerGp)
    }

    /** Реакция на таскание карты. */
    def _moving(): Unit = {
      _setCenterCurrent()
    }

    /** Реакция на окончание движения на карте. */
    def _moveEnd(dge: MoveEnd): Unit = {
      // Остановить таймер moving'а
      val sd0 = _stateData
      for (timerId <- sd0.movingTimerId) {
        DomQuick.clearInterval(timerId)
        _setMovTimerId(None, sd0)
      }

      // Уведомить ScFsm о необходимости перемещения root'а выдачи в новое место.
      ScFsm ! NewGeoLoc( _vm.center )

      // Вернутся в состояние ожидания.
      become(moveEndState)
    }

    /** Переключение на какое состояние после окончания move? */
    def moveEndState: FsmState

  }

}
