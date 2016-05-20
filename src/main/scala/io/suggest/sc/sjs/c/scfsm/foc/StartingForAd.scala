package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.c.scfsm.node.Index
import io.suggest.sc.sjs.c.scfsm.{FindAdsUtil, FindNearAdIds}
import io.suggest.sc.sjs.m.mfoc.{FAdShown, FocRootAppeared, MFocSd}
import io.suggest.sc.sjs.m.msrv.foc.find.{MFocAd, MFocAdSearchEmpty, MFocAds}
import io.suggest.sc.sjs.m.msrv.index.MNodeIndex
import io.suggest.sc.sjs.vm.layout.FsLoader
import io.suggest.sc.sjs.vm.res.FocusedRes
import io.suggest.sc.sjs.vm.foc.fad.FAdRoot
import io.suggest.sc.sjs.vm.foc.{FCarousel, FControls, FRoot}
import io.suggest.sc.sjs.vm.grid.GBlock
import io.suggest.sjs.common.msg.ErrorMsgs

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Failure
import io.suggest.sc.ScConstants.Focused.SLIDE_ANIMATE_MS
import io.suggest.sjs.common.controller.DomQuick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.06.15 11:19
  * Description: FSM-аддон для добавления поддержки начальной focused-загрузки карточек.
  * Используется для нулевого шага в работе focused-выдачи: инициализация focused-выдачи и первых карточек.
  *
  * 2016.may.20: Сюда криво вписан режим прямой фокусировки по id карточки.
  * появились if'ы в довольно неожиданных местах. По сути в одном коде живут два режима фокусировки:
  * - v2, последовательный относительно плитки, при кликах в плитке.
  * - Около-последовательный с ручной фокусировкой на первой карточке по её id. Он похож на v1-фокусировку.
  * Аналогичные костыли в PreLoading.
  */

// TODO Тут stub только. Логика не написана.
// Нужно реализовать состояния в подпакете scfsm, а этот файл удалить/переместить.

trait StartingForAd extends MouseMoving with FindAdsUtil with Index {

  /**
    * Трейт для состояния, когда focused-выдача отсутствует, скрыта вообще и ожидает активации.
    * При появлении top-level ScFsm это событие исчезнет, и будет обрабатываться где-то в вышестоящем обработчике.
    */
  protected trait StartingForAdStateT extends FsmEmptyReceiverState with FindNearAdIds with ProcessIndexReceivedUtil {

    private def _getGblock(fState: MFocSd): Option[GBlock] = {
      if (fState.forceFirstAdIds.isEmpty) {
        fState.currAdId.flatMap(GBlock.find)
      } else {
        // Активно ручное управление фокусировкой. Нет смысла искать блоки в плитке. См. зелёный коммент наверху.
        None
      }
    }

    /**
      * Попытаться определить индекс интересующей focused-карточки.
      * index может быть неизвестен, это бывает при запросе карточки по id с заведомо неизвесным индексом.
      */
    protected def _getCurrIndex(fState: MFocSd, gblockOpt: Option[GBlock]): Option[Int] = {
      gblockOpt
        .map { _.index }
        .orElse { fState.currIndex }
    }

