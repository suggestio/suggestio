package io.suggest.sc.sjs.c.scfsm.grid

import io.suggest.sc.sjs.c.scfsm.grid
import io.suggest.sc.sjs.m.mfoc.MFocSd
import io.suggest.sc.sjs.m.mgrid._
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sc.sjs.vm.grid.{GContainer, GContent, GRoot}
import io.suggest.sjs.common.controller.DomQuick

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 10:13
 * Description: Аддон для поддержки сборки состояний выдачи, связанных с возможностями плитки карточек.
 */
trait OnGrid extends grid.Append {

  /** Время ожидания окончания окна, после которого необходимо произвести изменения в плитке. */
  private def GRID_RESIZE_TIMEOUT_MS = 300

  /** Поддержка реакции на клики по карточкам в выдаче. */
  trait GridBlockClickStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart.orElse {
      // Клик по одной из карточек в сетке оных.
      case gbc: GridBlockClick =>
        _handleGridBlockClick( gbc )
    }

    /** Обработка кликов по карточкам в сетке. */
    protected def _handleGridBlockClick(gbc: IGridBlockClick): Unit = {
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
  trait OnGridStateT extends GridBlockClickStateT with GridAdsWaitLoadStateT {

    override def receiverPart: Receive = super.receiverPart.orElse {
      // Вертикальный скроллинг в плитке.
      case vs: GridScroll =>
        handleVScroll(vs)
      // Сигнал завершения ресайза
      case GridResizeTimeout(gen) =>
        if ( _stateData.grid.resizeOpt.exists(_.timerGen == gen) )
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

      // Обновить параметры grid-контейнера.
      for (groot <- GRoot.find()) {
        groot.reInitLayout(sd0)
      }

      // Отменить старый таймер. Изначально было внутри fold.
      for (grSd0 <- sd0.grid.resizeOpt) {
        DomQuick.clearTimeout(grSd0.timerId)
      }

      // Посчитать новые размер контейнера. Используем Option как Boolean.
      val needResizeOpt = for {
        scr <- sd0.screen
        // Прочитать текущее значение ширины в стиле. Она не изменяется до окончания ресайза.
        gc  <- GContainer.find()
        w   <- gc.styleWidthPx
        // Посчитать размер контейнера.
        cwCm = sd0.grid.getGridContainerSz(scr)
        // Если ресайза маловато, то вернуть здесь None.
        if Math.abs(cwCm.cw - w) >= 100
      } yield {
        // Значение не важно абсолютно
        1
      }
      val needResize = needResizeOpt.isDefined

      // Если нет изменений по горизонтали, то можно таймер ресайза не запускать/не обновлять.
      if (needResize) {
        // Запуск нового таймера ресайза
        val timerGen = System.currentTimeMillis()
        val timerId = DomQuick.setTimeout(GRID_RESIZE_TIMEOUT_MS) { () =>
          _sendEventSync(GridResizeTimeout(timerGen))
        }

        // Собрать обновлённое состояние ресайза.
        // TODO Opt если не будет новых полей, то можно унифицировать apply и copy.
        val grSd2 = sd0.grid.resizeOpt.fold[MGridResizeState] {
          // Ресайз только что начался, инициализировать новое состояние ресайза.
          MGridResizeState(
            timerId = timerId,
            timerGen = timerGen
          )
        } { grSd0 =>
          grSd0.copy(
            timerId = timerId,
            timerGen = timerGen
          )
        }

        // Сохранить обновлённое состояние FSM.
        _stateData = sd0.copy(
          grid = sd0.grid.copy(
            resizeOpt = Some(grSd2)
          )
        )
      }   // if needResize
    }     // _viewPortChanged()


    /** FSM-реакция на получение положительного ответа от сервера по поводу карточек сетки.
      *
      * @param mfa инстанс ответа MFindAds.
      */
    override def _findAdsReady(mfa: MFindAds): Unit = {
      for (gc <- GContainer.find()) {
        gc.clear()
      }
      val sd0 = _stateData
      for (mscreen <- sd0.screen) {
        _stateData = sd0.copy(
          grid = sd0.grid.copy(
            state = sd0.grid.state.nothingLoaded().copy(
              adsPerLoad = MGridState.getAdsPerLoad(mscreen)
            ),
            builderStateOpt = None,
            resizeOpt = None
          )
        )
      }
      super._findAdsReady(mfa)
    }

    /** Реакция на наступление таймаута ожидания ресайза плитки. */
    def _handleGridResizeTimeout(): Unit = {
      // TODO Opt Если *существенное* изменение по горизонтали, то нужно перезагружать выдачу.
      // TODO Opt Если существенное по горизонтали, но оно осталось ~кратно ячейкам, то просто перестроить выдачу: _rebuildGridOnPanelChange
      // TODO Если НЕсущественное по горизонтали, то можно оставлять всё как есть.
      val sd0 = _stateData
      for (mscreen <- sd0.screen) {
        val sd1 = sd0.copy(
          grid = sd0.grid.copy(
            state = MGridState(
              adsPerLoad = MGridState.getAdsPerLoad(mscreen)
            )
          )
        )
        _startFindGridAds(sd1)
      }
    }

    /** Состояние, когда все карточки загружены. */
    override final def _adsLoadedState: FsmState = null

    /** Состояние, когда запрос карточек не удался. */
    override final def _findAdsFailedState: FsmState = null

    /** Состояние подгрузки ещё карточек. */
    protected def _loadMoreState: FsmState

  }

}



