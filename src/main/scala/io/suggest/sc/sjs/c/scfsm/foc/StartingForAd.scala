package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.c.scfsm.{FindNearAdIds, FindAdsFsmUtil, ScFsmStub}
import io.suggest.sc.sjs.m.mfoc.{FAdShown, MFocSd, FocRootAppeared}
import io.suggest.sc.sjs.m.msrv.foc.find.{MFocAd, MFocAds, MFocAdSearchEmpty}
import io.suggest.sc.sjs.vm.res.FocusedRes
import io.suggest.sc.sjs.vm.foc.fad.FAdRoot
import io.suggest.sc.sjs.vm.foc.{FCarousel, FControls, FRoot}
import io.suggest.sc.sjs.vm.grid.GBlock
import org.scalajs.dom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}
import io.suggest.sc.ScConstants.Focused.SLIDE_ANIMATE_MS

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 11:19
 * Description: FSM-аддон для добавления поддержки начальной focused-загрузки карточек.
 * Используется для нулевого шага в работе focused-выдачи: инициализация focused-выдачи и первых карточек.
 */

// TODO Тут stub только. Логика не написана.
// Нужно реализовать состояния в подпакете scfsm, а этот файл удалить/переместить.

trait StartingForAd extends ScFsmStub with FindAdsFsmUtil {

  /** Трейт для состояния, когда focused-выдача отсутствует, скрыта вообще и ожидает активации.
    * При появлении top-level ScFsm это событие исчезнет, и будет обрабатываться где-то в вышестоящем обработчике. */
  protected trait StartingForAdStateT extends FsmState with FindNearAdIds {
    
    protected def _getCurrIndex(fState: MFocSd): Int = {
      fState.gblock
        .map { _.index }
        .orElse { fState.currIndex }
        .getOrElse(0)   // TODO Когда придёт время для восстановления состояния FSM из URL, надо будет сделать тут логику по-лучше.
    }

    override def afterBecome(): Unit = {
      super.afterBecome()
      val sd0 = _stateData
      for (screen <- sd0.screen;  fState0 <- sd0.focused;  fRoot <- FRoot.find();  car <- fRoot.carousel) {
        val gblockOpt   = fState0.gblock
        val currIndex   = _getCurrIndex(fState0)
        // Собрать и запустить fads-реквест на основе запроса к карточкам плитки.
        // Флаг: запрашивать ли карточку, предшествующую запрошенной. Да, если запрошена ненулевая карточка.
        val withPrevAd  = currIndex > 0
        val currMadIdOpt = fState0.currAdId
          .orElse { gblockOpt.flatMap(_.madId) }
        val firstMadIds = _nearAdIdsIter( gblockOpt orElse currMadIdOpt.flatMap(GBlock.find) )
            .toSeq
        // Поиск идёт с упором на multiGet по id карточек.
        val args = new MFocAdSearchEmpty with FindAdsArgsT {
          override def _sd        = sd0
          override def firstAdIds = firstMadIds
          // Выставляем под нужды focused-выдачи значения limit/offset.
          override def offset     = Some( if (withPrevAd) currIndex - 1 else currIndex )
          override def limit      = Some( if (withPrevAd) 3 else 2 )
          // level не выставляем, т.к. focused-выдача дублирует grid-выдачу в отображенном виде.
        }
        val fadsFut = MFocAds.find(args)

        // Скрыть за экран корневой focused-контейнер, подготовиться к появлению на экране.
        fRoot.disableTransition()
        fRoot.initialDisappear()

        // Готовим карусель к работе.
        if (!car.isEmpty)
          car.clear()
        // Ширина ячейки в карусели эквивалентна пиксельной ширине экрана.
        val cellWidthPx = screen.width
        // Начальная ширина карусели задаётся исходя из текущих ячеек. +1 -- Скорее всего будет как минимум одна карточка после текущей.
        car.setWidthPx( cellWidthPx * (currIndex + 1) )
        // Начальный сдвиг карусели выставляем без анимации. Весь focused будет выезжать из-за экрана.
        val carLeftPx = -cellWidthPx * currIndex
        car.disableTransition()
        car.animateToX( carLeftPx )

        // Подготовить контейнер для стилей.
        FocusedRes.ensureCreated()
        // Подготовить контейнер для заголовка и прочего FControls.
        for (fControls <- FControls.find()) {
          fControls.clear()
        }

        // повесить листенер для ожидания ответа сервера.
        fadsFut onComplete { case res =>
          val event = res match {
            case Success(mfa) => mfa
            case failure      => failure
          }
          _sendEvent(event)
        }
      }
    }

    /** Реакция на полученный ответ сервера. */
    protected def _focAdsReceived(mfa: MFocAds): Unit = {
      // Заливаем все полученные стили в DOM.
      for (styles <- mfa.styles; res <- FocusedRes.find()) {
        res.appendCss(styles)
      }
      val fads = mfa.focusedAdsIter.toSeq
      // Залить в карусель полученные карточки.
      val sd0 = _stateData
      for {
        fState    <- sd0.focused
        screen    <- sd0.screen
        fRoot     <- FRoot.find()
        car       <- fRoot.carousel
      } {
        val currIndex   = _getCurrIndex(fState)
        // Раскидать полученные карточки по аккамуляторам и карусели. Для ускорения закидываем в карусель только необходимую карточку.
        val (prevs2, firstAdOpt, nexts2) = {
          fads.foldLeft((fState.prevs, Option.empty[MFocAd], fState.nexts)) {
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
        fadRoot.initLayout( screen )
        // Повесить запрошенную карточку на месте текущего индекса.
        fadRoot.setLeftPx( currIndex * cellWidth )

        // Прилинковываем запрошенную карточку справа и запускаем анимацию.
        car.pushCellRight(fadRoot)
        car.animateToX( -currIndex * cellWidth )

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
              carState    = List( FAdShown(fadRoot, Right(fControls), firstAd) ),
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

    protected def _focAdsRequestFailed(ex: Throwable): Unit = {
      ???
    }


    private def _receiverPart: Receive = {
      case mfa: MFocAds =>
        _focAdsReceived(mfa)
      case Failure(ex) =>
        _focAdsRequestFailed(ex)
    }

    override def receiverPart: Receive = {
      _receiverPart orElse super.receiverPart
    }

  }



  /** Трейт состояния во время анимированного появления на экране focused-выдачи.
    * Таким образом, это состояние живёт только около 200мс. */
  protected trait OnAppearStateT extends FsmState {

    override def afterBecome(): Unit = {
      super.afterBecome()
      dom.setTimeout(
        {() =>
          for (fRoot <- FRoot.find()) {
            fRoot.appearTransition()
          }
          dom.setTimeout(
            {() => _sendEvent(FocRootAppeared) },
            SLIDE_ANIMATE_MS
          )
        },
        20
      )
    }

    private def _receiverPart: Receive = {
      case FocRootAppeared =>
        _appeared()
    }

    override def receiverPart: Receive = _receiverPart orElse super.receiverPart

    /** Логика реакции на окончание анимации. */
    protected def _appeared(): Unit = {
      for (car <- FCarousel.find()) {
        car.enableTransition()
        car.willAnimate()
      }
      become(_focReadyState)
    }

    /** Следующее состояние, т.е. когда анимация завершена уже. */
    protected def _focReadyState: FsmState
  }

}
