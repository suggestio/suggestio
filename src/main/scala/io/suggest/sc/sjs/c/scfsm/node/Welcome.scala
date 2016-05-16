package io.suggest.sc.sjs.c.scfsm.node

import io.suggest.sc.ScConstants.Welcome
import io.suggest.sc.sjs.c.scfsm.grid
import io.suggest.sc.sjs.m.magent.IVpSzChanged
import io.suggest.sc.sjs.m.mwc.{WcClick, WcTimeout}
import io.suggest.sc.sjs.vm.wc.{WcBgImg, WcFgImg, WcRoot}
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.model.TimeoutPromise
import io.suggest.sjs.common.msg.WarnMsgs

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

  /** Трейт сборки состояния приветствия узла.
    * Изначально, это был зоопарк NodeInit_* состояний, потом пришлось объединять для упрощения обработки screen resize.
    *
    * Обычно суть состояния в том, чтобы отобразить приветствие, и в это время подготовить плитку.
    * Скрывать приветствие параллельно или по наступлению сигналов.
    *
    * Считается, что запрос findAds уже запущен где-то снаружи, что заметно ускоряет работу при инициализации плитки,
    * но ущербно выглядит в целом. См. [[Index.ProcessIndexReceivedUtil]]#_nodeIndexReceived().
    */
  trait NodeInit_Welcome_AdsWait_StateT extends GridAdsWaitLoadStateT {

    /** Класс, описывающий состояние текущего welcome'а, если он существует в DOM. */
    private case class WcHideState(isHiding: Boolean, info: TimeoutPromise)

    /** Чтобы не заваливать основное состояние этим mutable-мусором,
      * сохраняем прямо тут текущее внутреннее состояние сокрытия welcome'а. */
    private var _wcHide: Option[WcHideState] = None


    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запускать запрос _findGridAds() не нужно, т.к. считается, что он был запущен где-то снаружи.

      WcRoot.find().fold[Unit] {
        // Нет приветствия. Сразу перейти на состояние плитки. Оно же и отработает приём FindAds.
        become(_nodeInitDoneState)

      } { wcRoot =>
        // Есть приветствие. Инициализировать его и организовать плавное сокрытие с задержкой.
        val sd0 = _stateData
        // Подготовить отображение карточки.
        for (screen  <- sd0.screen) {
          wcRoot.initLayout(screen)
        }
        wcRoot.willAnimate()

        // Запустить таймер запуска сокрытия welcome-картинки.
        val startHideTp = DomQuick.timeoutPromise( Welcome.HIDE_TIMEOUT_MS )
        _wcHide = Some( WcHideState(isHiding = false, startHideTp) )

        // Собрать и подписать future запуска сокрытия welcome'а.
        val startHideFut = startHideTp.fut

        // Запланировать дальнейшие действия с карточкой приветствия.
        for {
          // Когда таймер запуска сокрытия сработает...
          _ <- startHideFut
          // Запустить анимацию сокрытия, запустив таймер завершения этой самой анимации.
          _ <- {
            wcRoot.fadeOut()
            val tp = DomQuick.timeoutPromise(Welcome.FADEOUT_TRANSITION_MS)
            _wcHide = Some( WcHideState(isHiding = true, tp) )
            // Контроллировтаь WcTimeout доп.параметрами (типа generation) не требуется, т.к. сообщения WcTimout носят уведомительный характер.
            _sendEvent(WcTimeout)
            tp.fut
          }
        } {
          // Высвободить ресурсы из под welcome'а.
          wcRoot.remove()
          _wcHide = None
          _sendEventSync(WcTimeout)
        }
      }
    }


    override def receiverPart: Receive = super.receiverPart.orElse {
      // Сигнал таймаута стадии отображения приветствия.
      case WcTimeout =>
        _handleWcTimeout()
      // Сигнал, что юзер кликнул по приветствию.
      case wcc: WcClick =>
        _handleWcClicked(wcc)
    }


    /** Реакция на сокрытие welcome или педалирование этого сокрытия юзером. */
    def _handleWcTimeout(): Unit = {
      // Если приветствие более не отображается на экране, выполнить переключение на следующее состояние.
      if (_wcHide.isEmpty) {
        become(_nodeInitDoneState)
      }
    }


    /** Реакция на клик юзера по приветствию. */
    def _handleWcClicked(wcc: WcClick): Unit = {
      _wcHide.fold [Unit] {
        // Should not happen: Юзер кликает по уже сокрытому и удалённом приветствию.
        warn( WarnMsgs.NODE_WELCOME_MISSING + " " + _stateData.adnIdOpt )
        become(_nodeInitDoneState)

      } { wcInfo =>
        // Не прерываем анимацию, только педалируем её запуск. Это также защищает от двойных/дублирующихся кликов.
        if (!wcInfo.isHiding) {
          // Педалируем завершения фьючерса не дожидаясь его таймера.
          DomQuick.clearTimeout(wcInfo.info.timerId)
          wcInfo.info.promise.trySuccess()
        }
      }
    }


    /** Опциональное переключение на новое состояние при завершении запроса рекламных карточек. */
    private def _findAdsDoneState: FsmState = {
      if (_wcHide.isEmpty) {
        _nodeInitDoneState
      } else {
        // Ещё не закончилось сокрытие приветствия. Переключение состояния будет после его сокрытия.
        null
      }
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

      // TODO Принять действия по поводу выдачи, иначе она будет кривая.
    }


    override protected def _adsLoadedState: FsmState = _findAdsDoneState
    override protected def _findAdsFailedState: FsmState = _findAdsDoneState

    /** На какое состояние переключаться, когда финальная инициализация выдачи узла с приветствием закончены? */
    def _nodeInitDoneState: FsmState

  }

}
