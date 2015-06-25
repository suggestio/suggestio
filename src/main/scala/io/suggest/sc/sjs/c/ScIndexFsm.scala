package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.{FindAdsFsmUtil, ScFsmStub}
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sc.sjs.m.msrv.index.{MScIndexArgs, MNodeIndex}
import io.suggest.sc.sjs.v.res.{FocusedRes, CommonRes}
import io.suggest.sc.sjs.vm.{SafeBody, SafeWnd}
import io.suggest.sc.sjs.vm.layout.LayRootVm

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.concurrent.JSExecutionContext.queue
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.06.15 14:19
 * Description: Аддон центрального FSM, добавляющий состояния для получения и загрузки index'а выдачи.
 */
trait ScIndexFsm extends ScFsmStub with FindAdsFsmUtil {

  /** Реализация состояния активации и  */
  protected trait GetIndexStateT extends FsmState {

    /** id узла, на который переключаемся, или None.
      * @return None когда сервер должен определить новый узел. */
    def adnIdOpt: Option[String]

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

      val sd = _stateData

      val inxArgs = MScIndexArgs(
        adnIdOpt  = adnIdOpt,
        geoMode   = Some( sd.currGeoMode ),
        screen    = sd.screen
      )
      val inxFut = MNodeIndex.getIndex(inxArgs)

      // Дожидаясь ответа сервера, инициализировать кое-какие переменные, необходимые на следующем шаге.
      _stateData = _initStateData(sd)

      inxFut onComplete { case res =>
        val evt = res match {
          case Success(v) => IndexOk(v)
          case failure    => failure
        }
        _sendEventSync(evt)
      }
    }


    /** Кусок ресивера для текущего состояния. */
    override def receiverPart: PartialFunction[Any, Unit] = {
      case IndexOk(v) =>
        _onSuccess(v)
      case Failure(ex) =>
        error("Failed to get node index: " + adnIdOpt, ex)
        _onFailure(ex)
    }


    /** Контейнер для передачи в receiver корректного результата index-запроса. */
    protected case class IndexOk(v: MNodeIndex)


    /** Успешный запрос index'а. */
    protected def _onSuccess(v: MNodeIndex): Unit = {
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
      CommonRes.recreate()
      FocusedRes.recreate()

      val layout = LayRootVm.createNew()

      // Выставить верстку index в новый layout.
      for (layContent <- layout.content) {
        layContent.setIndexHtml(v.html)
      }
      layout.insertIntoDom()
      SafeWnd.scrollTop()

      val body = SafeBody
      body.overflowHidden()

      // Инициализация welcomeAd.
      val wcHideFut = NodeWelcomeCtl.handleWelcome()

      // TODO Раскидать логику этой инициализации по vm'кам.
      GridCtl.initNewLayout(wcHideFut, sd1.screen.get, sd1.grid.params)

      NavPanelCtl.initNav()

      // Очищаем подложку фона выдачи.
      wcHideFut onComplete { case _ =>
        layout.eraseBg()
        body.eraseBg()
      }

      // В порядке очереди запустить инициализацию панели поиска.
      wcHideFut.onComplete { case _ =>
        SearchPanelCtl.initNodeLayout()
      }(queue)

      for {
        scr         <- _stateData.screen
        layContent  <- layout.content
      } {
        layContent.setWndClass(scr)
      }

      HeaderCtl.initLayout()

      // Переключаемся на следующее состояния (плитка), в трейте это состояние абстрактно.
      become( _onSuccessNextState(findAdsFut, wcHideFut, sd1), sd1 )
    }


    /** На какое состояние переходить, когда инициализация index завершена. */
    protected def _onSuccessNextState(findAdsFut: Future[MFindAds], wcHideFut: Future[_], sd: SD): FsmState

    /** Запрос за index'ом не удался. */
    protected def _onFailure(ex: Throwable): Unit
  }

}
