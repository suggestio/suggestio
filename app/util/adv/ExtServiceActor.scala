package util.adv

import java.io.ByteArrayOutputStream

import akka.actor.Props
import controllers.routes
import models.ScJsState
import models.adv.js.ctx._
import models.adv._
import models.adv.js._
import models.blk.{SzMult_t, OneAdQsArgs}
import models.im.OutImgFmts
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.content.ByteArrayBody
import org.apache.http.entity.mime.MultipartEntityBuilder
import play.api.http.HeaderNames
import util.{Context, PlayMacroLogsImpl}
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
  def props(args: MExtAdvActorArgs) = {
    Props(ExtServiceActor(args))
  }

  /** Дефолтовое значение szMult для рендера карточки, когда другого значения нет под рукой. */
  def szMultDflt: SzMult_t = 2.0F

}


/** Актор, занимающийся загрузкой карточек в однин рекламный сервис. */
case class ExtServiceActor(args: MExtAdvActorArgs)
  extends FsmActor with PlayMacroLogsImpl with SioPrJsUtil
{
  
  import LOGGER._
  import args.{service, out, eactx}

  protected var _ctx  : JsCtx_t = args.ctx0
  
  /** Текущий контекст вызова. Выставляется в конструкторе актора и после инициализации клиента сервиса. */
  def serverCtx = _ctx \ "_server"
  def pictureUploadCtxRaw = serverCtx \ "picture" \ "upload"

  /** Текущее состояние FSM. */
  override protected var _state: super.FsmState = new DummyState

  override def receive: Receive = allStatesReceiver

  /** Ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty

  override def preStart(): Unit = {
    super.preStart()
    become(new EnsureServiceReadyState)
  }

  /** Команда к самозавершению. */
  protected def harakiri(): Unit = {
    trace("harakiri(). Last state was " + _state.name)
    context stop self
  }

  /**
   * Сгенерить аргументы для рендера карточки в картинку.
   * @param szMult Мультипликатор размера.
   * @return Экземпляр [[models.blk.OneAdQsArgs]], готовый к эксплуатации.
   */
  def getAdRenderArgs(szMult: SzMult_t = ExtServiceActor.szMultDflt) = {
    OneAdQsArgs(
      adId = args.eactx.qs.adId,
      szMult = szMult
    )
  }

  /**
   * Сгенерить абсолютную ссылку на картинку отрендеренной карточки.
   * @param szMult Мультипликатор размера.
   * @return Строка-ссылка.
   */
  def getExtAdImgAbsUrl(szMult: SzMult_t = ExtServiceActor.szMultDflt): String = {
    val args = getAdRenderArgs(szMult)
    Context.SC_URL_PREFIX + routes.MarketShowcase.onlyOneAd(args).url
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
      case EnsureServiceReadySuccess((_, ctx2)) =>
        _ctx = ctx2
        val puctxOpt = PictureUploadCtx.maybeFromJson(pictureUploadCtxRaw)
        val nextState = if (puctxOpt contains UrlPictureUpload) {
          new PublishMessageState(getExtAdImgAbsUrl())
        } else {
          new PreparePictureStorageState()
        }
        become(nextState)

      // При инициализации клиента возникла проблема.
      case EnsureServiceReadyError((_, reason)) =>
        error(name + ": JS failed to ensureServiceReady: " + reason)
        harakiri()
    }
  }


  /** Состояния, исполняемые в рамках обработки цели. */
  trait StateInTarget extends FsmState {
    def targets: ActorTargets_t
    def target = targets.head
    // TODO def Переключалка на начало обработки следующего target'а, если есть.
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
            new C2sPutPictureToStorageState
          case UrlPictureUpload =>
            // Возможно unreachable code, но избавляет от warning'a.
            new PublishMessageState(getExtAdImgAbsUrl())
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

    // TODO нужно вычислять на основе данных, присланных клиентом.
    def szMult: SzMult_t = ExtServiceActor.szMultDflt

    /** При переключении на состояние надо запустить в фоне рендер карточки. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить в фоне генерацию картинки и отправку её на удалённый сервер.
      val renderArgs = getAdRenderArgs(szMult)
      WkHtmlUtil.renderAd2img(renderArgs, eactx.request.mad.blockMeta, imgFmt)
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
      // Успешно выполнена загрузка картинки на удалённый сервер. Надо перейти на следующее состояние.
      case wsResp: WSResponse if respStatusCodeValid(wsResp.status) =>
        debug(s"$service successfully POSTed ad image to remote server: HTTP ${wsResp.statusText}")
        trace(s"$service Remote server response is:\n ${wsResp.body}")
        val nextState = new PublishMessageState(wsResp.body)
        become(nextState)

      // Запрос выполнился, но в ответ пришло что-то неожиданное.
      case wsResp: WSResponse =>
        error(s"$service Cannot load image to remote server: HTTP ${wsResp.statusText}: ${wsResp.body}")
        harakiri()

      // Запрос не удался или произошла ещё какая-то ошибка.
      case Failure(ex) =>
        error(s"$service Failed to POST image to ${uploadCtx.url} as part '${uploadCtx.partName}'", ex)
        harakiri()
    }
  }


  /** Отправка картинки в хранилище через клиента. */
  class C2sPutPictureToStorageState() extends FsmState {
    override def receiverPart: Receive = ???
  }


  /** Состояние постинга сообщений на стены. */
  class PublishMessageState(pictureInfo: String, targets: ActorTargets_t = args.targets0) extends FsmState {
    /** Нужно отправить в js команду отправки запроса размещения сообщения по указанной цели. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      if (targets.isEmpty) {
        debug("No more targes. Finishing")
        harakiri()
      } else {
        val targetInfo = targets.head
        // Извлекаем первую цель из списка, отправляем её на сторону js.
        val pmAsk = PublishMessageAsk(
          service = service,
          ctx     = _ctx,
          target  = targetInfo.target,
          onClickUrl = {
            // TODO Нужно, чтобы пользователь мог настраивать целевой url во время размещения КАРТОЧКИ.
            ScJsState( adnId = Some(targetInfo.target.adnId) )
              .ajaxStatedUrl()
          }
        )
        out ! mkJsAsk(pmAsk)
      }
    }

    override def receiverPart: Receive = {
      // js рапортует, что сообщение удалось разместить. Нужно перейти на следующую итерацию.
      case PublishMessageSuccess((_, ctx2)) =>
        ???

      // Ошибка публикации сообщения. Ругнутся в логи, по возможности написать об этом юзеру, перейти к следующей цели.
      case PublishMessageError((_, reason)) =>
        error(service.strId + "Failed to publish message: " + reason)
        ???
    }

    def nextTargetState(): Unit = {
      val nextState = ???
      ???
    }
  }

}


