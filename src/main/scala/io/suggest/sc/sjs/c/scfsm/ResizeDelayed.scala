package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.magent.{IVpSzChanged, MResizeDelay, ResizeDelayTimeout}
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

    /** Реакция на сигнал об изменении размеров окна или экрана устройства. */
    override def _viewPortChanged(e: IVpSzChanged): Unit = {

      // Храним ранний инстанс состояния, чтобы можно было ниже по коду получить исходный screen и grid cont sz.
      val sd00 = _stateData

      super._viewPortChanged(e)

      // С плиткой карточек есть кое-какие тонкости при ресайзе viewport'а: карточки под экран подгоняет
      // сервер. Нужно дождаться окончания ресайза с помощью таймеров, затем загрузить новую плитку с сервера.
      val sd0 = _stateData

      // Отменить старый таймер. Изначально было внутри fold.
      for (grSd0 <- sd0.common.resizeOpt) {
        DomQuick.clearTimeout(grSd0.timerId)
      }

      // Если нет изменений по горизонтали, то можно таймер ресайза не запускать/не обновлять.
      // Запуск нового таймера ресайза
      val timerGen  = System.currentTimeMillis()
      val timeoutMs = if (e.delayAllowed) RESIZE_DELAY_MS else 0
      val timerId   = DomQuick.setTimeout(timeoutMs) { () =>
        _sendEventSync(ResizeDelayTimeout(timerGen))
      }

      // Собрать обновлённое состояние ресайза.
      val grSd2 = sd0.common.resizeOpt.fold[MResizeDelay] {
        MResizeDelay(
          timerId   = timerId,
          timerGen  = timerGen,
          // В начальное состояние ресайза пихаем самые начальные данные по viewport'у и начальному контейнеру сетки.
          screen    = sd00.common.screen,
          gContSz   = sd00.grid.state.contSz
        )
      } { mrd0 =>
        // Пробросить исходные данные по viewport'у в обновлённое состояние, выставив новый таймер.
        mrd0.copy(
          timerId   = timerId,
          timerGen  = timerGen
        )
      }

      // Сохранить обновлённое состояние FSM.
      _stateData = sd0.copy(
        common = sd0.common.copy(
          resizeOpt = Some(grSd2)
        )
      )
    }     // _viewPortChanged()
  }


  /** Поддержка отложенного ресайза в ресивере состояния. */
  trait HandleResizeDelayed extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart.orElse {
      // Сигнал таймера о необходимости исполнения ресайза плитки.
      case ResizeDelayTimeout(gen) =>
        if ( _stateData.common.resizeOpt.exists(_.timerGen == gen) )
          _handleResizeDelayTimeout()
    }

    /** Реакция на отложенный ресайз. */
    def _handleResizeDelayTimeout(): Unit


    /** Требуется ли отложенный ресайз?
      * Да, если по горизонтали контейнер изменился слишком сильно. */
    protected def _isScrWidthReallyChanged(): Boolean = {
      // Посчитать новые размер контейнера. Используем Option как Boolean.
      val sd0 = _stateData

      val needResizeOpt = for {
        rsz <- sd0.common.resizeOpt
        // Прочитать текущее значение ширины в стиле. Она не изменяется до окончания ресайза.
        screen1 = sd0.common.screen
        // При повороте телефонного экрана с открытой боковой панелью бывает, что ширина контейнера не меняется в отличие от ширины экрана.
        // Такое было на one plus one, когда использовался rsz.gContSz, что ломало всё счастье из-за боковых отступов. Поэтому используем сравнение ширин экрана.
        screen0 = rsz.screen
        // Если ресайза маловато, то вернуть здесь None.
        if Math.abs(screen0.width - screen1.width) >= 50
      } yield {
        // Значение не важно абсолютно
        1
      }
      needResizeOpt.isDefined
    }

  }

}
