package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.ScFsmStub
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sc.sjs.m.msrv.index.{MScIndexArgs, MNodeIndex}
import io.suggest.sc.sjs.v.layout.LayoutView

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
      val findAdsFut = GridCtl.askMoreAds()

      // TODO Выставить новый заголовок окна

      // Стереть старый layout, создать новый. Кешируем
      val l = LayoutView.redrawLayout()

      // Модифицировать текущее отображение под узел, отобразить welcome-карточку, если есть.
      LayoutView.showIndex(v.html, layoutDiv = l.layoutDiv, rootDiv = l.rootDiv)

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
        LayoutView.eraseBg(l.rootDiv)
      }

      // В порядке очереди запустить инициализацию панели поиска.
      wcHideFut.onComplete { case _ =>
        SearchPanelCtl.initNodeLayout()
      }(queue)

      LayoutView.setWndClass(l.layoutDiv)
      HeaderCtl.initLayout()

      become( _onSuccessNextState(findAdsFut) )
    }


    /** На какое состояние переходить, когда инициализация index завершена. */
    protected def _onSuccessNextState(findAdsFut: Future[MFindAds]): FsmState


    /** Запрос за index'ом не удался. */
    protected def _onFailure(ex: Throwable): Unit
  }

}
