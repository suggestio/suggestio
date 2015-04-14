package util.adv.ut

import models.MAd
import models.blk.OneAdQsArgs
import util.PlayMacroLogsI
import util.async.FsmActor
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

    /** На какое состояние переключаться надо, когда картинка отрендерилась? */
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
