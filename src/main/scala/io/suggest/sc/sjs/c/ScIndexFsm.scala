package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.ScFsmStub
import io.suggest.sc.sjs.m.msrv.ads.find.{MFindAdsReqDflt, MFindAdsReqEmpty, MFindAds}
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
 * Description: Аддон центрального FSM, добавляющий абстрактное состояние для получения поддержки index'а выдачи.
 */
trait ScIndexFsm extends ScFsmStub {

  /** Реализация состояния активации и  */
  protected trait GetIndexStateT extends FsmState {

    /** id узла, на который переключаемся, или None.
      * @return None когда сервер должен определить новый узел. */
    def adnIdOpt: Option[String]


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

      // Сразу запускаем запрос к серверу за рекламными карточками.
      // Таким образом, под прикрытием welcome-карточки мы отфетчим и отрендерим плитку в фоне.
      val args = new MFindAdsReqEmpty with MFindAdsReqDflt {
        override def _mgs = _stateData.gridState
        override val _fsmState = super._fsmState
      }
      val findAdsFut = MFindAds.findAds(args)

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

      GridCtl.initNewLayout(wcHideFut)
      // Когда grid-контейнер инициализирован, можно рендерить полученные карточки.
      findAdsFut onSuccess { case resp =>
        // Анимацию размещения блоков можно отключить, если welcome-карточка закрывает собой всё это.
        val noWelcome = wcHideFut.isCompleted
        GridCtl.newAdsReceived(resp, isAdd = false, withAnim = noWelcome)
      }

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

      become( _onSuccessNextState(findAdsFut) )
    }


    /** На какое состояние переходить, когда инициализация index завершена. */
    protected def _onSuccessNextState(findAdsFut: Future[MFindAds]): FsmState


    /** Запрос за index'ом не удался. */
    protected def _onFailure(ex: Throwable): Unit
  }

}
