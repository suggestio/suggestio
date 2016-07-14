package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.c.scfsm.ScFsm
import io.suggest.sc.sjs.m.mgeo.NewGeoLoc
import io.suggest.sc.sjs.vm.mapbox.GlMapVm
import io.suggest.sjs.mapbox.gl.event.{MoveEnd, MoveStart, Moving}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 16:57
  * Description: Аддон для состояния перемещения карты пальцем/мышкой/колесиком и т.д.
  */
trait OnMove extends MapReady {

  /** Состояние, когда начато таскание карты.
    * В центре карты -- прицел, следующий за центром карты. */
  trait OnDragStateT extends StoreUpdateUserGeoLocStateT {

    private val _vm = GlMapVm( _stateData.glmap.get )

    override def afterBecome(): Unit = {
      super.afterBecome()

      // Необходимо выставить прицел на центр карты.
      _setCenterCurrent()
    }

    override def receiverPart: Receive = {
      val r: Receive = {
        // Самое частое событие: продолжается таскательство
        case dgg: Moving =>
          _moving(dgg)

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
    def _moving(dgg: Moving): Unit = {
      _setCenterCurrent()
    }

    /** Реакция на окончание движения на карте. */
    def _moveEnd(dge: MoveEnd): Unit = {
      // Уведомить ScFsm о необходимости перемещения root'а выдачи в новое место.
      ScFsm ! NewGeoLoc( _vm.center )

      // Вернутся в состояние ожидания.
      become(moveEndState)
    }

    /** Переключение на какое состояние после окончания move? */
    def moveEndState: FsmState

  }

}
