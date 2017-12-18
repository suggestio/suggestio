package io.suggest.sc.sjs.c.scfsm.grid

import io.suggest.msg.ErrorMsgs
import io.suggest.sc.sjs.c.scfsm.grid.build.GridBuilder
import io.suggest.sc.sjs.c.scfsm.FindAdsArgsT
import io.suggest.sc.sjs.m.msrv.tile.{MFindAdsTile, MScRespAdsTile}
import io.suggest.sc.sjs.vm.grid.GContent
import io.suggest.sc.sjs.vm.res.CommonRes
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future
import scala.util.Failure

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.15 16:16
 * Description: Конечный автомат для поддержки загрузки карточек в плитку выдачи.
 */
trait Append extends FindAdsArgsT {

  /** Запустить поиск карточек для выдачи, прислать результат Future в FSM.
    *
    * @return Фьючерс, обычно бесполезен, т.к. результат прилетает назад в FSM как сообщение.
    */
  protected[this] def _startFindGridAds(sd: SD = _stateData): Future[MScRespAdsTile] = {
    val args = MFindAdsArgsLimOff(sd)
    val fut = MFindAdsTile.findAds(args)
    _sendFutResBack(fut)
    fut
  }


  /** Состояние ожидания результатов инициализация index'а узла. Паралельно идут две фоновые операции:
    * получение карточек и отображение welcome-экрана. */
  trait GridAdsWaitStateBaseT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart.orElse {
      case mfa: MScRespAdsTile =>
        _findAdsReady(mfa)
      case Failure(ex) =>
        _findAdsFailed(ex)
    }

    /** Что делать при ошибке получения карточек. */
    protected def _findAdsFailed(ex: Throwable): Unit

    /** Что делать при получении ответа сервера с карточками. */
    protected def _findAdsReady(mfa: MScRespAdsTile): Unit
  }


  /** Закинуть в выдачу полученные карточки. */
  trait GridAdsWaitLoadStateT extends GridAdsWaitStateBaseT {

    /** FSM-реакция на получение положительного ответа от сервера по поводу карточек сетки.
      *
      * @param mfa инстанс ответа MFindAds.
      */
    override def _findAdsReady(mfa: MScRespAdsTile): Unit = {
      val gcontent = GContent.find().get
      // Далее заинлайнен перепиленный вызов GridCtl.newAdsReceived(mfa, isAdd = isAdd, withAnim)
      val sd0  = _stateData
      val mgs0 = sd0.grid.state

      // Вызываем пересчет ширин боковых панелей в выдаче без перестройки исходной плитки.
      val screen = sd0.common.screen

      // Если получены новые параметры сетки, то выставить их в состояние сетки
      val gridParams0 = sd0.grid.params
      val gridParams2 = mfa.params
        .flatMap(gridParams0.withChangesFrom)
        .getOrElse(gridParams0)

      val grid1 = sd0.grid.copy(
        params = gridParams2
      )
      val csz = grid1.getGridContainerSz(screen)
      gcontent.setContainerSz(csz)

      val sdFinal = if (mfa.mads.isEmpty) {
        for (l <- gcontent.loader) {
          l.hide()
        }

        // Скрыть loader-индикатор, он больше не нужен ведь.
        sd0.copy(
          grid = grid1.copy(
            state = mgs0.copy(
              fullyLoaded = true
            )
          )
        )

      } else {

        // Закачать в выдачу новый css.
        for (css <- mfa.css; res <- CommonRes.find()) {
          res.appendCss(css)
        }

        // Посчитать и сохранить кол-во загруженных карточек плитки.
        val madsSize = mfa.mads.size

        // Показать либо скрыть индикатор подгрузки выдачи.
        val mgs1 = gcontent.loader.fold(mgs0) { gloader =>
          if (madsSize < mgs0.adsPerLoad) {
            gloader.hide()
            mgs0.copy(fullyLoaded = true)
          } else {
            gloader.show()
            mgs0
          }
        }


        // Окучиваем контейнер карточек новыми карточками.
        val gcontainer = gcontent.container.get
        // Залить все карточки в DOM, создав суб-контейнер frag.
        val frag = gcontainer.appendNewMads(mfa.mads)

        // Далее логика cbca_grid.init(). Допилить сетку под новые карточки: визуально и на уровне состояния сетки.
        val _mgs2 = mgs1.withContParams(csz)
          .withNewBlocks( frag.blocks )

        val grid2 = grid1.copy(
          state  = _mgs2
        )
        // Расположить все новые карточки на экране.
        val gbuilder = GridBuilder(grid2, sd0.common.browser, frag.blocks)

        val gbState2 = gbuilder.execute()
        val grid3 = grid2.copy(
          builderStateOpt = Some(gbState2)
        )

        // Расширить контейнер карточек до этой высоты.
        gcontainer.resetHeightUsing(grid3)

        // Инициализировать новые блоки вешаньем обработчиков событий.
        frag.blocks.foreach { gblock =>
          gblock.initLayout()
        }

        // Вернуть результат, перещелкнуть автомат на следующее состояние: ожидание сигналов от юзера.
        sd0.copy(
          grid = grid3
        )
      }

      // Переключиться на следующее состояние.
      become(_adsLoadedState, sdFinal)
    }


    protected def _findAdsFailed(ex: Throwable): Unit = {
      LOG.error(ErrorMsgs.FIND_ADS_REQ_FAILED, ex)
      become(_findAdsFailedState)
    }

    /** Состояние, когда все карточки загружены. */
    protected def _adsLoadedState: FsmState

    /** Состояние, когда запрос карточек не удался. */
    protected def _findAdsFailedState: FsmState

  }

}
