package io.suggest.sc.sjs.c.scfsm.node

import io.suggest.sc.ScConstants.Welcome
import io.suggest.sc.sjs.c.scfsm.grid
import io.suggest.sc.sjs.m.magent.IVpSzChanged
import io.suggest.sc.sjs.m.mwc.{IWcStepSignal, WcTimeout}
import io.suggest.sc.sjs.vm.wc.{WcBgImg, WcFgImg, WcRoot}
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.model.TimeoutPromise
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.15 15:41
  * Description: Аддон для ScFsm для сборки состояний, связанных с карточкой приветствия.
  *
  * 2016.may.13: Производится объединение зоопарка состояний NodeInit_* (инициализации узла)
  * в welcome-фазе, т.е. после шага получения index'а с сервера.
  * Данный Welcome-контейнер трейтов получил более широкое назначение:
  * welcome-фаза-состояние или трейты для его сборки.
  */
trait Welcome extends grid.Append {

  /** Дедублицированный код опциональной отмены таймера, сохраненного в состоянии,
    * в зависимости от полученного сигнала. */
  private def _maybeCancelWcTimer(signal: IWcStepSignal): Unit = {
    if (signal.isUser)
      _stateData.maybeCancelTimer()
  }

  /** Интерфейс с методом, возвращающим выходное состояние (выход из welcome-фазы). */
  trait IWelcomeFinished {
    /** Состояние, когда welcome-карточка сокрыта и стёрта из DOM. */
    protected def _welcomeFinishedState: FsmState
  }

  trait IWelcomeHiding {
    /** Состояние, когда происходит плавное сокрытие welcome-карточки. */
    protected def _welcomeHidingState: FsmState
  }


  /** Трейт для сборки состояний нахождения на welcome-карточке. */
  trait OnWelcomeShownStateT extends FsmEmptyReceiverState with IWelcomeFinished with IWelcomeHiding {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Приём сигнала от таймера о необходимости начать сокрытие карточки приветствия. Либо юзер тыкает по welcome-карточке.
      case signal: IWcStepSignal =>
        _maybeCancelWcTimer(signal)
        _letsHideWelcome()
    }

    /** Необходимо запустить плавное сокрытие welcome-карточки. */
    protected def _letsHideWelcome(): Unit = {
      val wcRootOpt = WcRoot.find()
      val hideTimerIdOpt = for (wcRoot <- wcRootOpt) yield {
        wcRoot.fadeOut()
        DomQuick.setTimeout( Welcome.FADEOUT_TRANSITION_MS ) { () =>
          _sendEventSyncSafe(WcTimeout)
        }
      }
      val nextState = wcRootOpt.fold[FsmState] (_welcomeFinishedState) (_ => _welcomeHidingState)
      val sd2 = _stateData.copy(
        timerId = hideTimerIdOpt
      )
      become(nextState, sd2)
    }

    /** Реакция на сигнал об изменении размеров окна или экрана устройства. */
    override def _viewPortChanged(e: IVpSzChanged): Unit = {
      super._viewPortChanged(e)

      // Подогнать bg img под новые параметры экрана.
      for {
        mscreen <- _stateData.screen
        wcBg    <- WcBgImg.find()
      } {
        wcBg.adjust(mscreen)
      }

      // Подогнать размеры fg img в связи с новыми параметрами экрана.
      for (wcFg <- WcFgImg.find()) {
        wcFg.adjust()
      }
    }

  }


  /** Трейт для сборки состояний, реагирующий на завершение плавного сокрытия welcome-карточки. */
  trait OnWelcomeHidingState extends FsmEmptyReceiverState with IWelcomeFinished {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Сработал таймер окончания анимированного сокрытия welcome. Или юзер тыкнул по welcome-карточке.
      case signal: IWcStepSignal =>
        _maybeCancelWcTimer(signal)
        _letsFinishWelcome()
    }

    protected def _letsFinishWelcome(): Unit = {
      for (wcRoot <- WcRoot.find()) {
        wcRoot.remove()
      }
      val sd2 = _stateData.copy(
        timerId = None
      )
      become(_welcomeFinishedState, sd2)
    }

  }



  // TODO Описать тут новое состояние.

  /** Трейт сборки состояния приветствия узла.
    * Изначально, это был зоопарк NodeInit_* состояний, потом пришлось объединять для упрощения обработки screen resize.
    *
    * Обычно суть состояния в том, чтобы отобразить приветствие, и в это время подготовить плитку.
    * Скрывать приветствие параллельно или по наступлению сигналов.
    *
    * Считается, что запрос findAds уже запущен где-то снаружи, что заметно ускоряет работу при инициализации плитки,
    * но ущербно выглядит в целом. См. [[Index.ProcessIndexReceivedUtil]]#_nodeIndexReceived().
    */
  trait NodeInit_AdsWait_StateT extends GridAdsWaitLoadStateT {

    /** Чтобы не заваливать основное состояние этим mutable-мусором,
      * сохраняем прямо тут текущее состояние сокрытия welcome'а. */
    private var _wcHideInfo: Option[TimeoutPromise] = None

    override def afterBecome(): Unit = {
      super.afterBecome()
      // TODO Запустить отображение welcome-карточки и таймер запуска сокрытия оной.
      // TODO Запустить запрос _findGridAds

      val sd0 = _stateData

      // Запустить инициализацию welcome layout'а.
      for (wcRoot <- WcRoot.find()) {
        // Подготовить отображение карточки.
        for (screen  <- sd0.screen) {
          wcRoot.initLayout(screen)
        }
        wcRoot.willAnimate()

        // Запустить таймер запуска сокрытия welcome-картинки.
        val startHideTp = DomQuick.timeoutPromise( Welcome.HIDE_TIMEOUT_MS )
        _wcHideInfo = Some(startHideTp)

        val startHideFut = startHideTp.promise.future

        for (_ <- startHideFut) {

        }

        // Запланировать дальнейшие действия с карточкой приветствия.
        for {
          // Когда таймер запуска сокрытия сработает...
          _ <- startHideFut
          // Запустить анимацию сокрытия, запустив таймер завершения этой самой анимации.
          _ <- {
            wcRoot.fadeOut()
            val tp = DomQuick.timeoutPromise(Welcome.FADEOUT_TRANSITION_MS)
            _wcHideInfo = Some(tp)
            tp.promise.future
          }
        } {
          // Высвободить ресурсы из под welcome'а.
          wcRoot.remove()
          _wcHideInfo = None
        }

      }   // for wcRoot
    }

    override def receiverPart: Receive = super.receiverPart.orElse {
      ???
    }
  }

}