    override def afterBecome(): Unit = {
      super.afterBecome()

      // Необходимо запустить focused ad реквест к серверу.
      val sd0 = _stateData
      for {
        screen  <- sd0.common.screen
        fState0 <- sd0.focused
        fRoot   <- FRoot.find()
        car     <- fRoot.carousel
      } {

        val gblockOpt     = _getGblock(fState0)
        val currIndexOpt  = _getCurrIndex(fState0, gblockOpt)

        // Собрать и запустить fads-реквест на основе запроса к карточкам плитки.
        // Флаг: запрашивать ли карточку, предшествующую запрошенной. Да, если запрошена ненулевая карточка.
        val withPrevAd  = currIndexOpt.exists(_ > 0)

        // Поиск идёт с упором на multiGet по id карточек.
        val args = new MFocAdSearchEmpty with FindAdsArgsT {
          override def _sd        = sd0
          override def firstAdIds = {
            val ffais = fState0.forceFirstAdIds
            if (ffais.isEmpty) {
              currIndexOpt.fold(Nil: Seq[String]) { _ =>
                _nearAdIdsIter(gblockOpt).toSeq
              }
            } else {
              ffais
            }
          }
          // Выставляем под нужды focused-выдачи значения limit/offset.
          override def offset     = for (currIndex <- currIndexOpt) yield {
            if (withPrevAd) currIndex - 1 else currIndex
          }
          override def limit      = Some( if (withPrevAd) 3 else 2 )
          // Для первой открываемой карточки допускается переход на index-выдачу узла вместо открытия focused-выдачи.
          override def openIndexAdId = fState0.currAdId
        }
        val fadsFut = MFocAds.findOrIndex(args)

        // Запрос запущен, пора бы отобразить loader
        for (fsl <- FsLoader.find()) {
          fsl.show()
        }

        // Скрыть за экран корневой focused-контейнер, подготовиться к появлению на экране.
        fRoot.disableTransition()
        fRoot.initialDisappear()

        // Готовим карусель к работе.
        if (!car.isEmpty)
          car.clear()
        // Ширина ячейки в карусели эквивалентна пиксельной ширине экрана.
        // Начальная ширина карусели задаётся исходя из текущих ячеек. +1 -- Скорее всего будет как минимум одна карточка после текущей.
        car.setCellWidth(currIndexOpt.getOrElse(1), screen)
        // Начальный сдвиг карусели выставляем без анимации. Весь focused будет выезжать из-за экрана.
        car.disableTransition()
        car.animateToCell(currIndexOpt.getOrElse(0), screen, sd0.common.browser)

        // Подготовить контейнер для стилей.
        FocusedRes.ensureCreated()
        // Подготовить контейнер для заголовка и прочего FControls.
        for (fControls <- FControls.find()) {
          fControls.clear()
        }

        // повесить листенер для ожидания ответа сервера.
        _sendFutResBack(fadsFut)
      }
    }

    /** Реакция на полученный ответ сервера. */
    protected def _focAdsReceived(mfa: MFocAds): Unit = {
      // Заливаем все полученные стили в DOM.
      for (styles <- mfa.styles; res <- FocusedRes.find()) {
        res.appendCss(styles)
      }

      // Залить в карусель полученные карточки.
      val sd0 = _stateData
      for {
        fState    <- sd0.focused
        screen    <- sd0.common.screen
        fRoot     <- FRoot.find()
        car       <- fRoot.carousel
      } {
        val currIndexOpt  = _getCurrIndex(fState, _getGblock(fState))

        // Раскидать полученные карточки по аккамуляторам и карусели. Для ускорения закидываем в карусель только необходимую карточку.
        val (prevs2, firstAdOpt, nexts2) = currIndexOpt.fold {
          // Нет индекса, значит идёт доступ напрямую по mad id. Закинуть всё в nexts кроме первой карточки.
          val fads = mfa.focusedAdsIter.toStream
          val nexts1 = fState.nexts
          val nexts2 = if (fads.nonEmpty) nexts1.enqueue(fads.tail) else nexts1
          (fState.prevs, fads.headOption, nexts2)

        } { currIndex =>
          // Есть индекс текущей карточки, значит идёт номальное функционирование. Дробим выдачу по числовым индексам.
          mfa.focusedAdsIter.foldLeft((fState.prevs, Option.empty[MFocAd], fState.nexts)) {
            case ((_prevs, _firstOpt, _nexts), _e) =>
              val i = _e.index
              if (i < currIndex)
                (_e +: _prevs, _firstOpt, _nexts)
              else if (i == currIndex)
                (_prevs, Some(_e), _nexts)
              else
                (_prevs, _firstOpt, _nexts.enqueue(_e))
          }
        }

        val cellWidth = screen.width

        // Сначала обрабатываем запрошенную карточку:
        // Индекс запрошенной карточки в массиве fads: она или первая крайняя, или вторая при наличии предыдущей.
        val firstAd = firstAdOpt.get      // TODO Отработать сценарий отсутствия запрошенной карточки.
        val fadRoot = FAdRoot( firstAd.bodyHtml )
        fadRoot.initLayout( screen, sd0.common.browser )
        // Повесить запрошенную карточку на месте текущего индекса.
        val currIndex = currIndexOpt.getOrElse(0)
        fadRoot.setLeftPx( currIndex * cellWidth )

        // Прилинковываем запрошенную карточку справа и запускаем анимацию.
        car.pushCellRight(fadRoot)
        car.animateToCell(currIndex, screen, sd0.common.browser)

        // Начата обработка тяжелого тела focused-карточки. Залить текущий заголовок focused-выдачи.
        for (fControls <- fRoot.controls) {
          fControls.setContent( firstAd.controlsHtml )

          fRoot.willAnimate()
          fRoot.show()
          fRoot.enableTransition()

          val sd1: SD = sd0.copy(
            focused = Some( fState.copy(
              totalCount  = Some(mfa.totalCount),
              loadedCount = fState.loadedCount + mfa.fadsCount,
              nexts       = nexts2,
              carState    = List( FAdShown(fadRoot, firstAd) ),
              prevs       = prevs2,
              currIndex   = Some(currIndex)
            ))
          )
          become(_focOnAppearState, sd1)
        }

      }
    }

