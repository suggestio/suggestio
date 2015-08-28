package io.suggest.sc.sjs.c.scfsm.node

import io.suggest.sc.ScConstants.Welcome
import io.suggest.sc.sjs.c.scfsm.{FindAdsUtil, ScFsmStub}
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.msrv.index.{MNodeIndex, MScIndexArgs}
import io.suggest.sc.sjs.m.mwc.WcTimeout
import io.suggest.sc.sjs.vm.layout.LayRootVm
import io.suggest.sc.sjs.vm.nav.nodelist.NlRoot
import io.suggest.sc.sjs.vm.res.CommonRes
import io.suggest.sc.sjs.vm.{SafeBody, SafeWnd}
import io.suggest.sjs.common.msg.ErrorMsgs
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.{Success, Try, Failure}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.06.15 14:19
 * Description: Аддон центрального FSM, добавляющий трейты для сборки состояний инициализации выдачи узла.
 */
trait Index extends ScFsmStub with FindAdsUtil {

  /** Утиль для запуска запроса sc index с сервера. */
  trait GetIndexUtil {

    /** Запустить запрос получения индекса с сервера на основе переданного состояния. */
    protected def _getIndex(sd0: SD = _stateData): Future[MNodeIndex] = {
      val inxArgs = MScIndexArgs(
        adnIdOpt  = sd0.adnIdOpt,
        geoMode   = Some( sd0.geo.currGeoMode ),
        screen    = sd0.screen
      )
      MNodeIndex.getIndex(inxArgs)
    }

  }

  /** Реализация состояния активации и  */
  protected trait GetIndexStateT extends FsmState with GetIndexUtil {

    /** Аддон для запуска запроса index с сервера. */
    override def afterBecome(): Unit = {
      super.afterBecome()

      val sd0 = _stateData
      val inxFut = _getIndex(sd0)

      // Дожидаясь ответа сервера, инициализировать кое-какие переменные, необходимые на следующем шаге.
      for (screen <- sd0.screen) {
        val adsPerLoad = MGridState.getAdsPerLoad( screen )
        val sd1 = sd0.copy(
          grid = sd0.grid.copy(
            state = sd0.grid.state.copy(
              adsPerLoad = adsPerLoad
            )
          )
        )
        _stateData = sd1
      }

      _sendFutResBack(inxFut)
    }
  }


  /** Общий интерфейс для метода-реакции на получение node-индекса. */
  trait INodeIndexReceived {
    protected def _nodeIndexReceived(v: MNodeIndex): Unit
  }
  trait IGetNodeIndexFailed {
    protected def _getNodeIndexFailed(ex: Throwable): Unit
  }
  trait HandleNodeIndex extends INodeIndexReceived with IGetNodeIndexFailed {
    def _handleNodeIndexResult(tryRes: Try[MNodeIndex]): Unit = {
      tryRes match {
        case Success(mni) => _nodeIndexReceived(mni)
        case Failure(ex)  => _getNodeIndexFailed(ex)
      }
    }
  }

  /** Статическая утиль для состояний, обрабатывающих получаемые от сервера node index. */
  trait ProcessIndexReceivedUtil extends FsmState with INodeIndexReceived {

