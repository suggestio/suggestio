package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.c.scfsm.node.Index
import io.suggest.sc.sjs.m.mfoc.FocRootAppeared
import io.suggest.sc.sjs.m.msrv.foc.find.{MFocAdSearchDflt, MFocAds}
import io.suggest.sc.sjs.m.msrv.index.MScRespIndex
import io.suggest.sc.sjs.vm.layout.FsLoader
import io.suggest.sc.sjs.vm.res.FocusedRes
import io.suggest.sc.sjs.vm.foc.fad.FAdRoot
import io.suggest.sc.sjs.vm.foc.{FCarCont, FControls, FRoot}
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import scala.util.Failure
import io.suggest.sc.ScConstants.Focused
import io.suggest.sc.sjs.m.msc.MFindAdsArgsT
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.model.mlu.MLookupModes

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

trait StartingForAd extends MouseMoving with Index {

  /**
    * Трейт для состояния, когда focused-выдача отсутствует, скрыта вообще и ожидает активации.
    * При появлении top-level ScFsm это событие исчезнет, и будет обрабатываться где-то в вышестоящем обработчике.
    */
  protected trait StartingForAdStateT extends FsmEmptyReceiverState with ProcessIndexReceivedUtil {

    override def afterBecome(): Unit = {
      super.afterBecome()

      val sd0 = _stateData
      for {
        fState0 <- sd0.focused
      } {
        // Необходимо по-скорее запустить focused ad реквест к серверу. Подготовка аргументов поиска:
        val args = new MFocAdSearchDflt with MFindAdsArgsT {
          override def _sd              = sd0
          override def limit            = Some( Focused.AROUND_LOAD_LIMIT )
          override def producerId       = fState0.producerId
          // Новая (2016.may) методика поиска карточек подразумевает выборку вокруг карточки на первом шаге.
          override def adsLookupMode    = MLookupModes.Around
          override def adIdLookup       = fState0.current.madId
          // Разрешить серверу делать перескок выдачи, если не форсируется lookup-карточки.
          override def allowReturnJump  = !fState0.current.forceFocus
        }
        // Запустить поиск focused-карточек в фоне.
        val fadsFut = MFocAds.findOrIndex(args)

        // Продолжить синхронные операции с каруселью.
        for {
          fRoot   <- FRoot.find()
          car     <- fRoot.carousel
        } {

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
          // Начальная ширина карусели задаётся исходя из текущих ячеек.
          car.setCellWidth(1, sd0.common.screen)
          // Начальный сдвиг карусели выставляем без анимации. Весь focused будет выезжать из-за экрана.
          car.disableTransition()
          car.animateToCell(0, sd0.common)

          // Подготовить контейнер для стилей.
          FocusedRes.ensureCreated()
          // Подготовить контейнер для заголовка и прочего FControls.
          for (fControls <- FControls.find()) {
            fControls.clear()
          }

          // повесить листенер для ожидания ответа сервера.
          _sendFutResBack {
            fadsFut
          }
        }
      }
    }


    /** Реакция на полученный ответ сервера. */
    protected def _focAdsReceived(mfa: MFocAds): Unit = {
      // Заливаем все полученные стили в DOM.
      FocCommon.appendStyles(mfa)

      // Залить в карусель полученные карточки.
      val sd0 = _stateData
      for {
        fState    <- sd0.focused
        fRoot     <- FRoot.find()
        car       <- fRoot.carousel
      } {

        val fads2 = if (fState.fads.isEmpty) {
          mfa.fads
        } else {
          fState.fads ++ mfa.fads
        }

        val currAdId = fState.current.madId
        val firstAd = mfa.fads
          .find(_.madId == currAdId)
          .orElse {
            warn( WarnMsgs.FOC_AD_NOT_FOUND_IN_RESP + " " + currAdId + " " + mfa )
            fads2.headOption
          }
          .get

        // Сначала обрабатываем запрошенную карточку:
        // Индекс запрошенной карточки в массиве fads: она или первая крайняя, или вторая при наличии предыдущей.
        val fadRoot = FAdRoot( firstAd.bodyHtml )
        fadRoot.initLayout( sd0.common )
        // Повесить запрошенную карточку на месте текущего индекса.
        val currIndex = fState.current.index //firstAd.index
        fadRoot.setLeft( currIndex, sd0.common.screen )

        // Прилинковываем запрошенную карточку справа и запускаем анимацию.
        car.pushCellRight(fadRoot)
        car.animateToCell(currIndex, sd0.common)

        // Начата обработка тяжелого тела focused-карточки. Залить текущий заголовок focused-выдачи.
        for (fControls <- fRoot.controls) {
          fControls.setContent( firstAd.controlsHtml )
        }

        fRoot.willAnimate()
        fRoot.show()
        fRoot.enableTransition()

        val repsFadsIds = mfa.fads.map(_.madId)
        // Если id крайней (первой, последней) карточки в around-ответе содержит id текущей карточки, то значит известна id крайней первой/последней карточки.
        // В противном случае, сервер вернул бы текущую карточку в середине around-ответа.
        def __maybeLastAdId(lastIdOpt: Option[String]): Option[String] = {
          lastIdOpt.filter(_ == currAdId)
        }

        val sd1: SD = sd0.copy(
          focused = Some( fState.copy(
            //current = fState.current.copy(
            //  forceFocus = false
            //),
            totalCount  = Some(mfa.totalCount),
            fads        = fads2,
            // Если id первой карточки в around-ответе равен текущей карточке, то значит известна id крайней первой карточки.
            firstAdId   = __maybeLastAdId( repsFadsIds.headOption ),
            lastAdId    = __maybeLastAdId( repsFadsIds.lastOption )
          ))
        )
        become(_focOnAppearState, sd1)

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
      case mfa: MFocAds =>
        _focAdsReceived(mfa)
      case mni: MScRespIndex =>
        _nodeIndexReceived(mni)
      case Failure(ex) =>
        _focAdsRequestFailed(ex)
    }

    /** При ошибке надо возвращаться назад в сетку. */
    override def processFailure(ex: Throwable): Unit = {
      super.processFailure(ex)
      _backToGrid()
    }

    /** Реакция на успешный результат запроса node index. */
    override protected def _nodeIndexReceived(mni: MScRespIndex): Unit = {
      // Предварительно подготовить состояние к переключению.
      _stateData = _stateData.withNodeSwitch( mni.adnIdOpt )
      super._nodeIndexReceived(mni)
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
        DomQuick.setTimeout( Focused.SLIDE_ANIMATE_MS ) { () =>
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
      for (car <- FCarCont.find()) {
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
