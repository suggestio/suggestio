package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.c.GridCtl
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sc.sjs.util.grid.builder.V1Builder
import io.suggest.sc.sjs.v.res.CommonRes
import io.suggest.sc.sjs.vm.grid.{GBlock, GContent}
import io.suggest.sjs.common.model.browser.IBrowser
import io.suggest.sjs.common.util.SjsLogger

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.15 16:16
 * Description: Конечный автомат для поддержки загрузки карточек в плитку выдачи.
 */
trait GridAppend extends ScFsmStub {

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

    protected def withAnim: Boolean

    protected def adsLoadedState: FsmState
    
    override protected def _findAdsReady(mfa: MFindAds): Unit = {
      val gcontent = GContent.find().get
      // Далее заинлайнен перепиленный вызов GridCtl.newAdsReceived(mfa, isAdd = isAdd, withAnim)
      val sd0  = _stateData
      val mgs0 = sd0.grid.state

      val sdFinal = if (mfa.mads.isEmpty) {
        for (l <- gcontent.loader) {
          l.hide()
        }

        // Скрыть loader-индикатор, он больше не нужен ведь.
        sd0.copy(
          grid = sd0.grid.copy(
            state = mgs0.copy(
              fullyLoaded = true
            )
          )
        )

      } else {

        // Если получены новые параметры сетки, то выставить их в состояние сетки
        val gridParams0 = sd0.grid.params
        val gridParams2 = mfa.params
          .flatMap(gridParams0.withChangesFrom)
          .getOrElse(gridParams0)

        // Закачать в выдачу новый css.
        for (css <- mfa.css) {
          CommonRes.appendCss(css)
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

        // Вызываем пересчет ширин боковых панелей в выдаче без перестройки исходной плитки.
        val screen = sd0.screen.get
        GridCtl.resetGridOffsets(mgs1, screen)    // TODO Нужно без GridCtl здесь обойтись.


        // Окучиваем контейнер карточек новыми карточками.
        val gcontainer = gcontent.container.get
        // Залить все карточки в DOM, создав суб-контейнер frag.
        val frag = gcontainer.appendNewMads(mfa.mads)

        // Далее логика cbca_grid.init(). Допилить сетку под новые карточки: визуально и на уровне состояния сетки.
        val csz = sd0.grid.getGridContainerSz(screen)
        gcontent.setContainerSz(csz)
        val _mgs2 = mgs1.withContParams(csz)

        // Проанализировать залитые в DOM блоки, сохранить метаданные в модель блоков.
        _mgs2.withNewBlocks( frag.blocks )

        val grid2 = sd0.grid.copy(
          params = gridParams2,
          state  = _mgs2
        )
        // Расположить все новые карточки на экране.
        val gbuilder = new GridBuilder2 {
          override def browser = sd0.browser
          override def grid = grid2
          override def _addedBlocks = frag.blocks
        }

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
      become(adsLoadedState, sdFinal)
    }

    /** Частичная реализация grid builder под нужды FSM-MVM-архитектуры. */
    protected trait GridBuilder2 extends V1Builder with SjsLogger {
      override type BI = GBlock
      def browser: IBrowser

      // Собираем функцию перемещения блока. При отключенной анимации не будет лишней сборки ненужного
      // списка css-префиксов и проверки значения withAnim.
      val _moveBlockF: (Int, Int, BI) => Unit = {
        if (withAnim) {
          // Включена анимация. Собрать необходимые css-префиксы. {} нужно для защиты от склеивания с последующей строкой.
          val animCssPrefixes = { browser.CssPrefixing.transforms3d }
          {(leftPx: Int, topPx: Int, b: BI) =>
            b.moveBlockAnimated(leftPx, topPx, animCssPrefixes)
          }

        } else {
          // Анимация отключена.
          {(leftPx: Int, topPx: Int, b: BI) =>
            b.moveBlock(leftPx, topPx)
          }
        }
      }

      override protected def moveBlock(leftPx: Int, topPx: Int, b: BI): Unit = {
        _moveBlockF(leftPx, topPx, b)
      }
    }

  }


  /** Состояние добавки карточек одновременно с отображением welcome. */
  protected trait AppendAdsToGridDuringWelcomeStateT extends AppendAdsToGridStateT {
    /** Future, который исполняется когда начинается скрытие welcome. */
    protected def wcHideFut: Future[_]

    /** Анимация нужна только если welcome-карточка уже теряет непрозрачность или же скрыта. */
    override protected def withAnim = wcHideFut.isCompleted
  }

}
