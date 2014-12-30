package util.adv

import java.io.ByteArrayOutputStream

import akka.actor.{Props, ActorRef}
import models.adv.MExtServices.MExtService
import models.adv.{MExtAdvContext, MExtTarget}
import models.adv.js._
import models.blk.OneAdQsArgs
import models.im.OutImgFmts
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.content.ByteArrayBody
import org.apache.http.entity.mime.MultipartEntityBuilder
import play.api.http.HeaderNames
import play.api.libs.json.{JsString, JsObject}
import util.PlayMacroLogsImpl
import util.img.WkHtmlUtil
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.14 15:44
 * Description: Актор, обслуживающий один сервис внешнего размещения.
 */
object ExtServiceActor {
  def props(out: ActorRef, service: MExtService, targets0: List[MExtTarget], ctx1: JsObject, eactx: MExtAdvContext) = {
    Props(ExtServiceActor(out, service, targets0, ctx1, eactx))
  }
}


/** Актор, занимающийся загрузкой карточек в однин рекламный сервис. */
case class ExtServiceActor(
  out       : ActorRef,
  service   : MExtService,
  targets0  : List[MExtTarget],
  ctx1      : JsObject,
  eactx     : MExtAdvContext
)
  extends FsmActor with PlayMacroLogsImpl with SioPrJsUtil
{

  import LOGGER._

  /** Текущий контекст вызова. Выставляется в конструкторе актора и после инициализации клиента сервиса. */
  protected var _ctx = ctx1

  /** Параметры сервиса, присланные клиентом. */
  protected var _serviceParams: ServiceParams = null

  /** Текущее состояние FSM. */
  override protected var _state: super.FsmState = new DummyState

  override def receive: Receive = allStatesReceiver

  protected var imgBytesOpt: Option[Array[Byte]] = None

  /** Ресивер для всех состояний. */
  override val allStatesReceiver: Receive = PartialFunction.empty

  override def preStart(): Unit = {
    super.preStart()
    become(new EnsureServiceReadyState)
  }

  /** Команда к самозавершению. */
  protected def harakiri(): Unit = {
    trace("harakiri(). Last state was " + _state.name)
    context stop self
  }


  /** Трейт одного состояния. */
  sealed trait FsmState extends super.FsmState {
    override def name: String = service.strId + "/" + super.name
  }

  /** Состояние, когда запускается инициализация API одного сервиса. */
  class EnsureServiceReadyState extends FsmState {
    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Сразу отправить в сокет запрос иницализации этого сервиса.
      val ctx11 = service.prepareContext(_ctx)
      val askJson = EnsureServiceReadyAsk(service, ctx11)
      out ! mkJsAsk(askJson)
    }

    override def receiverPart: Receive = {
      // Сообщение от SioPR.js об удачной инициализации
      case EnsureServiceReadySuccess((_, ctx2, params)) =>
        _ctx = ctx2
        _serviceParams = params
        val nextState = if (params.picture.needStorage) {
          new PreparePictureStorageState()
        } else {
          // TODO Генерить абсолютную ссылку на отрендеренную карточку-картинку.
          new WallPostState(???)
        }
        become(nextState)

      // При инициализации клиента возникла проблема.
      case EnsureServiceReadyError((_, reason)) =>
        error(name + ": JS failed to ensureServiceReady: " + reason)
        harakiri()
    }
  }


  /** Если требуется отдельное обслуживание хранилища картинок, то в этом состоянии узнаём,
    * существует ли необходимое хранилище на сервисе? */
  class PreparePictureStorageState() extends FsmState {
    /** Отправить в js запрос на проверку наличия хранилища картинок. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      val jsonAsk = PreparePictureStorageAsk(service, _ctx)
      out ! mkJsAsk(jsonAsk)
    }

    override def receiverPart: Receive = {
      case PreparePictureStorageSuccess((_, ctx2)) =>
        _ctx = ctx2
        // Как заливать картинку на сервис? Можно с сервера, можно с клиента.
        val nextState = ctx2 \ "uploadUrl" match {
          // Клиент прислал uploadUrl на валидность, чтобы сервак отправил картинку вручную.
          case JsString(uploadUrl) =>
            if (service checkImgUploadUrl uploadUrl) {
              new S2sRenderAd2ImgState(uploadUrl)
            } else {
              error(name + "Unexpected or invalid upload URL: " + uploadUrl)
              harakiri()
              new DummyState
            }

          // Нет ссылки.
          case _ => new JsPutPictureToStorageState
        }
        become(nextState)

      case PreparePictureStorageError((_, reason)) =>
        error(name + ": Failed to ensure picture storage persistence: " + reason)
        harakiri()
    }
  }

  /** Формат картинки, из которой рендерится карточка. */
  def imgFmt = OutImgFmts.PNG


  /**
   * Произвести заливку картинки на удалённый сервис с помощью ссылки.
   * @param uploadUrl Ссылка для загрузки.
   */
  class S2sRenderAd2ImgState(uploadUrl: String) extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить в фоне генерацию картинки и отправку её на удалённый сервер.
      val oneAdArgs = OneAdQsArgs(
        adId = eactx.qs.adId,
        szMult = 2.0F   // TODO нужно вычислять на основе данных, присланных клиентом.
      )
      WkHtmlUtil.renderAd2img(oneAdArgs, eactx.request.mad.blockMeta, imgFmt)
        .onComplete {
          case Success(imgBytes)  =>  self ! new Ad2ImgRenderOk(imgBytes)
          case result             =>  self ! result
      }
    }

    // Дождаться завершения отправки картинки на удалённый сервер...
    override def receiverPart: Receive = {
      // Картинка готова. Можно собирать запрос.
      case imgReady: Ad2ImgRenderOk =>
        val nextState = new S2sPutPictureState(uploadUrl, imgReady.imgBytes)
        become(nextState)

      // Не удалось отрендерить карточку в картинку.
      case Failure(ex) =>
        error(s"$service: Failed to render ad[${eactx.qs.adId}] to picture", ex)
        harakiri()
    }

    sealed class Ad2ImgRenderOk(val imgBytes: Array[Byte])
  }


  /** Состояние отсылки запроса сохранения картинки на удалённый сервер. */
  class S2sPutPictureState(uploadUrl: String, imgBytes: Array[Byte]) extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Собрать POST-запрос и запустить его на исполнение
      val entity = MultipartEntityBuilder.create()
      val partName = _ctx \ "uploadName" match {
        case JsString(n) =>
          n
        case other =>
          val dflt = "photo"
          warn(name + "No multipart field name specified for picture in field uploadName. Using default: " + dflt)
          dflt
      }
      val fmt = imgFmt
      val partCt = ContentType.create(fmt.mime)
      val partFilename = eactx.qs.adId + "-" + eactx.request.mad.versionOpt.getOrElse(0L) + "." + fmt.name
      val partBody = new ByteArrayBody(imgBytes, partCt, partFilename)
      entity.addPart(partName, partBody)
      val baos = new ByteArrayOutputStream((imgBytes.length * 1.1F).toInt)
      entity.build().writeTo(baos)
      WS.url(uploadUrl)
        .withHeaders(HeaderNames.CONTENT_TYPE -> fmt.mime)
        .post(baos.toByteArray)
        .onComplete {
          case Success(wsResp)  => self ! wsResp
          case result           => self ! result
        }
    }

    override def receiverPart: Receive = {
      case wsResp: WSResponse =>
        //wsResp.json
        ???

      case Failure(ex) =>
        ???
    }
  }

  class JsPutPictureToStorageState() extends FsmState {
    override def receiverPart: Receive = ???
  }

  /** Состояние постинга на стену. */
  class WallPostState(imgId: String) extends FsmState {
    override def receiverPart: Receive = ???
  }

}


