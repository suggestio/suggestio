package util.adv.ut

import models.MAd
import models.blk.OneAdQsArgs
import models.event.ErrorInfo
import util.PlayMacroLogsI
import util.async.FsmActor
import util.event.EventTypes
import util.img.AdRenderUtil
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 13:35
 * Description: Заготовка (трейт) состояния асинхронного рендера карточки в картинку.
 */
trait RenderAd2Img extends FsmActor with PlayMacroLogsI {
  
  /** Заготовка fsm-состояния рендера карточки в картинку. */
  trait RenderAd2ImgStateT extends FsmState {

    def _mad: MAd
    def _adRenderArgs: OneAdQsArgs

    /** Действия, если карточка в картинку отрендерилась на отличненько. */
    def handleImgOk(okRes: Ad2ImgRenderOk): Unit

    def rendererError(ex: Throwable): Unit

    /** При переключении на состояние надо запустить в фоне рендер карточки. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить в фоне генерацию картинки и отправку её на удалённый сервер.
      AdRenderUtil.renderAd2img(_adRenderArgs, _mad)
        .onComplete {
          case Success(imgBytes)  =>  self ! new Ad2ImgRenderOk(imgBytes)
          case result             =>  self ! result
        }
    }

    /** Дождаться завершения отправки картинки на удалённый сервер... */
    override def receiverPart: Receive = {
      // Картинка готова. Можно собирать запрос.
      case imgReady: Ad2ImgRenderOk =>
        handleImgOk(imgReady)

      // Не удалось отрендерить карточку в картинку.
      case Failure(ex) =>
        rendererError(ex)
    }

    class Ad2ImgRenderOk(val imgBytes: Array[Byte])
  }

}


/** Частичная реализация [[RenderAd2Img]], которая рисует ошибку на экране юзера. */
trait RenderAd2ImgRender extends RenderAd2Img with ExtTargetActorUtil {
  trait RenderAd2ImgStateT extends super.RenderAd2ImgStateT {
    override def rendererError(ex: Throwable): Unit = {
      val err = ErrorInfo(
        msg  = "error.sio.internal",
        info = Some(s"[$replyTo] ${ex.getMessage}")
      )
      val rargs = evtRenderArgs(EventTypes.AdvExtTgError, err)
      renderEventReplace(rargs)
    }
  }
}