    /** Реакция на успешный результат запроса node index. */
    override protected def _nodeIndexReceived(v: MNodeIndex): Unit = {
      // Заливаем в данные состояния полученные метаданные по текущему узлу.
      // TODO Это нужно только на первом шаге по факту (geo). Потом adnId обычно известен наперёд.
      val sd1: SD = {
        val sd0 = _stateData
        if (sd0.adnIdOpt != v.adnIdOpt) {
          sd0.copy(
            adnIdOpt = v.adnIdOpt
          )
        } else {
          sd0
        }
      }

      // Начинаем запрос карточек как можно скорее, чтобы распараллелить деятельность.
      val findAdsFut = _findAds(sd1)

      // TODO Выставить новый заголовок окна

      // Стереть старый layout, создать новый. Кешируем
      for (oldLayRoot <- LayRootVm.find() ) {
        oldLayRoot.remove()
      }

      val layout = LayRootVm.createNew()
      val lcOpt = layout.content

      // Выставить верстку index в новый layout.
      for (layContent <- lcOpt) {
        layContent.setContent(v.html)
      }
      layout.insertIntoDom()
      SafeWnd.scrollTop()

      // Контейнер ресурсов должен быть создан
      CommonRes.ensureCreated()

      val body = SafeBody
      body.overflowHidden()

      // Инициализация верстки welcome-карточки, если есть:
      val wcHideTimerOpt = for {
        lc      <- lcOpt
        wcRoot  <- lc.welcome
        screen  <- sd1.screen
      } yield {
        // Подготовить отображение карточки.
        wcRoot.initLayout(screen)
        wcRoot.willAnimate()
        // Запустить таймер сокрытия
        dom.setTimeout(
          { () => _sendEventSyncSafe( WcTimeout ) },
          Welcome.HIDE_TIMEOUT_MS
        )
      }

      for (lc <- lcOpt;  groot <- lc.grid) {
        groot.initLayout(sd1)
      }

      // Инициализация панели навигации.
      for (nlRoot <- NlRoot.find()) {
        nlRoot.initLayout(sd1)
      }

      for (layContent <- lcOpt) {
        for (sroot <- layContent.searchPanel) {
          sroot.initLayout(sd1)
        }
        for (froot <- layContent.focused) {
          froot.initLayout()
        }
      }

      for (scr <- _stateData.screen;  layContent <- layout.content) {
        layContent.setWndClass(scr)
      }

      // Инициализация кнопок заголовка. Раньше было тут HeaderCtl.initLayout().
      for (lc <- layout.content; hdr <- lc.header) {
        hdr.initLayout()
      }

      // Очистить подложку фона выдачи.
      layout.eraseBg()
      body.eraseBg()

      _sendFutResBack(findAdsFut)

      // Переключаемся на следующее состояния (плитка), в трейте это состояние абстрактно.
      val nextState: FsmState = {
        if (wcHideTimerOpt.isDefined) {
          _welcomeAndWaitGridAdsState
        } else {
          _waitGridAdsState
        }
      }
      val sd2 = sd1.copy(
        timerId = wcHideTimerOpt
      )
      become( nextState, sd2 )
    }


    /** На какое состояние переходить, когда инициализация index завершена, и мы находимся на welcome,
      * ожидая grid ads. */
    protected def _welcomeAndWaitGridAdsState: FsmState

    /** Welcome-карточка отсутствует. На какое состояние переходить для просто ожидания grid-ads от сервера. */
    protected def _waitGridAdsState: FsmState

  }


  /** Поддержка реакции на получение MNodeIndex без конкретной логики обработки. */
  trait WaitIndexStateBaseT extends FsmEmptyReceiverState with INodeIndexReceived with IGetNodeIndexFailed {

    override def receiverPart: Receive = super.receiverPart orElse {
      case mni: MNodeIndex =>
        _nodeIndexReceived(mni)
      case Failure(ex) =>
        error("Failed to get node index: " + _stateData.adnIdOpt, ex)
        _getNodeIndexFailed(ex)
    }

  }


  /** Трейт для состояния ожидания получения index с сервера. */
  trait WaitIndexStateT extends WaitIndexStateBaseT with ProcessIndexReceivedUtil {

    override protected def _getNodeIndexFailed(ex: Throwable): Unit = {
      error(ErrorMsgs.GET_NODE_INDEX_FAILED, ex)
      _retry(50)( _onNodeIndexFailedState )
    }

    /** На какое состояние переключаться, когда нет возможности получить index-страницу узла. */
    protected def _onNodeIndexFailedState: FsmState

  }

}
