package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.c.NodeWelcomeCtl
import io.suggest.sc.sjs.m.mfsm.signals.WelcomeDisplayTimeout
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sc.sjs.m.msrv.index.{MNodeIndex, MScIndexArgs}
import io.suggest.sc.sjs.vm.res.CommonRes
import io.suggest.sc.sjs.vm.layout.LayRootVm
import io.suggest.sc.sjs.vm.nav.nodelist.NlRoot
import io.suggest.sc.sjs.vm.{SafeBody, SafeWnd}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.concurrent.JSExecutionContext.queue
import scala.util.Failure

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.06.15 14:19
 * Description: Аддон центрального FSM, добавляющий трейты для сборки состояний инициализации выдачи узла.
 */
trait GetIndex extends ScFsmStub with FindAdsUtil {

  /** Реализация состояния активации и  */
  protected trait GetIndexStateT extends FsmState {

    /** Дожидаясь ответа сервера, инициализировать кое-какие переменные, необходимые на следующем шаге. */
    protected def _initStateData(sd: SD): SD = {
      sd.screen.fold(sd) { screen =>
        val adsPerLoad = MGridState.getAdsPerLoad( screen )
        sd.copy(
          grid = sd.grid.copy(
            state = sd.grid.state.copy(
              adsPerLoad = adsPerLoad
            )
          )
        )
      }
    }

    /** После активации state нужно запросить index с сервера и ожидать получения. */
    override def afterBecome(): Unit = {
      super.afterBecome()

      val sd0 = _stateData

      val inxArgs = MScIndexArgs(
        adnIdOpt  = sd0.adnIdOpt,
        geoMode   = Some( sd0.currGeoMode ),
        screen    = sd0.screen
      )
      val inxFut = MNodeIndex.getIndex(inxArgs)

      // Дожидаясь ответа сервера, инициализировать кое-какие переменные, необходимые на следующем шаге.
      _stateData = _initStateData(sd0)
      _sendFutResBack(inxFut)
    }

    override def receiverPart: Receive = {
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

      // Выставить верстку index в новый layout.
      for (layContent <- layout.content) {
        layContent.setContent(v.html)
      }
      layout.insertIntoDom()
      SafeWnd.scrollTop()

      // Контейнер ресурсов должен быть создан
      CommonRes.ensureCreated()

      val body = SafeBody
      body.overflowHidden()

      // Инициализация welcomeAd.
      // TODO Раскидать логику этой инициализации по welcome vm'кам.
      val wcHideFut = NodeWelcomeCtl.handleWelcome()

      // Инициализация верстки grid'а:
      for (layContent <- layout.content;  groot <- layContent.grid) {
        groot.initLayout(sd1)
      }

      // Инициализация панели навигации.
      for (nlRoot <- NlRoot.find()) {
        nlRoot.initLayout(sd1)
      }

      // Очищаем подложку фона выдачи.
      wcHideFut onComplete { case _ =>
        layout.eraseBg()
        body.eraseBg()
      }

      // В порядке очереди запустить инициализацию панели поиска.
      wcHideFut.onComplete { case _ =>
        for (layContent <- layout.content) {
          for (sroot <- layContent.searchPanel) {
            sroot.initLayout(sd1)
          }
          for (froot <- layContent.focused) {
            froot.initLayout()
          }
        }
      }(queue)

      for (scr <- _stateData.screen;  layContent <- layout.content) {
        layContent.setWndClass(scr)
      }

      // Инициализация кнопок заголовка. Раньше было тут HeaderCtl.initLayout().
      for (lc <- layout.content; hdr <- lc.header) {
        hdr.initLayout()
      }

      // Переключаемся на следующее состояния (плитка), в трейте это состояние абстрактно.
      val nextState = _onSuccessNextState(findAdsFut, wcHideFut, sd1)
      become( nextState, sd1 )
    }


    /** На какое состояние переходить, когда инициализация index завершена. */
    protected def _onSuccessNextState(findAdsFut: Future[MFindAds], wcHideFut: Future[_], sd: SD): FsmState

    /** Запрос за index'ом не удался. */
    protected def _getNodeIndexFailed(ex: Throwable): Unit
  }


  /** Трейт для сборки состояний нахождения на welcome-карточке. */
  trait OnWelcomeStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Приём сигнала от таймера о необходимости начать сокрытие карточки приветствия.
      case WelcomeDisplayTimeout =>
        _letsHideWelcome()
    }

    def _letsHideWelcome(): Unit = {
      ???
    }
  }

}
