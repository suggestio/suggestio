package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.ScFsmStub
import io.suggest.sc.sjs.m.mgrid.{VScroll, GridBlockClick}
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sc.sjs.v.res.CommonRes
import io.suggest.sc.sjs.vm.grid.GLoader
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.15 16:16
 * Description: Конечный автомат для поддержки выдачи.
 */
trait GridFsm extends ScFsmStub {

  /** Состояние ожидания результатов инициализация index'а узла. Паралельно идут две фоновые операции:
    * получение карточек и отображение welcome-экрана. */
  protected trait AdsWaitStateBaseT extends FsmState {

    /** Фьючерс с искомыми карточками. */
    protected def findAdsFut: Future[MFindAds]

    /** Необходимо подписаться на события futures'ов. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Повесить ожидание события.
      findAdsFut onComplete {
        case Success(resp) => _sendEvent(resp)
        case failure       => _sendEvent(failure)
      }
    }

    override def receiverPart: PartialFunction[Any, Unit] = {
      case mfa: MFindAds =>
        _findAdsReady(mfa)
      case Failure(ex) =>
        _findAdsFailed(ex)
    }

    /** Что делать при ошибке получения карточек. */
    protected def _findAdsFailed(ex: Throwable): Unit

    /** Что делать при получении ответа сервера с карточками. */
    protected def _findAdsReady(mfa: MFindAds): Unit
  }

  /** Закинуть в выдачу полученные карточки. */
  protected trait AppendAdsToGridStateT extends AdsWaitStateBaseT {

    protected def isAdd: Boolean
    protected def addWithAnim: Boolean

    protected def noMoreMadsState: FsmState

    abstract override protected def _findAdsReady(mfa: MFindAds): Unit = {
      super._findAdsReady(mfa)
      // Далее заинлайнен перепиленный вызов GridCtl.newAdsReceived(mfa, isAdd = isAdd, withAnim = addWithAnim)
      val sd0  = _stateData
      val mgs0 = sd0.gridState
      val gloaderOpt = GLoader.find()

      if (mfa.mads.isEmpty) {
        for (l <- gloaderOpt) {
          l.hide()
        }

        // Скрыть loader-индикатор, он больше не нужен ведь.
        val sd1 = sd0.copy(
          gridState = mgs0.copy(
            fullyLoaded = true
          )
        )
        become(noMoreMadsState, sd1)

      } else {

        // Если получены новые параметры сетки, то выставить их в состояние сетки
        val gridParams0 = sd0.gridParams
        val gridParams2 = mfa.params
          .map(gridParams0.withChangesFrom)
          .getOrElse(gridParams0)

        // Закачать в выдачу новый css.
        for (css <- mfa.css) {
          CommonRes.appendCss(css)
        }

        // Посчитать и сохранить кол-во загруженных карточек плитки.
        val madsSize = mfa.mads.size

        // Показать либо скрыть индикатор подгрузки выдачи.
        val mgs1 = gloaderOpt.fold(mgs0) { gloader =>
          if (madsSize < mgs0.adsPerLoad) {
            gloader.hide()
            mgs0.copy(fullyLoaded = true)
          } else {
            gloader.show()
            mgs0
          }
        }

        ??? // TODO Дорефакторить под FSM код, которые закомменчен ниже:

        // Вызываем пересчет ширин боковых панелей в выдаче без перестройки исходной плитки.
        /*resetGridOffsets()

        for (containerDiv <- containerDivOpt) {
          // Залить все карточки в DOM, создав суб-контейнер frag.
          val frag = GridView.appendNewMads(containerDiv, mads)

          // Далее логика cbca_grid.init(). Допилить сетку под новые карточки:
          resetContainerSz(containerDiv, loaderDivOpt)

          // Проанализировать залитые в DOM блоки, сохранить метаданные в модель блоков.
          val newBlocks = analyzeNewBlocks(frag)
          mgs.appendNewBlocksMut(newBlocks, madsSize)

          // Расположить все новые карточки на экране.
          build(isAdd, mgs, newBlocks, withAnim)

          // Вычислить максимальную высоту в колонках и расширить контейнер карточек до этой высоты.
          updateContainerHeight(containerDiv)

          // Повесить события на блоки
          GridView.initNewBlocks(newBlocks)
        }*/
      }

    }
  }


  /** Трейт для сборки состояний, в которых доступна сетка карточек и клики по ней. */
  protected trait TiledStateT extends FsmState {

    /** Обработка кликов по карточкам в сетке. */
    protected def handleGridBlockClick(gbc: GridBlockClick): Unit = {
      // TODO Нужно запустить focused-запрос и перключиться на focused-выдачу.
      ???
    }

    /** Реакция на вертикальный скроллинг. */
    protected def handleVScroll(vs: VScroll): Unit = {
      // TODO Нужно запустить подгрузку ещё карточек, если возможно.
      ???
    }

    /** Обработка событий сетки карточек. */
    override def receiverPart: Receive = {
      // Клик по одной из карточек в сетке оных.
      case gbc: GridBlockClick =>
        handleGridBlockClick(gbc)

      // Вертикальный скроллинг в плитке.
      case vs: VScroll =>
        handleVScroll(vs)
    }

  }

}