    /** Состояние текущей анимации появления на экране. */
    protected def _focOnAppearState: FsmState

    /** Состояние возврата на сетку. Нештатная работа этого состояния. */
    protected def _backToGridState: FsmState

    protected def _focAdsRequestFailed(ex: Throwable): Unit = {
      // Если не удалось сделать начальный реквест, то надо сбросить состояние и вернутся в состояние плитки.
      error(ErrorMsgs.FOC_FIRST_REQ_FAILED, ex)
      _backToGrid()
    }

    protected def _backToGrid(): Unit = {
      val sd1 = _stateData.copy(
        focused = None
      )
      for (fsl <- FsLoader.find()) {
        fsl.hide()
      }
      become(_backToGridState, sd1)
    }

    override def receiverPart: Receive = super.receiverPart.orElse {
      case Right(mfa: MFocAds) =>
        _focAdsReceived(mfa)
      case Left(mni: MNodeIndex) =>
        _stateData = _stateData.withNodeSwitch( mni.adnIdOpt )
        _nodeIndexReceived(mni)
      case Failure(ex) =>
        _focAdsRequestFailed(ex)
    }

    /** При ошибке надо возвращаться назад в сетку. */
    override def processFailure(ex: Throwable): Unit = {
      super.processFailure(ex)
      _backToGrid()
    }

  }



  /** Трейт состояния во время анимированного появления на экране focused-выдачи.
    * Таким образом, это состояние живёт только около 200мс. */
  protected trait OnAppearStateT extends FocMouseMovingStateT {

    override def afterBecome(): Unit = {
      super.afterBecome()
      DomQuick.setTimeout(20) { () =>
        for (fRoot <- FRoot.find()) {
          fRoot.appearTransition()
        }
        DomQuick.setTimeout(SLIDE_ANIMATE_MS) { () =>
          _sendEvent(FocRootAppeared)
        }
      }
    }

    override def receiverPart: Receive = {
      case FocRootAppeared =>
        _appeared()
    }

    /** Логика реакции на окончание анимации. */
    protected def _appeared(): Unit = {
      for (car <- FCarousel.find()) {
        car.enableTransition()
        car.willAnimate()
      }
      for (fsl <- FsLoader.find()) {
        fsl.hide()
      }
      become(_focReadyState)
    }

    /** Следующее состояние, т.е. когда анимация завершена уже. */
    protected def _focReadyState: FsmState
  }

}
