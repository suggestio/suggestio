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

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.Failure

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.06.15 14:19
 * Description: Аддон центрального FSM, добавляющий трейты для сборки состояний инициализации выдачи узла.
 */
trait Index extends ScFsmStub with FindAdsUtil {

  /** Реализация состояния активации и  */
  protected trait GetIndexStateT extends FsmState {

    /** Аддон для запуска запроса index с сервера. */
    override def afterBecome(): Unit = {
      super.afterBecome()

      val sd0 = _stateData

      val inxArgs = MScIndexArgs(
        adnIdOpt  = sd0.adnIdOpt,
        geoMode   = Some( sd0.geo.currGeoMode ),
        screen    = sd0.screen
      )
      val inxFut = MNodeIndex.getIndex(inxArgs)

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


  /** Ожидание получения index с сервера. */
  trait WaitIndexStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart orElse {
      case mni: MNodeIndex =>
        _nodeIndexReceived(mni)
      case Failure(ex) =>
        error("Failed to get node index: " + _stateData.adnIdOpt, ex)
        _getNodeIndexFailed(ex)
    }


    /** Реакция на успешный результат запроса node index. */
    protected def _nodeIndexReceived(v: MNodeIndex): Unit = {
      // Заливаем в данные состояния полученные метаданные по текущему узлу.
      val sd1 = _stateData.copy(
        adnIdOpt = v.adnIdOpt
      )

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
      become( nextState, sd1 )
    }


    /** На какое состояние переходить, когда инициализация index завершена, и мы находимся на welcome,
      * ожидая grid ads. */
    protected def _welcomeAndWaitGridAdsState: FsmState

    /** Welcome-карточка отсутствует. На какое состояние переходить для просто ожидания grid-ads от сервера. */
    protected def _waitGridAdsState: FsmState

    /** Запрос за index'ом не удался. */
    protected def _getNodeIndexFailed(ex: Throwable): Unit = {
      error(ErrorMsgs.GET_NODE_INDEX_FAILED, ex)
      _retry(50)( _onNodeIndexFailedState )
    }

    /** На какое состояние переключаться, когда нет возможности получить index-страницу узла. */
    protected def _onNodeIndexFailedState: FsmState

  }

}
