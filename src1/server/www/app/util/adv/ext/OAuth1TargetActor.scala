package util.adv.ext

import java.io.File

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import io.suggest.ahc.upload.IMpUploadArgs
import io.suggest.fsm.FsmActor
import io.suggest.util.logs.MacroLogsImpl
import models.adv.IOAuth1AdvTargetActorArgs
import models.adv.ext.act.EtaCustomArgsBase
import models.adv.js.ctx.JsErrorInfo
import models.blk.OneAdQsArgs
import models.mctx.ContextUtil
import models.mext._
import models.mproj.ICommonDi
import play.api.libs.ws.{WSClient, WSResponse}
import util.adr.AdRenderUtil
import util.adv.ext.ut._
import util.ext.ExtServicesUtil
import util.n2u.N2NodesUtil

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 15:36
 * Description: Актор, занимающийся размещением карточек через уже готовую связь с удаленным сервером.
 * Для размещения используется access_token, полученный от service-актора.
 * Ссылки API берутся из service.oauth1Support.
 */

/** Интерфейс для Guice DI factory для сборки инжектируемых инстансов актора [[OAuth1TargetActor]]. */
trait OAuth1TargetActorFactory {
  /** Вернуть инстанс класса актора для указанных аргументов. */
  def apply(args: IOAuth1AdvTargetActorArgs): OAuth1TargetActor
}


class OAuth1TargetActor @Inject() (
                                    @Assisted override val args   : IOAuth1AdvTargetActorArgs,
                                    override val n2NodesUtil      : N2NodesUtil,
                                    override val advExtFormUtil       : AdvExtFormUtil,
                                    override val adRenderUtil     : AdRenderUtil,
                                    override val extServicesUtil  : ExtServicesUtil,
                                    override val ctxUtil          : ContextUtil,
                                    implicit val wsClient         : WSClient,
                                    override val mCommonDi        : ICommonDi
)
  extends FsmActor
  with AdvExtTargetActorUtil
  with ReplyTo
  with MediatorSendCommand
  with MacroLogsImpl
  with RenderAd2ImgRender
  with S2sMpUploadRender
  with EtaCustomArgsBase
{

  import LOGGER._
  import mCommonDi.ec

  override type State_t = FsmState

  override protected var _state: FsmState = new DummyState

  override def receive: Receive = allStatesReceiver

  val oa1Support = serviceHelper.oauth1Support.get


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
    val _ca = new MCustomArgs {
      override lazy val madRenderInfo = super.madRenderInfo
    }
    override def rendererError(ex: Throwable): Unit = {
      super.rendererError(ex)
      harakiri()
    }
    override def _mad = args.request.mad
    override def _adRenderArgs: OneAdQsArgs = _ca.adRenderArgs
    override def handleImgOk(okRes: Ad2ImgRenderOk): Unit = {
      become( new UploadRenderedMadState(okRes.imgFile) )
    }
  }


  class UploadRenderedMadState(imgFile: File) extends S2sMpUploadStateT {
    /** Ссылка, которая была использована для аплоада. */
    override def upUrl: String = args.target.target.url

    /** Формирование данных для сборки тела multipart. */
    override def mkUpArgs: IMpUploadArgs = {
      mpUploadClient.uploadArgsSimple(
        file      = imgFile,
        ct        = serviceHelper.imgFmt.mime,
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
      val mkPostArgs = new IOAuth1MkPostArgs {
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
