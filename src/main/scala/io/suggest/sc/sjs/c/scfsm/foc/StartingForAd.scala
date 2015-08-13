package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.c.scfsm.{FindAdsFsmUtil, ScFsmStub}
import io.suggest.sc.sjs.m.mfoc.{SlideDone, FadsReceived}
import io.suggest.sc.sjs.m.msrv.foc.find.{MFocAds, MFocAdSearchEmpty}
import io.suggest.sc.sjs.vm.foc.{FCarCell, FRoot, FCarousel, FocAd}
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
  protected trait StartingForAdStateT extends FsmState {

    override def afterBecome(): Unit = {
      super.afterBecome()
      val sd0 = _stateData
      for (screen <- sd0.screen;  fState0 <- sd0.focused;  fRoot <- FRoot.find() ) {
        // Собрать и запустить fads-реквест на основе запроса к карточкам плитки.
        val startOffset = Math.max(0, fState0.currIndex - 1)
        // Флаг: запрашивать ли карточку, предшествующую запрошенной. Да, если запрошена ненулевая карточка.
        val withPrevAd = startOffset > 0
        val args = new MFocAdSearchEmpty with FindAdsArgsT {
          override def _sd = sd0
          override def firstAdId = fState0.firstAdId
          // Выставляем под нужды focused-выдачи значения limit/offset.
          override def offset = Some( startOffset )
          override def limit = Some( if (withPrevAd) 3 else 2 )
          //override def levelId = Some(ShowLevels.ID_PRODUCER)  // TODO Тут должно быть что-то, не?.
        }
        val fadsFut = MFocAds.find(args)

        // Подготовить focused-карусель к работе.
        val car = FCarousel()
        // Ширина ячейки в карусели эквивалентна пиксельной ширине экрана.
        val cellWidthPx = screen.width
        // Начальная ширина карусели задаётся исходя из текущих ячеек. +1 -- Скорее всего будет как минимум одна карточка после текущей.
        car.setWidthPx( cellWidthPx * (fState0.currIndex + 1) )
        // Начальный сдвиг карусели выставляем без анимации. -1 т.к. первая карточка должна выезжать из-за экрана.
        val carLeftPx = -cellWidthPx * (fState0.currIndex - 1)
        car.animateToX( carLeftPx )
        // Подключить собранную карусель к работе
        fRoot.replaceCarousel( car )
        // Пустая карусель, но к работе вроде готова.
        car.enableTransition()
        // TODO Карусель пустая. Не будет ли обратного эффекта от такой оптимизации?
        car.willAnimate()

        // Обновить состояние FSM.
        _stateData = sd0.copy(
          focused = Some(fState0.copy(
            ???
          ))
        )

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
      val fads = mfa.focusedAdsIter.toSeq
      // Залить в карусель полученные карточки.
      val sd0 = _stateData
      for (fState <- sd0.focused;  screen <- sd0.screen;  car <- FCarousel.find()) {
        // Сначала обрабатываем запрошенную карточку:
        val cellWidth = screen.width
        // Индекс запрошенной карточки в массиве fads: она или первая крайняя, или вторая при наличии предыдущей.
        val firstAdFadsIndex = if (fState.currIndex > 0) 1 else 0
        val firstAd = fads(firstAdFadsIndex)
        val cell1 = FCarCell()
        cell1.setWidthPx( cellWidth )
        cell1.setContent( firstAd.html )
        // Повесить запрошенную карточку на шаг правее нужного индекса, чтобы можно было прослайдить на неё.
        cell1.setLeftPx( fState.currIndex * cellWidth )

        // Прилинковываем запрошенную карточку справа и запускаем анимацию.
        car.pushCellRight(cell1)
        car.animateToX( fState.currIndex * cellWidth )

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


  /** Состояние, когда запрошены у сервера карточки. */
  protected class WaitForFadsState(nextIndex: Int) extends FsmState {
    override def receiverPart: Receive = {
      case FadsReceived(fads) =>
        val fadsIter2 = fads.focusedAdsIter.map { fad =>
          fad.index -> FocAd(fad)
        }
        /*
          fads.map { fad  =>  fad.index -> FocAd(fad) }
        val sd0 = _data
        val stateData1 = sd0.copy(
          ads           = sd0.ads ++ fadsIter2,
          loadedCount   = sd0.loadedCount + fads.fadsCount,
          totalCount    = Some(fads.totalCount)
        )
        */
        // TODO Переключиться на следующее состояние.
        ???
    }
  }

}
