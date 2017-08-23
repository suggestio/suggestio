package util.adv.ext.ut

import java.io.File

import io.suggest.fsm.FsmActor
import io.suggest.model.n2.node.MNode
import io.suggest.primo.IToPublicString
import io.suggest.util.logs.IMacroLogs
import models.blk.OneAdQsArgs
import models.event.{ErrorInfo, MEventTypes}
import models.mproj.IMCommonDi
import util.adr.IAdRenderUtilDi

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 13:35
 * Description: Заготовка (трейт) состояния асинхронного рендера карточки в картинку.
 */
trait RenderAd2Img
  extends FsmActor
  with IMacroLogs
  with IAdRenderUtilDi
  with IMCommonDi
{

  import mCommonDi.ec
  
  /** Заготовка fsm-состояния рендера карточки в картинку. */
  trait RenderAd2ImgStateT extends FsmState {

    def _mad: MNode
    def _adRenderArgs: OneAdQsArgs

    /** Действия, если карточка в картинку отрендерилась на отличненько. */
    def handleImgOk(okRes: Ad2ImgRenderOk): Unit

    def rendererError(ex: Throwable): Unit

    /** При переключении на состояние надо запустить в фоне рендер карточки. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить в фоне генерацию картинки и отправку её на удалённый сервер.
      adRenderUtil.renderAd2img(_adRenderArgs, _mad)
        .onComplete {
          case Success(file)      =>  self ! new Ad2ImgRenderOk(file)
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

    class Ad2ImgRenderOk(val imgFile: File)
  }

}


/** Частичная реализация [[RenderAd2Img]], которая рисует ошибку на экране юзера. */
trait RenderAd2ImgRender
  extends RenderAd2Img
  with AdvExtTargetActorUtil
{

  trait RenderAd2ImgStateT extends super.RenderAd2ImgStateT {
    override def rendererError(ex: Throwable): Unit = {
      val err = ErrorInfo(
        msg  = "error.sio.internal",
        info = Some(s"[$replyTo] ${IToPublicString.getPublicString(ex)}")
      )
      val rargs = evtRenderArgs(MEventTypes.AdvExtTgError, err)
      renderEventReplace(rargs)
    }
  }

}

