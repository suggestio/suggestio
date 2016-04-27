package io.suggest.sc.sjs.c.scfsm.grid

import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.m.mfoc.MFocSd
import io.suggest.sc.sjs.m.mgrid._
import io.suggest.sjs.common.controller.DomQuick

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 10:13
 * Description: Аддон для поддержки сборки состояний выдачи, связанных с возможностями плитки карточек.
 */
trait OnGrid extends ScFsmStub {

  /** Время ожидания окончания ресайза, после которого необходимо отресайзить плитку. */
  private def GRID_RESIZE_TIMEOUT_MS = 300

  trait GridBlockClickStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Клик по одной из карточек в сетке оных.
      case gbc: GridBlockClick =>
        handleGridBlockClick( gbc )
    }

    /** Обработка кликов по карточкам в сетке. */
    protected def handleGridBlockClick(gbc: IGridBlockClick): Unit = {
      val gblock = gbc.gblock
      val sd0 = _stateData
      // Минимально инициализируем focused-состояние и переключаем логику на focused.
      val sd1 = sd0.copy(
        focused = Some(MFocSd(
          gblock = Some(gblock)
        ))
      )
      become(_startFocusOnAdState, sd1)
    }

    protected def _startFocusOnAdState: FsmState

  }


  /** Трейт для сборки состояний, в которых доступна сетка карточек, клики по ней и скроллинг. */
  trait OnGridStateT extends GridBlockClickStateT {

    override def receiverPart: Receive = super.receiverPart.orElse {
      // Вертикальный скроллинг в плитке.
      case vs: GridScroll =>
        handleVScroll(vs)
      case GridResizeTimeout(gen) if _stateData.grid.resizeOpt.exists(_.timerGen == gen) =>
        _handleGridResizeTimeout()
    }

    /** Реакция на вертикальный скроллинг. Нужно запросить с сервера ещё карточек. */
    protected def handleVScroll(vs: GridScroll): Unit = {
      if (!_stateData.grid.state.fullyLoaded) {
        become(_loadMoreState)
      }
    }

    /** Реакция на сигнал об изменении размеров окна или экрана устройства. */
    override def _viewPortChanged(): Unit = {
      super._viewPortChanged()

      // С плиткой карточек есть кое-какие тонкости при ресайзе viewport'а: карточки под экран подгоняет
      // сервер. Нужно дождаться окончания ресайза с помощью таймеров, затем загрузить новую плитку с сервера.
      val sd0 = _stateData

      // Запуск нового таймера ресайза
      val timerGen = System.currentTimeMillis()
      val timerId  = DomQuick.setTimeout(GRID_RESIZE_TIMEOUT_MS) { () =>
        _sendEventSync( GridResizeTimeout(timerGen) )
      }

      // Собрать обновлённое состояние ресайза.
      val grSd2 = sd0.grid.resizeOpt.fold [MGridResizeState] {
        // Ресайз только что начался, инициализировать новое состояние ресайза.
        MGridResizeState(
          timerGen  = timerGen,
          timerId   = timerId
        )
      } { grSd0 =>
        // Отменить старый таймер
        DomQuick.clearTimeout( grSd0.timerId )
        grSd0.copy(
          timerId   = timerId,
          timerGen  = timerGen
        )
      }

      // Сохранить обновлённое состояние FSM.
      _stateData = sd0.copy(
        grid = sd0.grid.copy(
          resizeOpt = Some(grSd2)
        )
      )
    }


    /** Реакция на наступление таймаута ожидания ресайза плитки. */
    def _handleGridResizeTimeout(): Unit = {
      // TODO Необходимо оценить значимость изменений размера, перезагрузить плитку если требуется.
      warn("TODO resize the grid")
    }

    /** Состояние подгрузки ещё карточек. */
    protected def _loadMoreState: FsmState

  }

}



