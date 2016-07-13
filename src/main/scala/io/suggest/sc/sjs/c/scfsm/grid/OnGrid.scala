package io.suggest.sc.sjs.c.scfsm.grid

import io.suggest.sc.sjs.c.scfsm.foc.IOnFocusBase
import io.suggest.sc.sjs.c.scfsm.{ResizeDelayed, ScFsmStub}
import io.suggest.sc.sjs.m.magent.IVpSzChanged
import io.suggest.sc.sjs.m.mfoc.{MFocCurrSd, MFocSd}
import io.suggest.sc.sjs.m.mgrid._
import io.suggest.sc.sjs.m.msc.MScSd
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sc.sjs.vm.grid.{GContainer, GContent, GRoot}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 10:13
 * Description: Аддоны для поддержки сборки состояний выдачи, связанных с возможностями плитки карточек.
 */

/** Контейнер очень базовых трейтов для сборки состояний, как-то связанных с сеткой. */
trait OnGridBase extends ScFsmStub with ResizeDelayed with Append {

  /** Интерфейс для поля, возвращающего инстанс состояния после возврата в/на плитку. */
  trait IBackToGridState {
    /** Состояние по "возвращению" на плитку. */
    def _backToGridState: FsmState
  }


  /** Трейт для подмешивания логики синхронной реакции на ресайз экрана. */
  trait GridHandleViewPortChangedSync extends FsmState {

    // С плиткой карточек есть кое-какие тонкости при ресайзе viewport'а: карточки под экран подгоняет
    // сервер. Нужно дождаться окончания ресайза с помощью таймеров, затем загрузить новую плитку с сервера.
    override def _viewPortChanged(e: IVpSzChanged): Unit = {
      super._viewPortChanged(e)

      // Обновить параметры grid-контейнера. Это позволяет исправить высоту контейнера.
      for (groot <- GRoot.find()) {
        groot.reInitLayout(_stateData)
      }

      // Заодно пересчитать сразу параметры контейнера сетки.
      // Эти параметры не очень-то корректны, т.к. с сервера придут новые размеры ячеек сетки, и надо будет пересчитывать снова.
      val sd0 = _stateData

      val gContSz = sd0.grid.getGridContainerSz( sd0.common.screen )
      // Обновить параметры контейнера сетки.
      for {
        gcontent  <- GContent.find()
      } {
        // Обновить контейнер сетки.
        gcontent.setContainerSz(gContSz)

        _handleNewGridContSz( Some(gContSz) )
      }
    }

    /** Опциональная реакция на наличие новых параметров grid-контейнера на руках.
      *
      * В grid-части происходит сохранение параметров контейнера в состояние.
      *
      * В focused-части этого не происходит, grid там не видна и не перестраивается.
      * Старое значение cwCm проверяется при выходе из focused части для возможного провоциорвания
      * пересчета сетки.
      */
    def _handleNewGridContSz(gContSzOpt: Option[ICwCm]): Unit = {}

  }


  /** Реализации поддержки отложенного ресайза плитки. */
  trait GridHandleResizeDelayed extends HandleResizeDelayed {

    protected def RSZ_THRESHOLD_PX = 50

    /** Требуется ли отложенный ресайз?
      * Да, если по горизонтали контейнер изменился слишком сильно. */
    protected def _isGridNeedResizeDelayed(): Boolean = {
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
        if Math.abs(screen0.width - screen1.width) >= RSZ_THRESHOLD_PX
      } yield {
        // Значение не важно абсолютно
        1
      }
      needResizeOpt.isDefined
    }

    /** Реакция на наступление таймаута ожидания ресайза плитки. */
    override def _handleResizeDelayTimeout(): Unit = {
      val sd0 = _stateData
      if (_isGridNeedResizeDelayed()) {
        // TODO Opt Если существенное по горизонтали, но оно осталось ~кратно ячейкам, то просто перестроить выдачу: _rebuildGridOnPanelChange
        val sd1 = sd0.copy(
          grid = sd0.grid.copy(
            state = MGridState(
              adsPerLoad = MGridState.getAdsPerLoad( sd0.common.screen )
            )
          )
        )
        _startFindGridAds(sd1)
      }
    }

  }


  protected[this] def _isNeedBlur(sd0: SD = _stateData): Boolean = {
    !sd0.grid.state.isDesktopView
  }

  /** Размыть плитку в фоне, если экран маловат.
    *
    * @return true, если blurring имел место быть. Иначе false. */
  protected[this] def _maybeBlurGrid(sd0: SD = _stateData): Unit = {
    val needBlur = _isNeedBlur(sd0)
    if (needBlur) {
      _blurGrid()
    }
  }
  protected[this] def _blurGrid(): Unit = {
    for (groot <- GRoot.find()) {
      groot.blur()
    }
  }
  /** Убрать размывку плитки, если она была, не проверяя размеры экрана на всякий случай. */
  protected[this] def _unBlurGrid(): Unit = {
    for (groot <- GRoot.find()) {
      groot.unblur()
    }
  }

}


/** Утиль для сборка состояний сетки. */
trait OnGrid extends OnGridBase with IOnFocusBase {

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
          current = MFocCurrSd(
            madId = gblock.madId.get,
            index = 0
          ),
          producerId = gblock.producerId
        ))
      )
      become(_startFocusOnAdState, sd1)
    }

  }


  /** Трейт для сборки состояний, в которых доступна сетка карточек, клики по ней и скроллинг. */
  trait OnGridStateT
    extends GridBlockClickStateT with GridAdsWaitLoadStateT
      with DelayResize with GridHandleResizeDelayed
      with GridHandleViewPortChangedSync
  {

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

      // Загрузить карточки в состояние.
      _stateData = {
        val sd0 = _stateData
        sd0.copy(
          common = sd0.common.copy(
            resizeOpt = None
          ),
          grid = sd0.grid.copy(
            state = sd0.grid.state.nothingLoaded().copy(
              adsPerLoad = MGridState.getAdsPerLoad( sd0.common.screen )
            ),
            builderStateOpt = None
          )
        )
      }

      // Запуск основной логики подхвата карточек плитки.
      super._findAdsReady(mfa)

      // Управление размывкой выдачи после ресайза.
      val sd2 = _stateData
      if (sd2.isAnySidePanelOpened && _isNeedBlur(sd2))
        _blurGrid()
      else
        _unBlurGrid()
    }

    // При наличии нового размера контейнера сетки обновить его в состоянии FSM.
    override def _handleNewGridContSz(gContSzOpt: Option[ICwCm]): Unit = {
      super._handleNewGridContSz(gContSzOpt)

      // Сохранить новые данные контейнера в состояние.
      val sd0 = _stateData
      _stateData = sd0.copy(
        grid    = sd0.grid.copy(
          state = sd0.grid.state.copy(
            contSz = gContSzOpt
          )
        )
      )
    }

    /** Состояние, когда все карточки загружены. */
    override final def _adsLoadedState: FsmState = null

    /** Состояние, когда запрос карточек не удался. */
    override final def _findAdsFailedState: FsmState = null

    /** Состояние подгрузки ещё карточек. */
    protected def _loadMoreState: FsmState


    /**
      * Укороченная реакция на popstate во время плитки, возможно с открытой панелью боковой.
      *
      * @param sdNext Распарсенные данные нового состояния из URL.
      */
    override def _handleStateSwitch(sdNext: MScSd): Unit = {
      if (sdNext.focused.nonEmpty) {
        // Перещёлкиваем на focused state независимо от конфигурации других вещей.
        become( _startFocusOnAdState )
      } else {
        super._handleStateSwitch( sdNext )
      }
    }
  }

}



