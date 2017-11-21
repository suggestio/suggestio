package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.common.html.HtmlConstants
import io.suggest.common.{MHand, MHands}
import io.suggest.common.m.mad.IMadId
import io.suggest.sc.ScConstants
import io.suggest.sc.sjs.m.mfoc._
import io.suggest.sc.sjs.m.mfsm.touch.TouchStart
import io.suggest.sc.sjs.vm.foc.{FCarCont, FRoot}
import io.suggest.sc.sjs.vm.foc.fad.{FAdRoot, FAdWrapper, FArrow}
import io.suggest.sc.ScConstants.Focused.FAd.KBD_SCROLL_STEP_PX
import io.suggest.sc.sjs.c.scfsm.grid.OnGridBase
import io.suggest.sc.sjs.c.scfsm.ust.State2UrlT
import io.suggest.sc.sjs.c.scfsm.{ResizeDelayed, ScFsmStub}
import io.suggest.sc.sjs.m.msrv.foc.{MFocAdSearchDflt, MFocAdSearchNoOpenIndex, MScAdsFoc}
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sc.focus._
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.util.TouchUtil
import org.scalajs.dom.{KeyboardEvent, MouseEvent, TouchEvent}
import org.scalajs.dom.ext.KeyCode

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.08.15 18:44
 */

/** Контейнер трейтов-интерфейсов для разработки focused-состояний. */
trait IOnFocusBase extends ScFsmStub {

  /** Интерейс состояний шифтинга влево и вправо. */
  protected trait ISimpleShift {

    /** Состояние переключения на следующую карточку. */
    protected def _shiftRightState: FsmState

    /** Состояние переключения на предыдущую карточку. */
    protected def _shiftLeftState : FsmState

    /** Код шифтинга в указанную сторону. */
    protected def _shiftForHand(mhand: MHand): FsmState = {
      if (mhand.isLeft)
        _shiftLeftState
      else
        _shiftRightState
    }

  }


  /** Интерфейс для поля с инстансом FSM-состояния OnFocusDelayedResize. */
  protected trait IStartFocusOnAdState {
    protected def _startFocusOnAdState: FsmState
  }

}


/**
  * Аддон для сборки состояний нахождения "в фокусе", т.е. на УЖЕ открытой focused-карточки.
  * Base -- трейт с вещами, расшаренными между трейтами конкретных состояний.
  */
