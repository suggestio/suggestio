package io.suggest.sc.sjs.c.scfsm.grid

import io.suggest.sc.sjs.c.scfsm.foc.IOnFocusBase
import io.suggest.sc.sjs.c.scfsm.{ResizeDelayed, ScFsmStub}
import io.suggest.sc.sjs.m.mfoc.MFocSd
import io.suggest.sc.sjs.m.mgrid._
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sc.sjs.vm.grid.{GContainer, GContent, GRoot}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 10:13
 * Description: Аддоны для поддержки сборки состояний выдачи, связанных с возможностями плитки карточек.
 */
trait OnGridBase extends ScFsmStub {

  /** Трейт для подмешивания логики синхронной реакции на ресайз экрана. */
  trait GridHandleViewPortChangedSync extends FsmState {
    // С плиткой карточек есть кое-какие тонкости при ресайзе viewport'а: карточки под экран подгоняет
    // сервер. Нужно дождаться окончания ресайза с помощью таймеров, затем загрузить новую плитку с сервера.
    override def _viewPortChanged(): Unit = {
      super._viewPortChanged()

      // Обновить параметры grid-контейнера. Это позволяет исправить высоту контейнера.
      for (groot <- GRoot.find()) {
        groot.reInitLayout(_stateData)
      }

      // Заодно пересчитать сразу параметры контейнера сетки.
      // Эти параметры не очень-то корректны, т.к. с сервера придут новые размеры ячеек сетки, и надо будет пересчитывать снова.
      val sd0 = _stateData
      val gContSzOpt = sd0.screen
        .map( sd0.grid.getGridContainerSz )

      // Обновить параметры контейнера сетки.
      for {
        gContSz   <- gContSzOpt
        gcontent  <- GContent.find()
      } {
        // Обновить контейнер сетки.
        gcontent.setContainerSz(gContSz)

        // Сохранить новые данные контейнера в состояние.
        _stateData = sd0.copy(
          grid    = sd0.grid.copy(
            state = sd0.grid.state.copy(
              contSz = gContSzOpt
            )
          )
        )
      }
    }
  }

}


/** Утиль для сборка состояний сетки. */
trait OnGrid extends Append with ResizeDelayed with IOnFocusBase with OnGridBase {

  /** Поддержка реакции на клики по карточкам в выдаче. */
  trait GridBlockClickStateT extends FsmEmptyReceiverState with IStartFocusOnAdState {

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
          currAdId = gblock.madId
        ))
      )
      become(_startFocusOnAdState, sd1)
    }

  }


  /** Трейт для сборки состояний, в которых доступна сетка карточек, клики по ней и скроллинг. */
  trait OnGridStateT
    extends GridBlockClickStateT with GridAdsWaitLoadStateT
      with DelayResize with HandleResizeDelayed
      with GridHandleViewPortChangedSync
  {

    protected def RSZ_THRESHOLD_PX = 100

    /** Требуется ли отложенный ресайз?
      * Да, если по горизонтали контейнер изменился слишком сильно. */
    private def _isNeedResizeDelayed(): Boolean = {
      // Посчитать новые размер контейнера. Используем Option как Boolean.
      val sd0 = _stateData

      val needResizeOpt = for {
        rsz <- sd0.resizeOpt
        // Прочитать текущее значение ширины в стиле. Она не изменяется до окончания ресайза.
        gc  <- GContainer.find()
        w   <- gc.styleWidthPx
        // Посчитать размер контейнера.
        cwCm <- rsz.gContSz.orElse {
          rsz.screen
            .map( sd0.grid.getGridContainerSz )
        }
        // Если ресайза маловато, то вернуть здесь None.
        if Math.abs(cwCm.cw - w) >= RSZ_THRESHOLD_PX
      } yield {
        // Значение не важно абсолютно
        1
      }
      needResizeOpt.isDefined
    }

    override def receiverPart: Receive = super.receiverPart.orElse {
      // Вертикальный скроллинг в плитке.
      case vs: GridScroll =>
        handleVScroll(vs)
    }

    /** Реакция на вертикальный скроллинг. Нужно запросить с сервера ещё карточек. */
    protected def handleVScroll(vs: GridScroll): Unit = {
      if (!_stateData.grid.state.fullyLoaded) {
        become(_loadMoreState)
      }
    }



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
          resizeOpt = None,
          grid = sd0.grid.copy(
            state = sd0.grid.state.nothingLoaded().copy(
              adsPerLoad = MGridState.getAdsPerLoad(mscreen)
            ),
            builderStateOpt = None
          )
        )
      }
      super._findAdsReady(mfa)
    }

    /** Реакция на наступление таймаута ожидания ресайза плитки. */
    override def _handleResizeDelayTimeout(): Unit = {
      if (_isNeedResizeDelayed()) {
        val sd0 = _stateData
        for (mscreen <- sd0.screen) {
          // TODO Opt Если существенное по горизонтали, но оно осталось ~кратно ячейкам, то просто перестроить выдачу: _rebuildGridOnPanelChange
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
    }

    /** Состояние, когда все карточки загружены. */
    override final def _adsLoadedState: FsmState = null

    /** Состояние, когда запрос карточек не удался. */
    override final def _findAdsFailedState: FsmState = null

    /** Состояние подгрузки ещё карточек. */
    protected def _loadMoreState: FsmState

  }

}



