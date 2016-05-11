package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.magent.{MResizeDelay, ResizeDelayTimeout}
import io.suggest.sc.sjs.vm.grid.GContainer
import io.suggest.sjs.common.controller.DomQuick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.05.16 11:47
  * Description: Трейт для поддержки отложенного ресайза по горизонтали и реакции на него.
  * Используется в сетки (Grid) и Focused. Возможно, ещё где-то будет использоваться.
  */
trait ResizeDelayed extends ScFsmStub {

  /** Трейт реакции на viewPortChanged() для опционального откладывания ресайза на будущее. */
  trait DelayResize extends FsmState {

    /** Время ожидания окончания окна, после которого необходимо произвести изменения в плитке. */
    protected def RESIZE_DELAY_MS = 300

    /** Определить быстро, требуется ли откладывание ресайза? [false] */
    def _isNeedResizeDelayed(): Boolean = false

    /** Реакция на сигнал об изменении размеров окна или экрана устройства. */
    override def _viewPortChanged(): Unit = {
      super._viewPortChanged()

      // С плиткой карточек есть кое-какие тонкости при ресайзе viewport'а: карточки под экран подгоняет
      // сервер. Нужно дождаться окончания ресайза с помощью таймеров, затем загрузить новую плитку с сервера.
      val sd0 = _stateData

      // Отменить старый таймер. Изначально было внутри fold.
      for (grSd0 <- sd0.resizeOpt) {
        DomQuick.clearTimeout(grSd0.timerId)
      }

      // Если нет изменений по горизонтали, то можно таймер ресайза не запускать/не обновлять.
      if (_isNeedResizeDelayed()) {
        // Запуск нового таймера ресайза
        val timerGen = System.currentTimeMillis()
        val timerId = DomQuick.setTimeout(RESIZE_DELAY_MS) { () =>
          _sendEventSync(ResizeDelayTimeout(timerGen))
        }

        // Собрать обновлённое состояние ресайза.
        // TODO Opt если не будет новых полей, то можно унифицировать apply и copy.
        val grSd2 = MResizeDelay(
          timerId = timerId,
          timerGen = timerGen
        )

        // Сохранить обновлённое состояние FSM.
        _stateData = sd0.copy(
          resizeOpt = Some(grSd2)
        )
      }   // if needResize
    }     // _viewPortChanged()
  }


  /** Поддержка отложенного ресайза в ресивере состояния. */
  trait HandleResizeDelayed extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart.orElse {
      // Сигнал таймера о необходимости исполнения ресайза плитки.
      case ResizeDelayTimeout(gen) =>
        if ( _stateData.resizeOpt.exists(_.timerGen == gen) )
          _handleResizeDelayTimeout()
    }

    /** Реакция на отложенный ресайз. */
    def _handleResizeDelayTimeout(): Unit

  }


  /** Реализация детектирования необходимости запуска таймера в DelayResize на основе данных сетки. */
  trait DelayHorizResizeUsingGrid extends DelayResize {

    protected def RSZ_THRESHOLD_PX = 100

    /** Требуется ли отложенный ресайз?
      * Да, если по горизонтали контейнер изменился слишком сильно. */
    override def _isNeedResizeDelayed(): Boolean = {
      super._isNeedResizeDelayed() || {
        // Посчитать новые размер контейнера. Используем Option как Boolean.
        val sd0 = _stateData

        val needResizeOpt = for {
          scr <- sd0.screen
          // Прочитать текущее значение ширины в стиле. Она не изменяется до окончания ресайза.
          gc  <- GContainer.find()
          w   <- gc.styleWidthPx
          // Посчитать размер контейнера.
          cwCm = sd0.grid.getGridContainerSz(scr)
          // Если ресайза маловато, то вернуть здесь None.
          if Math.abs(cwCm.cw - w) >= RSZ_THRESHOLD_PX
        } yield {
          // Значение не важно абсолютно
          1
        }
        needResizeOpt.isDefined
      }
    }
  }

}
