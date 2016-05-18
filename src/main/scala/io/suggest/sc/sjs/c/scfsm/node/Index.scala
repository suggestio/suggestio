package io.suggest.sc.sjs.c.scfsm.node

import io.suggest.sc.sjs.c.mapbox.MbFsm
import io.suggest.sc.sjs.c.scfsm.{FindAdsUtil, ScFsmStub}
import io.suggest.sc.sjs.m.mgeo.IMGeoMode
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.mmap.EnsureMap
import io.suggest.sc.sjs.m.msrv.index.{MNodeIndex, MScIndexArgs}
import io.suggest.sc.sjs.vm.layout.LayRootVm
import io.suggest.sc.sjs.vm.nav.nodelist.NlRoot
import io.suggest.sc.sjs.vm.res.CommonRes
import io.suggest.sc.sjs.vm.{SafeBody, SafeWnd}
import io.suggest.sjs.common.msg.ErrorMsgs

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.{Failure, Success, Try}

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
        adnIdOpt  = sd0.common.adnIdOpt,
        geoMode   = Some( IMGeoMode(sd0.geo.lastGeoLoc) ),
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

        val sd1 = sd0.copy(
          grid = sd0.grid.copy(
            state = sd0.grid.state.copy(
              adsPerLoad = MGridState.getAdsPerLoad( screen )
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
    /** Реакция на получение node index. */
    protected def _nodeIndexReceived(v: MNodeIndex): Unit
  }
  trait IGetNodeIndexFailed {
    /** Реакция на ошибку выполнения запроса к node index. */
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
      val sd0 = _stateData
      if (sd0.common.adnIdOpt != v.adnIdOpt) {
        val _sd1 = sd0.copy(
          common = sd0.common.copy(
            adnIdOpt = v.adnIdOpt
          )
        )
        _stateData = _sd1
      }

      val sd1: SD = _stateData

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
      body.setOverflowHidden()

      // Инициализация верстки welcome-карточки перенесена отсюда в отдельное состояние, см. Welcome

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

      for (lc <- layout.content) {
        for (scr <- _stateData.screen) {
          lc.setWndClass(scr)
        }
        // Инициализация кнопок заголовка. Раньше было тут HeaderCtl.initLayout().
        for (hdr <- lc.header) {
          hdr.initLayout()
        }
      }

      // Очистить подложку фона выдачи.
      layout.eraseBg()
      body.eraseBg()

      _sendFutResBack(findAdsFut)

      become( _nodeInitWelcomeState )

      // Запустить в фоне ensure'инг карты
      MbFsm ! EnsureMap()
    }

    /** Следующее состояние. */
    protected def _nodeInitWelcomeState: FsmState

  }


  /** Трейт для состояния ожидания получения index с сервера. */
  trait WaitIndexStateT
    extends FsmEmptyReceiverState
      with IGetNodeIndexFailed
      with ProcessIndexReceivedUtil
  {

    override def receiverPart: Receive = super.receiverPart orElse {
      case mni: MNodeIndex =>
        _nodeIndexReceived(mni)
      case Failure(ex) =>
        _getNodeIndexFailed(ex)
    }

    override protected def _getNodeIndexFailed(ex: Throwable): Unit = {
      error(ErrorMsgs.GET_NODE_INDEX_FAILED + " " + _stateData.common.adnIdOpt, ex)
      _retry(50)( _onNodeIndexFailedState )
    }

    /** На какое состояние переключаться, когда нет возможности получить index-страницу узла. */
    protected def _onNodeIndexFailedState: FsmState

  }

}
