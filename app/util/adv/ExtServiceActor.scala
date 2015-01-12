package util.adv

import java.io.ByteArrayOutputStream

import akka.actor.{Props, ActorRef}
import models.adv.MExtServices.MExtService
import models.adv.js.ctx._
import models.adv.{MExtAdvContext, MExtTarget}
import models.adv.js._
import models.blk.{SzMult_t, OneAdQsArgs}
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

  def serverCtx = _ctx \ "_server"
  def pictureUploadCtxRaw = serverCtx \ "picture" \ "upload"

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
        val puctx = PictureUploadCtx.fromJson(pictureUploadCtxRaw)
        val nextState = puctx match {
          case s2sCtx: S2sPictureUpload =>
            new S2sRenderAd2ImgState(s2sCtx)
          case C2sPictureUpload =>
            new JsPutPictureToStorageState
          case UrlPictureUpload =>
            // TODO Сгенерить ссылку на картику и пробросить в WallPostState
            new WallPostState(???)
        }
        become(nextState)

      case PreparePictureStorageError((_, reason)) =>
        error(name + ": Failed to ensure picture storage persistence: " + reason)
        harakiri()
    }
  }

  /** Формат картинки, из которой рендерится карточка. */
  def imgFmt = OutImgFmts.PNG


  /** Заготовка состояния рендера карточки в картинку. */
  trait RenderAd2ImgStateT extends FsmState {
    /** На какое состояние переключаться надо, когда картинка отрендерилась? */
    def getNextStateOk(okRes: Ad2ImgRenderOk): FsmState

    def szMult: SzMult_t = 2.0F

    /** При переключении на состояние надо запустить в фоне рендер карточки. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить в фоне генерацию картинки и отправку её на удалённый сервер.
      val oneAdArgs = OneAdQsArgs(
        adId = eactx.qs.adId,
        szMult = szMult   // TODO нужно вычислять на основе данных, присланных клиентом.
      )
      WkHtmlUtil.renderAd2img(oneAdArgs, eactx.request.mad.blockMeta, imgFmt)
        .onComplete {
          case Success(imgBytes)  =>  self ! new Ad2ImgRenderOk(imgBytes)
          case result             =>  self ! result
      }
    }

    /** Дождаться завершения отправки картинки на удалённый сервер... */
    override def receiverPart: Receive = {
      // Картинка готова. Можно собирать запрос.
      case imgReady: Ad2ImgRenderOk =>
        become(getNextStateOk(imgReady))

      // Не удалось отрендерить карточку в картинку.
      case Failure(ex) =>
        error(s"$service: Failed to render ad[${eactx.qs.adId}] to picture", ex)
        harakiri()
    }

    class Ad2ImgRenderOk(val imgBytes: Array[Byte])
  }
  
  /**
   * Произвести заливку картинки на удалённый сервис с помощью ссылки.
   * @param uploadCtx Данные для s2s-загрузки.
   */
  class S2sRenderAd2ImgState(uploadCtx: S2sPictureUpload) extends RenderAd2ImgStateT {
    override def getNextStateOk(okRes: Ad2ImgRenderOk): FsmState = {
      new S2sPutPictureState(uploadCtx, okRes.imgBytes)
    }
  }


  /** Состояние отсылки запроса сохранения картинки на удалённый сервер. */
  class S2sPutPictureState(uploadCtx: S2sPictureUpload, imgBytes: Array[Byte]) extends FsmState {

    /** При переходе на это состояние надо запустить отправку картинки на удалённый сервер. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Собрать POST-запрос и запустить его на исполнение
      val entity = MultipartEntityBuilder.create()
      val fmt = imgFmt
      val partCt = ContentType.create(fmt.mime)
      val partFilename = eactx.qs.adId + "-" + eactx.request.mad.versionOpt.getOrElse(0L) + "." + fmt.name
      val partBody = new ByteArrayBody(imgBytes, partCt, partFilename)
      entity.addPart(uploadCtx.partName, partBody)
      val baos = new ByteArrayOutputStream((imgBytes.length * 1.1F).toInt)
      entity.build().writeTo(baos)
      WS.url(uploadCtx.url)
        .withHeaders(HeaderNames.CONTENT_TYPE -> fmt.mime)
        .post(baos.toByteArray)
        .onComplete {
          case Success(wsResp)  => self ! wsResp
          case result           => self ! result
        }
    }

    /** Проверка валидности возвращаемого значения. */
    def respStatusCodeValid(status: Int): Boolean = {
      status >= 200 && status <= 299
    }

    /** Ждём ответа от удалённого сервера с результатом загрузки картинки. */
    override def receiverPart: Receive = {
      // Успешно выполнена загрузка картинки на удалённый сервер.
      case wsResp: WSResponse if respStatusCodeValid(wsResp.status) =>
        //wsResp.json
        ???

      // Запрос выполнился, но в ответ пришло что-то неожиданное.
      case wsResp: WSResponse =>
        ???

      case Failure(ex) =>
        ???
    }
  }

  class JsPutPictureToStorageState() extends FsmState {
    override def receiverPart: Receive = ???
  }

  /** Состояние постинга на стену. */
  class WallPostState(pictureInfo: String) extends FsmState {
    override def receiverPart: Receive = ???
  }

}


