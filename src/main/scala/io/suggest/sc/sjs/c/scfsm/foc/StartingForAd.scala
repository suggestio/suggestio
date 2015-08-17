package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.c.scfsm.{FindNearAdIds, FindAdsFsmUtil, ScFsmStub}
import io.suggest.sc.sjs.m.mfoc.{MFocSd, SlideDone}
import io.suggest.sc.sjs.m.msrv.foc.find.{MFocAds, MFocAdSearchEmpty}
import io.suggest.sc.sjs.vm.res.FocusedRes
import io.suggest.sc.sjs.vm.foc.fad.FAdRoot
import io.suggest.sc.sjs.vm.foc.{FRoot, FCarousel}
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
      for (screen <- sd0.screen;  fState0 <- sd0.focused;  car <- FCarousel.find()) {
        val gblockOpt   = fState0.gblock
        val currIndex   = _getCurrIndex(fState0)
        // Собрать и запустить fads-реквест на основе запроса к карточкам плитки.
        // Флаг: запрашивать ли карточку, предшествующую запрошенной. Да, если запрошена ненулевая карточка.
        val withPrevAd  = currIndex > 0
        val currMadIdOpt = fState0.currAdId
          .orElse { gblockOpt.flatMap(_.madId) }
        val firstMadIds = _nearAdIdsIter( gblockOpt orElse currMadIdOpt.flatMap(GBlock.find) )
            .toSeq
        // TODO Надо запрашивать просто по id'шникам необходимые карточки, типа multiget.
        val args = new MFocAdSearchEmpty with FindAdsArgsT {
          override def _sd        = sd0
          override def firstAdIds = firstMadIds
          // Выставляем под нужды focused-выдачи значения limit/offset.
          override def offset     = Some( if (withPrevAd) currIndex - 1 else currIndex )
          override def limit      = Some( if (withPrevAd) 3 else 2 )
          //override def levelId = Some(ShowLevels.ID_PRODUCER)  // TODO Тут должно быть что-то, не?.
        }
        val fadsFut = MFocAds.find(args)

        // Готовим карусель к работе.
        if (!car.isEmpty)
          car.clean()
        // Ширина ячейки в карусели эквивалентна пиксельной ширине экрана.
        val cellWidthPx = screen.width
        // Начальная ширина карусели задаётся исходя из текущих ячеек. +1 -- Скорее всего будет как минимум одна карточка после текущей.
        car.setWidthPx( cellWidthPx * (currIndex + 1) )
        // Начальный сдвиг карусели выставляем без анимации. -1 т.к. первая карточка должна выезжать из-за экрана.
        val carLeftPx = -cellWidthPx * (currIndex - 1)
        car.disableTransition()
        car.animateToX( carLeftPx )
        // Пустая карусель, но к работе вроде готова.
        car.enableTransition()
        // TODO Карусель пустая, а will-change выставляется. Не будет ли негативного эффекта от такой странной оптимизации?
        car.willAnimate()

        // Подготовить контейнер для стилей.
        FocusedRes.ensureCreated()

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
      // Заливаем стили в дерево, если они есть.
      for (styles <- mfa.styles; res <- FocusedRes.find()) {
        res.appendCss(styles)
      }
      val fads = mfa.focusedAdsIter.toSeq
      // Залить в карусель полученные карточки.
      val sd0 = _stateData
      for (fState <- sd0.focused;  screen <- sd0.screen;  fRoot <- FRoot.find();  car <- fRoot.carousel) {
        val currIndex   = _getCurrIndex(fState)
        // Сначала обрабатываем запрошенную карточку:
        val cellWidth = screen.width
        // Индекс запрошенной карточки в массиве fads: она или первая крайняя, или вторая при наличии предыдущей.
        val firstAdFadsIndex = if (currIndex > 0) 1 else 0
        val firstAd = fads(firstAdFadsIndex)
        val cell1 = FAdRoot( firstAd.bodyHtml )
        cell1.setWidthPx( cellWidth )
        // Повесить запрошенную карточку на шаг правее нужного индекса, чтобы можно было прослайдить на неё.
        cell1.setLeftPx( currIndex * cellWidth )

        // Прилинковываем запрошенную карточку справа и запускаем анимацию.
        car.pushCellRight(cell1)
        fRoot.show()
        car.animateToX( -currIndex * cellWidth )

        // TODO После анимации надо прилинковать к карусели оставшиеся карточки: prev и next, если они есть.
        val afterAnimateF = { () =>
          _sendEvent(SlideDone)
        }
        dom.setTimeout(afterAnimateF, SLIDE_ANIMATE_MS + 50)

        // TODO Обновить состояние FSM: сохранить туда оставшиеся карточки для прицепляния.
        ???
      }
    }

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

}