trait OnFocusBase
  extends MouseMoving
    with ResizeDelayed
    with IOnFocusBase
    with OnGridBase
    with State2UrlT
{

  /** Поддержка отложенного ресайза focused-выдачи. */
  protected trait OnFocusDelayedResize
    extends DelayResize with HandleResizeDelayed
      with IStartFocusOnAdState
      with GridHandleViewPortChangedSync
  {

    // TODO override def _viewPortChanged(): Unit
    // Подгонять текущий focused div height контейнеры под изменяющийся экран.

    // При наступлении необходимости ресайза надо скрыть текущие focused-карточки и
    override def _handleResizeDelayTimeout(): Unit = {
      val sd0 = _stateData
      if (_isScrWidthReallyChanged()) {
        // Обновить данные состояния в связи с наступающим ресайзом.
        // Иначе будут проблемы с floation-стрелочками влево-вправо, текущей просмотраваемой карточкой и т.д.
        for (fState0 <- sd0.focused) {
          _stateData = sd0.copy(
            common = sd0.common.copy(
              resizeOpt = None
            ),
            focused = Some(MFocSd(
              current = fState0.current,
              producerId = fState0.producerId
            ))
          )
        }
        // Перещелкнуть на след.состояние.
        become(_startFocusOnAdState)
      }
    }
  }


  /** Трейт для поддержки общения с сервером в рамках focused-выдачи. */
  protected trait OnFocSrvResp extends FsmEmptyReceiverState {

    // Без override, т.к. это дело тут почему-то ничего не оверрайдит пока что.
    override def receiverPart: Receive = super.receiverPart.orElse {
      case mfaRespTs: MFocSrvRespTs =>
        _handleSrvResp(mfaRespTs)
    }

    def _analyzeCurrentFads(): Unit = {
      for (car <- FCarCont.find()) {
        _analyzeCurrentFads(car)
      }
    }
    /** Проверить имеющиеся сейчас в наличии карточки. */
    def _analyzeCurrentFads(car: FCarCont): Unit = {
      val sd0 = _stateData

      for (fState <- sd0.focused) {
        val currAdId = fState.current.madId

        // Отработать next-карточку: сначала понять, не является ли текущая карточка крайней справа.
        val fadsAfterCurrent = fState.fadsAfterCurrentIter.toStream

        if (!fState.isCurrAdLast && fadsAfterCurrent.isEmpty) {
          // Надо запустить запрос следующих (вправо) карточек с сервера. Запустить запрос к серваку за след.порцией карточек.
          _maybeReqMoreFads(MLookupModes.After)

        } else {

          // Дедубликация кода фонового рендера focused-карточки.
          def __prepareFad(fad: IFocAd, indexDelta: Int): FAdRoot = {
            val fadRoot = FAdRoot(fad.bodyHtml)
            fadRoot.initLayout(sd0.common)
            fadRoot.setLeft(fState.current.index + indexDelta, sd0.common.screen)
            fadRoot
          }

          // Собираем текущие карточки карусели для дальнейшего многократного использования.
          val nowShownFadIds0 = car.fadRootsIter
            .flatMap(_.madId)
            .toStream

          // next-карточку запрашивать с сервера не требуется, но если карточка отсутствует в карусели, то закинуть её в карусель.
          for {
            nextFad <- fadsAfterCurrent.headOption
            if !nowShownFadIds0.iterator
              // Пропустить мимо карточки, предшествующей текущей вместе с текущей.
              .dropWhile(_ != currAdId)
              .drop(1)
              .contains(nextFad.madId)
          } {
            // next-карточка доступна, но её нет в карусели. Отрендерить карточку, затолкать в карусель.
            val fadRoot = __prepareFad(nextFad, +1)
            car.pushCellRight(fadRoot)
          }

          // Пора проверить prev-карточку аналогично. Возможно её надо запросить с сервера или же затолкать в карусель.
          val fadsBeforeCurrent = fState.fadsBeforeCurrentIter.toStream
          if (!fState.isCurrAdFirst && fadsBeforeCurrent.isEmpty) {
            // Нет предшествующих карточек, которые скорее всего существуют. Запросить их с сервера...
            _maybeReqMoreFads(MLookupModes.Before)

          } else {
            // Есть какие-то предшествующие карточки. Если в карусели нет пред.карточки, то затолкать туда готовую.
            for {
              prevFad <- fadsBeforeCurrent.lastOption
              if !nowShownFadIds0.iterator
                .takeWhile(_ != currAdId)
                .contains(prevFad.madId)
            } {
              // prev-карточка доступна, но её нет в карусели. Исправить этот недостаток:
              val fadRoot = __prepareFad(prevFad, -1)
              car.pushCellLeft(fadRoot)
            }

          } // if prev ad
        } // if next ad
      }
    }


    /**
      * Попытаться запустить preload-запрос поиска карточек в указанном направлении.
      * Возможные результаты запуска запроса будут сохранены в состояние.
      */
    protected def _maybeReqMoreFads(where: MLookupMode): Unit = {
      val sd0 = _stateData
      for {
        fState0   <- sd0.focused
        if fState0.req.isEmpty
      } {
        //println("req more fads: " + where)
        val currAdId  = fState0.current.madId

        // Собрать параметры для поиска
        val reqArgs = new MFocAdSearchDflt with MFindAdsArgsT with MFocAdSearchNoOpenIndex {
          override def _sd            = sd0
          // Выставляем под нужды focused-выдачи значения limit/offset.
          override def limit          = Some( ScConstants.Focused.SIDE_PRELOAD_MAX )
          override def adsLookupMode  = where
          override def adIdLookup     = currAdId
          override def producerId     = fState0.producerId
        }

        // Отправить запрос focused-карточек.
        val fadsRepsFut = for (
          mfa <- MScAdsFoc.find(reqArgs)
        ) yield {
          MFocSrvResp(mfa, reqArgs)
        }

        // При завершении надобно оповестить FSM о столь знаменательном событии вместе с реквизитами запроса.
        val reqTimestamp = _sendFutResBackTimestamped(fadsRepsFut, MFocSrvRespTs)

        // Сохранить данные о текущем запросе в состояние.
        _stateData = sd0.copy(
          focused = Some(fState0.copy(
            req = Some(MFocReqInfo(
              timestamp = reqTimestamp,
              fut       = fadsRepsFut
            ))
          ))
        )
      }
    }


    /** Реакция на получение карточек с сервера: запихать карточки в состояние,
      * выполнить рендер если необходимо. */
    def _handleSrvResp(mfaRespTs: MFocSrvRespTs): Unit = {
      val focReqOpt = _stateData.focused.flatMap(_.req)
      val hasTimestamp = focReqOpt.exists(_.timestamp == mfaRespTs.timestamp)
      // Проверять timestamp. Это для самоконтроля + для случаев, когда focused-выдача была переоткрыта юзером, а запрос был повисшим всё это время.
      if (hasTimestamp) {
        mfaRespTs.result match {
          // Получен положительный ответ от сервера:
          case Success(res) =>
            _handleSrvRespOk(res)

          // Ошибка выполнения запроса.
          case Failure(ex) =>
            // выкинуть данные реквеста из состояния, если реквест тот, который и должен быть.
            LOG.error(WarnMsgs.FOC_RESP_TS_UNEXPECTED, ex)
        }

        // Сохранить итоги обработки ответа сервера в состояние.
        val sd0 = _stateData
        _stateData = sd0.copy(
          focused = sd0.focused.map(_.copy(
            req = None
          ))
        )

        // Посмотреть, не надо ли ещё подгрузить карточек? Или новые текущие затолкать в foc-карусель?
        _analyzeCurrentFads()

      } else {
        LOG.warn(WarnMsgs.FOC_RESP_TS_UNEXPECTED, msg = focReqOpt.toString)
      }
    }

    /** Среагировать на положительный ответ сервера. */
    def _handleSrvRespOk(res: MFocSrvResp): Unit = {
      val sd0 = _stateData
      for (fState0 <- sd0.focused) {
        // Закинуть карточки в аккамулятор fads в зависимости от режима сделанного запроса.
        val focUpdaterComp: IFocUpdaterCompanion = res.reqArgs.adsLookupMode match {
          // Искались карточки после указанной карточки.
          case MLookupModes.After =>
            AfterFocUpdater
          // Запрашивались карточки, которые были перед указанной.
          case MLookupModes.Before =>
            BeforeFocUpdater
          // Других направлений запроса карточек быть не может, выдаём ошибку.
          case other =>
            // Тут было более подробное сообщение об ошибке, но т.к. вызов этого кода в реальности невероятен, всё очень упрощено:
            throw new IllegalArgumentException( ErrorMsgs.FOC_ANSWER_ACTION_INVALID + HtmlConstants.SPACE + other )
        }

        // Залить новые стили в DOM.
        FocCommon.appendStyles( res.resp )

        // Провести обновление состояния
        _stateData = sd0.copy(
          focused = Some(
            focUpdaterComp(fState0, res)
              .fState2()
          )
        )
      }
    }

  }


  /** Заготовка для состояний, связанных с нахождением на карточке.
    * Тут реакция на события воздействия пользователя на focused-выдачу. */
  protected trait OnFocusStateBaseT
    extends OnFocusDelayedResize
      with OnFocSrvResp
      with FocMouseMovingStateT
      with INodeSwitchState
      with ISimpleShift
  {

    override def receiverPart: Receive = {
      val r2: Receive = {
        case TouchStart(event) =>
          _onTouchStart(event)
        case CloseBtnClick =>
          _closeFocused()
        case ProducerLogoClick =>
          _goToProducer()
        case MouseClick(evt) =>
          _mouseClicked(evt)
      }
      r2.orElse( super.receiverPart )
    }

    /** С началом свайпа надо инициализировать touch-параметры и перейти в свайп-состояние. */
    protected def _onTouchStart(event: TouchEvent): Unit = {
      val sd0 = _stateData
      val touch = event.touches(0)
      val sd1 = sd0.copy(
        focused = sd0.focused.map( _.copy(
          touch = Some(MFocTouchSd(
            start = TouchUtil.touch2coord( touch ),
            lastX = touch.pageX
          ))
        ))
      )
      become(_onTouchStartState, sd1)
    }
    protected def _onTouchStartState: FsmState


    override def _onKbdKeyUp(event: KeyboardEvent): Unit = {
      super._onKbdKeyUp(event)

      // Узнать нажатую клавишу и среагировать на неё.
      val c = event.keyCode
      // ESC должен закрывать выдачу.
      if (c == KeyCode.Escape)
        _closeFocused()
      // Клавиатурные стрелки влево-вправо должны переключать карточки в соотв. направлениях.
      else if (c == KeyCode.Right)
        _kbdShifting( MHands.Right, _shiftRightState )
      else if (c == KeyCode.Left)
        _kbdShifting( MHands.Left, _shiftLeftState )
      // TODO Скроллинг должен быть непрерывным. Сейчас он срабатывает только при отжатии клавиатурных кнопок скролла.
      else if (c == KeyCode.Down)
        _kbdScroll( KBD_SCROLL_STEP_PX )
      else if (c == KeyCode.Up)
        _kbdScroll( -KBD_SCROLL_STEP_PX )
      else if (c == KeyCode.PageDown)
        _kbdScroll( screenH )
      else if (c == KeyCode.PageUp)
        _kbdScroll( -screenH )
    }

    private def screenH: Int = {
      _stateData.common.screen.height
    }

    protected def _kbdScroll(delta: Int): Unit = {
      // Найти враппер текущей карточки и проскроллить его немного вниз.
      for {
        fState    <- _stateData.focused
        fWrap     <- FAdWrapper.find( fState.current.madId )
      } {
        fWrap.vScrollByPx(delta)
      }
    }

    /** Реакция на переключение focused-карточек стрелками клавиатуры.
      *
      * @param dir направление переключения.
      */
    protected def _kbdShifting(dir: MHand, nextState: FsmState): Unit = {
      if (_filterSimpleShiftSignal(dir)) {
        val sd0 = _stateData
        val sd1 = sd0.copy(
          focused = sd0.focused.map(_.copy(
            arrDir = Some(dir)
          ))
        )
        become(nextState, sd1)
      }
    }

    /** Реакция на клик по кнопке закрытия или иному выходу из focused-выдачи. */
    protected def _closeFocused(): Unit = {
      become(_closingState)
    }

    /** Состояние процесса закрытия focused-выдачи. */
    protected def _closingState: FsmState

    /** Реакция на сигнал перехода на producer-выдачу. */
    protected def _goToProducer(): Unit = {
      val sd0 = _stateData
      // Найти в состоянии текущую карточку. Узнать продьюсера,
      for {
        fRoot     <- FRoot.find()
        fState    <- {
          fRoot.willAnimate()
          sd0.focused
        }
        fAdRoot   <- fState.findCurrFad
      } {
        val adnId = fAdRoot.producerId
        if ( sd0.common.adnIdOpt.contains(adnId) ) {
          // Возврат на плитку текуйщего узла отрабатывается соответствующим состоянием.
          become(_closingState)

        } else {
          // Переход на другой узел.
          val sd1 = sd0.withNodeSwitch( Some(adnId) )
          become(_onNodeSwitchState, sd1)
          // Скрыть текущую focused-выдачу в фоне. Глубокая чистка не требуется, т.к. layout будет полностью пересоздан.
          DomQuick.setTimeout(10) { () =>
            fRoot.disappearTransition()
          }
        }
      }
    }

    /** Можно игнорить шифтинг карточек в указанном направлении с помощью этого флага.
      * TODO Это костыль. По хорошему надо шифтить на следующую карточку и отображать там loader, а потом это сразу перезаписывать.
      */
    protected def _filterSimpleShiftSignal(mhand: MHand): Boolean = {
      true
    }

    /** Реакция на кликанье мышки по focused-выдаче. Надо понять, слева или справа был клик, затем
      * запустить листание в нужную сторону. */
    protected def _mouseClicked(event: MouseEvent): Unit = {
      val sd0 = _stateData
      for {
        fState <- sd0.focused
      } {
        val mhand = _mouse2hand(event, sd0.common.screen)
        for (fArr <- FArrow.find()) {
          _maybeUpdateArrDir(mhand, fArr, fState, sd0)
        }
        if (_filterSimpleShiftSignal(mhand)) {
          become( _shiftForHand(mhand) )
        }
      }
    }


    // При возврате в плитку из focused-выдачи можно отработать всё по укороченному сценарию.
    override def _handleStateSwitch(sdNext: SD): Unit = {
      sdNext.focused.fold[Unit] {
        // Новое состояние -- без focused-выдачи. Отработать закрытие текущей focused-выдачи:
        _closeFocused()

      } { fStateNext =>
        // Какие-то перескоки по истории браузера внутри focused-выдачи.
        val sd0 = _stateData
        val nextMadId = fStateNext.current.madId

        sd0.focused
          // Отработать только ситуацию смены карточки.
          .filter { fState0 =>
            nextMadId != fState0.current.madId
          }
          .fold[Unit] {
            // Это не смена foc-карточки, а что-то иное вообще.
            super._handleStateSwitch(sdNext)

          } { fState0 =>
            // Перейти на новую focused-карточку в рамках текущей выдачи.
            // Вполне возможно, что она находится слева или справа от текущей. Отработать эти ситуации:
            def __tryFirst(fadOpt: Option[IFocAd], dir: MHand): Boolean = {
              val has = fadOpt.exists(_.madId == nextMadId)
              if (has)
                become(_shiftForHand(dir))
              has
            }
            // Искомая карточка -- предыдущая (слева)?
            __tryFirst(fState0.fadsBeforeCurrentIter.toSeq.lastOption, MHands.Left) || {
                // Искомая карточка -- следующая (справа)?
                __tryFirst( fState0.fadsAfterCurrentIter.toStream.headOption, MHands.Right)
              } || {
                // Искомая карточка -- где-то в другом месте или отсутствует. Проще всего будет перезагрузить foc-выдачу в новое место.
                // TODO Если карточка уже есть в fads, то отобразить её без запроса к серверу. Нужно отдельную ветку ||.
                _stateData = sd0.copy(
                  focused = Some( fStateNext )
                )
                // Очищение текущей карусели произойдёт внутри Starting-состояния.
                become( _startFocusOnAdState )
                true
              }
          }
      }
    }

  }


  /** Общий код обновлялки focused-состояния при получении с сервера какого-то сегмента focused-выдачи. */
  protected trait FocUpdaterT {

    /** Начальное focused-состояние. */
    val fState0: MFocSd
    /** Результат запроса к серверу. */
    val res: MFocSrvResp

    // Закинуть в кеш карточек полученные карточки, обновить граничные данные.
    def relAdId = res.reqArgs.adIdLookup

    // Дедубликация кода логгирования разных ситуаций.
    def logSuffix = " " + relAdId + " " + res.reqArgs.adsLookupMode + " [" + fState0.fadIdsIter.mkString(",") + "]"

    val respFadsSize        = res.resp.fads.size
    val respFadsLimit       = res.reqArgs.limit.getOrElse(ScConstants.Focused.SIDE_PRELOAD_MAX)
    def respFadsNotEnought  = respFadsLimit > respFadsSize

    /** Выявление отсутсвия опорной карточки в fState.fads.
      *
      * @return false: крайне невероятный сценарий, но он всё же отрабатывается.
      *         true: не должно быть такого, что карточки нет.
      */
    lazy val fadsMissingRelAdId = !fState0.fads.exists(_.madId == relAdId)

    /** Объединение новых карточек с уже имеющимися. */
    def fads2: FAdQueue

    // В языке англосаксов нет нормального слова "крайний", а last уже подразумевает последний. Используем utter для этого термина.
    /** Крайняя рекламная карточка в зависимости от контекста. */
    def utterFad(in: Seq[IFocAd]): Option[IFocAd]

    /** Код выявления id крайней карточки. Вызывается при оверрайде firstAdIdOpt или lastAdIdOpt.
      * Если маловато карточек в ответе, то надо определить id последней карточки.
      *
      * @return Some(node_id) достоверно выявлена крайняя карточка.
      *         None нет точных результатов.
      */
    def utterAdIdOpt: Option[String] = {
      if (respFadsNotEnought) {
        val fadsColl = /*if (respFadsSize > 0) {
          res.resp.fads
        } else {*/
          fads2
        //}
        // Вернуть id крайней карточки.
        for (fad <- utterFad(fadsColl)) yield {
          fad.madId
        }
      } else {
        None
      }
    }

    // оверрайдить в реализациях с помощью = utterAdIdOpt.
    /** Опциональное выявление id самой первой карточки. */
    def firstAdIdOpt: Option[String] = fState0.firstAdId
    /** Опциональное выявление id последней карточки. */
    def lastAdIdOpt : Option[String] = fState0.lastAdId

    /** Интерфейсный метод сборки и возврата обновлённого состояния. */
    def fState2(): MFocSd = {
      // Вернуть все результаты в focused-состоянии:
      fState0.copy(
        fads      = fads2,
        lastAdId  = lastAdIdOpt,
        firstAdId = firstAdIdOpt
      )
    }
  }
  /** Интерфейс объекта-компаньона обновлятеля focused-состояния.
    * Используется для интерфейсинга самого конструктора реализации FocUpdaterT. */
  trait IFocUpdaterCompanion {
    def apply(fState0: MFocSd, res: MFocSrvResp): FocUpdaterT
  }


  /** Реализация обновлятеля foc-состояния для сегмента After-карточек. */
  protected case class AfterFocUpdater(
    override val fState0  : MFocSd,
    override val res      : MFocSrvResp
  ) extends FocUpdaterT {

    override lazy val fads2: FAdQueue = {
      if (fState0.fads.lastOption.map(_.madId).contains(relAdId) || fadsMissingRelAdId) {
        // Если опорной карточки нет в fads (should never happen), то ругаться в логи, но добавлять карточки в конец в исходном порядке.
        // TODO Тут косяк есть: выдача не забывает о потерявшейся карточке.
        if (fadsMissingRelAdId)
          LOG.warn( ErrorMsgs.FOC_LOOKUP_MISSING_AD, msg = logSuffix )
        fState0.fads ++ res.resp.fads

      } else {
        // Маловозможна ситуация запроса карточек после указанной, хотя там карточки почему-то уже есть.
        LOG.warn( WarnMsgs.FOC_LOOKUPED_AD_NOT_LAST, msg = logSuffix )
        // Нужно скопировать в новую коллекции все элементы, включая текущую карточку.
        var flag = false
        fState0.fads
          .iterator
          .takeWhile { fad =>
            val res = flag
            flag = fad.madId == relAdId
            res
          }
          .++(res.resp.fads)
          .toSeq
      }
    }

    override def utterFad(in: Seq[IFocAd]): Option[IFocAd] = {
      in.lastOption
    }

    override def lastAdIdOpt = utterAdIdOpt
  }
  object AfterFocUpdater extends IFocUpdaterCompanion


  /** Обновлятель foc-состояния для before-сегмента карточек. */
  protected case class BeforeFocUpdater(
    override val fState0  : MFocSd,
    override val res      : MFocSrvResp
  ) extends FocUpdaterT {

    override lazy val fads2: FAdQueue = {
      if (fState0.fads.isEmpty) {
        // Should never happen: внезапно пустая очередь-кеш fads. Код написан для безопасности вызова оптимизированного fads.head
        LOG.error( ErrorMsgs.FOC_ADS_EMPTY, msg = logSuffix )
        fState0.fads
      } else if (fState0.fads.head.madId == relAdId || fadsMissingRelAdId) {
        // Если первая карточка -- искомая, то запихиваем всё по-упрощенке
        // Если нет карточки, вокруг которой пляшет весь запрос, то просто запихать карточки в выдачу, ругнувшись в логи.
        if (fadsMissingRelAdId)
          LOG.warn( ErrorMsgs.FOC_LOOKUP_MISSING_AD, msg = logSuffix )
        res.resp.fads ++: fState0.fads

      } else {
        LOG.warn( WarnMsgs.FOC_LOOKUPED_AD_NOT_LAST, msg = logSuffix )
        // Карточка есть, но она не первая почему-то. Используем iterator + dropWhile.
        res.resp.fads
          .iterator
          .++ {
            fState0.fads
              .iterator
              .dropWhile(_.madId != relAdId)
          }
          .toSeq
      }
    }

    override def utterFad(in: Seq[IFocAd]): Option[IFocAd] = {
      in.headOption
    }

    override def firstAdIdOpt = utterAdIdOpt
  }
  object BeforeFocUpdater extends IFocUpdaterCompanion

}


