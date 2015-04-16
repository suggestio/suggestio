package util.adv

import akka.actor.Props
import models.adv.ext.act.EtaCustomArgsBase
import models.adv.js.ctx.JsErrorInfo
import models.adv.IOAuth1AdvTargetActorArgs
import models.blk.OneAdQsArgs
import models.mext._
import play.api.libs.ws.WSResponse
import util.PlayMacroLogsImpl
import util.adv.ut.ExtTargetActorUtil
import util.async.FsmActor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import ut._

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 15:36
 * Description: Актор, занимающийся размещением карточек через уже готовую связь с удаленным сервером.
 * Для размещения используется access_token, полученный от service-актора.
 * Ссылки API берутся из service.oauth1Support.
 */

object OAuth1TargetActor {

  def props(args: IOAuth1AdvTargetActorArgs): Props = {
    Props(OAuth1TargetActor(args))
  }

}


case class OAuth1TargetActor(args: IOAuth1AdvTargetActorArgs)
  extends FsmActor
  with ExtTargetActorUtil
  with ReplyTo
  with MediatorSendCommand
  with PlayMacroLogsImpl
  with CompatWsClient    // TODO
  with RenderAd2ImgRender
  with S2sMpUploadRender
  with EtaCustomArgsBase
{
  import LOGGER._

  /** Общий ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty

  override protected var _state: FsmState = new DummyState

  override def receive: Receive = allStatesReceiver

  val oa1Support = service.oauth1Support.get


  /** Текущий сервис, в котором задействован текущий актор. */
  override def service: MExtService = args.target.target.service

  /** Запуск актора. Выставить исходное состояние. */
  override def preStart(): Unit = {
    super.preStart()
    renderInProcess()
    val nextState = if (oa1Support.isMkPostNeedMpUpload) {
      new RenderAd2ImgState
    } else {
      new PublishState()
    }
    become(nextState)
  }


  /** Состояние рендера текущей карточки в картинку. */
  class RenderAd2ImgState extends RenderAd2ImgStateT {
    /** Аргументы для генерации данных для ссылки рендера карточки. */
    val _ca = new MCustomArgsT {
      override lazy val madRenderInfo = super.madRenderInfo
    }
    override def rendererError(ex: Throwable): Unit = {
      super.rendererError(ex)
      harakiri()
    }
    override def _mad = args.request.mad
    override def _adRenderArgs: OneAdQsArgs = _ca.adRenderArgs
    override def handleImgOk(okRes: Ad2ImgRenderOk): Unit = {
      become( new UploadRenderedMadState(okRes.imgBytes) )
    }
  }


  class UploadRenderedMadState(imgBytes: Array[Byte]) extends S2sMpUploadStateT {
    /** Ссылка, которая была использована для аплоада. */
    override def upUrl: String = args.target.target.url

    /** Формирование данных для сборки тела multipart. */
    override def mkUpArgs: IMpUploadArgs = {
      mpUploadClient.uploadArgsSimple(
        data      = imgBytes,
        ct        = service.imgFmt.mime,
        url       = None,
        fileName  = ad2imgFileName,
        oa1AcTok  = Some(args.accessToken)
      )
    }

    /** Аплоад точно удался. */
    override def uploadedOk(wsResp: WSResponse): Unit = {
      trace("Img uploaded to service ok, resp = " + wsResp.body)
      val atts = mpUploadClient.resp2attachments(wsResp)
      become( new PublishState(atts) )
    }

  }


  /** Состояние публикации одного поста. */
  class PublishState(_attachments: Seq[IPostAttachmentId] = Nil) extends FsmState {
    /** При входе в состояние надо запустить постинг с помощью имеющегося access_token'а. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      val mkPostArgs = new IOa1MkPostArgs {
        override def mad      = args.request.mad
        override def acTok    = args.accessToken
        override def target   = args.target.target
        override def mnode    = args.request.producer
        override def returnTo = args.target.returnTo
        override def geo      = args.request.producer.geo.point
        override def attachments = _attachments
      }
      val mkPostFut = oa1Support.mkPost(mkPostArgs)
      mkPostFut onComplete {
        case Success(res) => self ! res
        case other        => self ! other
      }
    }

    override def receiverPart: Receive = {
      // Всё ок
      case newPostInfo: IExtPostInfo =>
        // TODO Нужно ссылку на пост передать в рендер
        renderSuccess()
        trace("POSTed successfully, info = " + newPostInfo)
        harakiri()

      // Ошибка постинга.
      case Failure(ex) =>
        val jsErr = JsErrorInfo(s"${ex.getClass.getSimpleName}: ${ex.getMessage}")
        renderError("e.adv.ext.api", Some(jsErr))
        error(s"POST failed for target[${args.target.target.idOrNull}] ${args.target.target.url}", ex)
        harakiri()
    }
  }

}
