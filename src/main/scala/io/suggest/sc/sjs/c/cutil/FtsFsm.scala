package io.suggest.sc.sjs.c.cutil

import io.suggest.sc.sjs.c.GridCtl
import io.suggest.sc.sjs.m.msc.fsm.MScFsm
import io.suggest.sc.sjs.m.msearch.{MSearchDom, MFtsFsmState}
import io.suggest.sc.sjs.v.search.FtsFieldView
import io.suggest.sjs.common.util.ISjsLogger
import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLDivElement, HTMLInputElement}
import org.scalajs.dom.{KeyboardEvent, Event}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.06.15 10:34
 * Description: FSM для создания и обработки запросов полнотекстового поиска.
 */
trait FtsFsm extends ISjsLogger {

  /** Экземпляр состояния этого FSM. */
  private var _state: Option[MFtsFsmState] = None

  /** Нормализация текстового поискового запроса. */
  private def normalizeQ(q: String): String = {
    q.trim
  }

  /** Через сколько миллисекунд после окончания ввода запускать запрос. */
  protected def START_TIMEOUT_MS = 600

  /** Узнать input из события, если возможно. */
  private def getInput(e: Event): Option[HTMLInputElement] = {
    e.target match {
      case input: HTMLInputElement =>
        Some(input)
      case _ =>
        error("event not related to input")    // TODO Удалить после отладки
        getInput()
    }
  }
  private def getInput(): Option[HTMLInputElement] = {
    MSearchDom.ftsInput
  }

  private def initState(): MFtsFsmState = {
    val state = MFtsFsmState()
    _state = Some(state)
    state
  }

  /** Фокус попал в поле поиска. */
  def onFieldFocus(e: Event): Unit = {
    // При первой фокусировке надо активировать поле.
    if (_state.isEmpty) {
      initState()
      MScFsm.transformState() {
        _.copy(
          ftsSearch = Some("")
        )
      }
    }
  }


  /** Происходит набор текста внутри поля. */
  def onFieldKeyUp(e: KeyboardEvent): Unit = {
    for (input <- getInput(e)) {
      val q2 = input.value
      val q2Norm = normalizeQ( q2 )

      val state0 = _state getOrElse {
        warn("keyUp fsm: state was empty")
        onFieldFocus(e)
        _state.get
      }

      // Если изменился текст запроса...
      if (state0.q != q2Norm) {
        // то надо запустить таймера запуска поискового запроса, отменив предыдущий таймер.
        for (timerId <- state0.reqTimerId) {
          dom.clearTimeout(timerId)
        }
        val g2 = MFtsFsmState.getGeneration

        val newTimerId = dom.setTimeout(
          { () => startRequestTimerTimeout(g2) },
          START_TIMEOUT_MS
        )

        _state = Some(state0.copy(
          q           = q2Norm,
          reqTimerId  = Some(newTimerId),
          generation  = g2
        ))
      }
    }
  }


  /** Потеря фокуса в input-поле. */
  def onFieldBlur(e: Event): Unit = {
    val inputOpt = getInput(e)
    for (input <- inputOpt) {
      val q2 = input.value
      val q2Norm = normalizeQ(q2)
      if (q2Norm.isEmpty) {
        // Деактивация поиска, т.к. поисковое поле пустое.
        resetFts(inputOpt, resetInputValue = q2 != q2Norm)
      }
    }
  }

  def resetFts(inputOpt: Option[HTMLInputElement] = MSearchDom.ftsInput,
               inputContOpt: Option[HTMLDivElement] = MSearchDom.ftsInputContainerDiv,
               resetInputValue: Boolean = true): Unit = {
    if (_state.isDefined) {
      _state = None
      MScFsm.transformState() {
        _.copy(
          ftsSearch = None
        )
      }
      GridCtl.reFindAds()
      for (input <- inputOpt) {
        for (inputCont <- inputContOpt) {
          FtsFieldView.deactivateField( SafeEl(inputCont) )
        }
        if (resetInputValue) {
          FtsFieldView.setFtsFieldText(input, "")
        }
      }

    }
    // TODO Удалить возможное сообщение об ошибке в запросе.
  }


  protected def startRequestTimerTimeout(generation0: Long): Unit = {
    // Отфильтровать устаревшее, но поздно отмененное, события таймера.
    val stateOpt = _state
    if ( stateOpt.exists(_.generation == generation0) ) {
      val state = stateOpt.get
      // Начать перестройку состояния системы.
      MScFsm.transformStateReplace() {
        _.copy(
          ftsSearch = Some(state.q)
        )
      }
    }
  }

  /** Запуск поискового реквеста. */
  def startFindReq(): Unit = {
    val ftsGeneration = _state.get.generation
    val (mgs1, fut) = GridCtl.askNewAds()
    val fut2 = fut.filter { _ =>
      // Срезать ответ этого реквеста, если был получен более новый поисковый ответ до этого реквеста,
      // либо если поиск был отрублен во время исполнения реквеста.
      _state.nonEmpty && {
        val lrgenOpt = _state.get.lastRcvdGen
        lrgenOpt.isEmpty || lrgenOpt.get <= ftsGeneration
      }

    }.flatMap { mfa =>
      GridCtl.askNewAdsCallback(mgs1, fut)
    }

    fut2 onFailure {
      case nsee: NoSuchElementException =>
        log("Skip obsoleted req[" + ftsGeneration + "]")
      case ex: Throwable =>
        error("Failed find request", ex)
    }
  }

}