/** Аддон для [[io.suggest.sc.sjs.c.scfsm.ScFsm]] с трейт-реализацией состояния
  * спокойного нахождения в focused-выдаче. */
trait OnFocus extends OnFocusBase {

  /** Состояние нахождения в фокусе одной карточки.
    * Помимо обработки сигналов это состояние готовит соседние карточки к отображению. */
  protected trait OnFocusStateT extends OnFocusStateBaseT {

    override def afterBecome(): Unit = {
      super.afterBecome()

      val sd0 = _stateData
      for (fState <- sd0.focused) {

        // Отработать карусель
        for (car <- FCarCont.find()) {
          // Сначала удалить из vm карусели карточки, находящиеся дальше от текущей чем 1 шаг.
          // Для этого строим список допущенных id карточек, которые там ещё МОГУТ оставаться.
          // Не заворачиваем в Set, т.к. множества с length <= 4 внутри ближе к Seq по смыслу.
          val inCarKnownIds = IMadId.nearIds(fState.current.madId, fState.fads, 1)

          // Чистим текущие car.children по списку допущенных.
          for (fadRoot <- car.fadRootsIter) {
            // Если fadRoot.madId нет в списке допущенных -- удаляем.
            if (!fadRoot.madId.exists(inCarKnownIds.contains))
              fadRoot.remove()
          }

          // Запустить анализ текущих карточек: вдруг что-то подгрузить надо в фоне.
          _analyzeCurrentFads(car)
        } // for {} {...}

        // Нельзя обновлять состояние в случае принудительно фокусировки.
        State2Url.pushCurrState()

      }     // fState
    }       // afterBecome()

  }         // FSM state trait

}
