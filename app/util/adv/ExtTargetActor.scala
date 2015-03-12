package util.adv

import java.io.ByteArrayOutputStream

import akka.actor.Props
import controllers.routes
import io.suggest.util.UrlUtil
import models.{MImgSizeT, Context}
import models.adv.js._
import models.adv.{JsExtTarget, MExtReturn, MExtReturns, IExtAdvTargetActorArgs}
import models.adv.js.ctx._
import models.blk.{OneAdWideQsArgs, SzMult_t, OneAdQsArgs}
import models.event.{ErrorInfo, MEventTmp, RenderArgs}
import models.im.OutImgFmts
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.ByteArrayBody
import play.api.http.HeaderNames
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsString
import play.api.libs.ws.{WSResponse, WS}
import play.api.Play.{current, configuration}
import util.PlayMacroLogsImpl
import util.async.FsmActor
import util.event.{EventType, EventTypes}
import util.img.AdRenderUtil
import util.jsa.{InnerHtmlById, JsAppendById}
import ExtUtil.RUNNER_EVENTS_DIV_ID

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 14:35
 * Description: ext adv API v2: Актор, занимающийся одной целью и только.
 * Пришел на смену ExtServiceActor, который перебирал цели в рамках одного сервиса.
 */
object ExtTargetActor {

  def props(args: IExtAdvTargetActorArgs): Props = {
    Props(ExtTargetActor(args))
  }

  /** Формат картинки, из которой рендерится карточка. */
  def imgFmt = OutImgFmts.JPEG

  /** Макс.число попыток fillCtx. Нужно чтобы избегать ситуаций бесконечного заполнения контекста. */
  val MAX_FILL_CTX_TRIES = configuration.getInt("adv.ext.target.fillCtx.try.max") getOrElse 2
}


import ExtTargetActor._


case class ExtTargetActor(args: IExtAdvTargetActorArgs)
  extends FsmActor
  with PlayMacroLogsImpl
  with MediatorSendCommand
{

  import args.ctx
  import LOGGER._

  override protected var _state: FsmState = new DummyState

  /** Ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty
  override def receive = allStatesReceiver


  override def preStart(): Unit = {
    super.preStart()
    become(new EnsureClientReadyState(args.mctx0))
  }

  protected var fillCtxTry: Int = 0


  /** Значение replyTo в запросах клиенту. */
  protected val replyTo = self.path.name

  protected def mad = args.request.mad
  protected def service = args.target.target.service

  /** Инфа по картинке кодируется этим классом. */
  protected case class PicInfo(wide: Option[OneAdWideQsArgs], width: Int, height: Int, szMult: SzMult_t) extends MImgSizeT

  /** Инфа по рендеру карточки в картинке. */
  protected lazy val madRenderInfo: PicInfo = {
    // Вычисляем мультипликатор размера исходя из отношения высот.
    val srv = service
    val advPostMaxSz = srv.advPostMaxSz
    val hDiff = advPostMaxSz match {
      case Some(sz) => sz.height.toFloat / mad.blockMeta.height.toFloat
      case None     => 1.0F
    }
    // Нужно домножить на минимально необходимый размер для сервиса.
    // TODO Проквантовать полученный szMult?
    val szMult1 = hDiff * srv.szMult
    // Вычислить необходимость и ширину широкого отображения.
    val wideWidthOpt = srv.advExtWidePosting(mad, szMult1)
      //.filter { pmWidth => mad.blockMeta.wide || pmWidth.toFloat > mad.blockMeta.width * 1.15F }
    PicInfo(
      wide   = wideWidthOpt,
      width  = wideWidthOpt.fold { (mad.blockMeta.width * szMult1).toInt } (_.width),
      height = (advPostMaxSz.fold(mad.blockMeta.height)(_.height) * szMult1).toInt,
      szMult = szMult1
    )
  }

  /**
   * Сгенерить аргументы для рендера карточки в картинку.
   * @return Экземпляр [[models.blk.OneAdQsArgs]], готовый к эксплуатации.
   */
  protected def adRenderArgs = {
    val mri = madRenderInfo
    OneAdQsArgs(
      adId    = args.qs.adId,
      szMult  = mri.szMult,
      vsnOpt  = mad.versionOpt,
      wideOpt = mri.wide
    )
  }


  def getDomain: String = UrlUtil.url2dkey(args.target.target.url)


  /**
   * Генерация абс.ссылок на выдачу s.io.
   * @param ret Куда делается возврат.
   * @return Абсолютный URL в виде строки.
   */
  def getScUrl(ret: MExtReturn): String = {
    ret.builder()
      .setAdnId( args.target.target.adnId )
      .setFocusedAdId( mad.idOrNull )
      .setFocusedProducerId( mad.producerId )
      .toAbsUrl
  }


  def evtRenderArgs(etype: EventType, errors: ErrorInfo*): RenderArgs = {
    evtRenderArgs(etype, withContainer = false, errors : _*)
  }
  def evtRenderArgs(etype: EventType, withContainer: Boolean, errors: ErrorInfo*): RenderArgs = {
    RenderArgs(
      mevent = MEventTmp(
        etype   = etype,
        ownerId = args.request.producerId,
        id      = Some(replyTo)
      ),
      advExtTgs = Seq(args.target.target),
      errors    = errors,
      withContainer = withContainer
    )
  }

  /** Перезаписать содержимое блока цели на странице. */
  def renderEventReplace(rargs: RenderArgs): Unit = {
    val html = rargs.mevent.etype.render(rargs)
    val htmlStr = JsString(html.body) // TODO Вызывать для рендера туже бадягу, что и контроллер вызывает.
    val jsa = InnerHtmlById(replyTo, htmlStr)
    val cmd = JsCmd( jsa.renderToString() )
    sendCommand(cmd)
  }

  // FSM states

  class EnsureClientReadyState(mctx0: MJsCtx) extends FsmState {

    def renderInProcess(): Unit = {
      val etype = EventTypes.AdvExtTgInProcess
      val rargs = evtRenderArgs(etype, withContainer = true)
      val html = etype.render(rargs)
      val htmlStr = JsString(html.body) // TODO Вызывать для рендера туже бадягу, что и контроллер вызывает.
      val jsa = JsAppendById(RUNNER_EVENTS_DIV_ID, htmlStr)
      val cmd = JsCmd( jsa.renderToString() )
      sendCommand(cmd)
    }

    /** При переходе в это состояние нужно дождаться готовности клиента
      * и отрендерить юзеру инфу о начале процесса публикации. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // TODO Запустить проверку готовности клиента к обработке домена.
      // Считаем, что клиент готов. Отрендерить юзеру писульку о начале процесса публикации цели.
      renderInProcess()
      // Переход на след.состояние...
      become(new PrepareZeroContextState(mctx0))
    }

    override def receiverPart: Receive = PartialFunction.empty
  }


  /** JSON-контекст инфы по картинке текущей карточки. */
  protected def jsPicCtx: MPictureCtx = {
    val mri = madRenderInfo
    val sz1 = PictureSizeCtx(
      width  = mri.width,
      height = mri.height
    )
    val url = Context.SC_URL_PREFIX + routes.MarketShowcase.onlyOneAdAsImage(adRenderArgs).url
    MPictureCtx(
      size   = Some(sz1),
      sioUrl = Some(url)
    )
  }


  /** Состояние начальной подготовки контекста.
    * Пока асинхронных и ресурсоёмких операций у нас тут нет, поэтому всё необходимое происходит в afterBecome(). */
  class PrepareZeroContextState(mctx0: MJsCtx) extends FsmState {

    /** Запустить сборку данных и нового контекста. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить заполнение контекста исходными данными по цели.
      val madCtx = MAdCtx(
        id      = mad.id.get,
        content = MAdContentCtx.fromAd( mad, args.request.producer ),
        scUrl   = Some( getScUrl(MExtReturns.ToAd) ),
        // Сразу вставить URL картинки в контекст.
        picture = Some( jsPicCtx )
      )
      // Собираем инфу по цели размещения
      val targetFull = JsExtTarget(
        target      = args.target.target,
        onClickUrl  = getScUrl(args.target.returnTo)
      )
      // Собираем новый context
      val mctx1 = mctx0.copy(
        mads    = Seq(madCtx),
        target  = Some(targetFull),
        domain  = Seq(getDomain),
        status  = None,
        error   = None,
        service = Some(service)
      )
      become(new HandleTargetState(mctx1))
    }

    /** Ресивер состояния. До сюда выполнение пока не доходит.
        Раздельная логика оставлена на случай появления асинхрона в afterBecome(). */
    override def receiverPart: Receive = PartialFunction.empty
  }

  /**
   * Это главное состояние актора. В нём актор взаимодействует с JS-подсистемой.
   * @param mctx0 Исходный context.
   */
  class HandleTargetState(mctx0: MJsCtx) extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      val cmd = HandleTargetAsk(
        mctx0     = mctx0,
        replyTo   = Some(replyTo),
        sendMode  = CmdSendModes.Queued
      )
      sendCommand(cmd)
    }

    /** Рендер на экран уведомления об успехе, стерев предыдущую инфу по target'у. */
    def renderSuccess(): Unit = {
      val rargs = evtRenderArgs( EventTypes.AdvExtTgSuccess )
      renderEventReplace(rargs)
    }

    /** Сообщить юзеру, что на стороне js зафиксирована ошибка. */
    def renderError(errOpt: Option[JsErrorInfo]): Unit = {
      val err = errOpt match {
        case Some(jserr) =>
          ErrorInfo(
            msg = jserr.msg,
            args = jserr.args,
            info = jserr.other.map(_.toString)
          )
        case None =>
          ErrorInfo(
            msg = "error.adv.ext.js.refused"
          )
      }
      val rargs = evtRenderArgs(EventTypes.AdvExtTgError, err)
      renderEventReplace(rargs)
    }

    override def receiverPart: Receive = {
      // Супервизор прислал распарсенный ws-ответ от js по текущему таргету.
      case ans: Answer if ans.ctx2.status.nonEmpty =>
        ans.ctx2.status.get match {
          // Публикация удалась. Актор должен обрадовать юзера и тихо завершить работу.
          case AnswerStatuses.Success =>
            trace("Success received from js. Finishing...")
            renderSuccess()
            harakiri()

          // Непоправимая ошибка на стороне js. Актор должен огорчить юзера, и возможно ещё что-то сделать.
          case AnswerStatuses.Error =>
            warn("Error received from JS: " + ans.ctx2.error)
            renderError(ans.ctx2.error)
            harakiri()

          // JS'у недостаточно данных в контексте для создания публикации.
          case AnswerStatuses.FillContext =>
            if (fillCtxTry >= ExtTargetActor.MAX_FILL_CTX_TRIES) {
              error("Too many fill ctx tries. Stopping. Last answer was:\n  " + ans)
              renderError(ans.ctx2.error)
              harakiri()
            } else {
              trace("Fill context requested")
              fillCtxTry += 1
              become(new FillContextState(ans.ctx2))
            }
        }

      // Для облегчения отладки.
      case ans: Answer =>
        error("_status field is missing in answer:\n  " + ans)
    }
  }


  /** Состояние заполнения контекста недостающими данными. */
  class FillContextState(mctx0: MJsCtx) extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Пройти данные контекста в поисках каких-либо зацепок...
      // Смотрим [mads].picture.upload на предмет каких-либо указаний...
      val maybeAdNeedPicUpload = mctx0.mads.find {
        _.picture.flatMap(_.upload).nonEmpty
      }
      val nextState: FsmState = if (maybeAdNeedPicUpload.isDefined) {
        val ai = maybeAdNeedPicUpload.get
        ai.picture.flatMap(_.upload).get match {
          case s2sCtx: S2sPictureUpload =>
            new S2sRenderAd2ImgState(mctx0, ai.id, s2sCtx)
        }
      } else {
        val maybeAdNeedNewPicUrl = mctx0.mads.find { madCtx =>
          val picUrlOpt = madCtx.picture.flatMap(_.sioUrl)
          picUrlOpt.isEmpty || picUrlOpt.exists(_.trim.isEmpty)
        }
        if (maybeAdNeedNewPicUrl.nonEmpty) {
          // Требуется выставить новую ссылку на картинку в контекст
          val madCtx1 = maybeAdNeedNewPicUrl.get.copy(picture = Some(jsPicCtx))
          val mads1 = mctx0.mads.map { madCtx =>
            if (madCtx1.id == madCtx.id) madCtx1 else madCtx
          }
          val mctx1 = mctx0.copy(mads = mads1)
          new FillContextState(mctx1)

        } else {
          // TODO Добавить поддержку запроса ещё неск.карточек.
          // Хз что делать. Видимо всё исправно уже.
          new HandleTargetState(mctx0)
        }
      }
      become(nextState)
    }

    override def receiverPart = PartialFunction.empty
  }


  // TODO Вынести render ad + s2s upload в отдельный актор. Так можно будет рендерить и грузить несколько карточек одновременно.

  /** Заготовка состояния рендера карточки в картинку. */
  trait RenderAd2ImgStateT extends FsmState {
    /** На какое состояние переключаться надо, когда картинка отрендерилась? */
    def getNextStateOk(okRes: Ad2ImgRenderOk): FsmState

    def renderWkhtmlError(ex: Throwable): Unit = {
      val err = ErrorInfo(
        msg  = "error.sio.internal",
        info = Some(s"[$replyTo] ${ex.getMessage}")
      )
      val rargs = evtRenderArgs(EventTypes.AdvExtTgError, err)
      renderEventReplace(rargs)
    }

    /** При переключении на состояние надо запустить в фоне рендер карточки. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить в фоне генерацию картинки и отправку её на удалённый сервер.
      AdRenderUtil.renderAd2img(adRenderArgs, mad, imgFmt)
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
        error(s"[$replyTo] Failed to render ad[${args.qs.adId}] into picture", ex)
        renderWkhtmlError(ex)
        harakiri()
    }

    class Ad2ImgRenderOk(val imgBytes: Array[Byte])
  }

  /**
   * Произвести заливку картинки на удалённый сервис с помощью ссылки.
   * @param uploadCtx Данные для s2s-загрузки.
   */
  class S2sRenderAd2ImgState(val mctx0: MJsCtx, madId: String, uploadCtx: S2sPictureUpload) extends RenderAd2ImgStateT {
    override def getNextStateOk(okRes: Ad2ImgRenderOk): FsmState = {
      new S2sPutPictureState(mctx0, madId, uploadCtx, okRes.imgBytes)
    }
  }


  /** Состояние отсылки запроса сохранения картинки на удалённый сервер. */
  class S2sPutPictureState(val mctx0: MJsCtx, madId: String, uploadCtx: S2sPictureUpload, imgBytes: Array[Byte])
    extends FsmState {

    /** Выставить в picutre.saved указанной картинки новое значение. */
    def withPictureBody(respBody: String): MJsCtx = {
      mctx0.copy(
        mads = mctx0.mads.map { mad =>
          if (mad.id == madId) {
            mad.copy(
              picture = {
                val pc = mad.picture
                  .get
                  .copy(upload = None, saved = Some(respBody))
                Some(pc)
              }
            )   // mad.copy()
          } else {
            mad
          }
        }   // map()
      )     // copy()
    }       // withPictureBody()


    /** Отправить юзеру траурную весточку, что не удалось картинку залить по s2s. */
    def renderImgUploadRefused(wsResp: WSResponse): Unit = {
      val errMsg = new StringBuilder(wsResp.body.length + uploadCtx.url.length + 128)
        .append("POST ").append(uploadCtx.url).append('\n')
        .append(wsResp.status).append(' ').append(wsResp.statusText).append('\n').append('\n')
        .append(wsResp.body)
        .toString()
      val err = ErrorInfo(
        msg  = "error.adv.ext.s2s.img.upload.refused",
        args = Seq(getDomain),
        info = Some(errMsg)
      )
      val rargs = evtRenderArgs(EventTypes.AdvExtTgError, err)
      renderEventReplace(rargs)
    }

    /** Не удалось связаться с запрошенным сервером. */
    def renderImgUploadFailed(ex: Throwable): Unit = {
      val err = ErrorInfo(
        msg  = "error.adv.ext.s2s.img.upload.failed",
        args = Seq(getDomain),
        info = Some(s"[$replyTo] ${ex.getMessage}")
      )
      val rargs = evtRenderArgs(EventTypes.AdvExtTgError, err)
      renderEventReplace(rargs)
    }

    /** При переходе на это состояние надо запустить отправку картинки на удалённый сервер. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Собрать POST-запрос и запустить его на исполнение
      val boundary = "----------BOUNDARY--" + mad.id.getOrElse("BoUnDaRy-_-")
      val entity = MultipartEntityBuilder.create()
        .setBoundary(boundary)
      val fmt = imgFmt
      val partCt = ContentType.create(fmt.mime)
      val partFilename = args.qs.adId + "-" + mad.versionOpt.getOrElse(0L) + "." + fmt.name
      val partBody = new ByteArrayBody(imgBytes, partCt, partFilename)
      entity.addPart(uploadCtx.partName, partBody)
      val baos = new ByteArrayOutputStream((imgBytes.length * 1.1F).toInt)
      val resp = entity.build()
      resp.writeTo(baos)
      WS.url(uploadCtx.url)
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> ("multipart/form-data; boundary=" + boundary)
        )
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
        debug(s"successfully POSTed ad image to remote server: HTTP ${wsResp.statusText}")
        trace(s"Remote server response is:\n ${wsResp.body}")
        val mctx2 = withPictureBody(wsResp.body)
        val nextState = new FillContextState(mctx2)
        become(nextState)

      // Запрос выполнился, но в ответ пришло что-то неожиданное.
      case wsResp: WSResponse =>
        error(s"Cannot load image to remote server: HTTP ${wsResp.statusText}: ${wsResp.body}")
        renderImgUploadRefused(wsResp)
        harakiri()

      // Запрос не удался или произошла ещё какая-то ошибка.
      case Failure(ex) =>
        // Если юзер обратится с описаловом, то там будет ключ ошибки. Экзепшен можно будет отследить по логам.
        error(s"[$replyTo] Failed to POST image to ${uploadCtx.url} as part '${uploadCtx.partName}'", ex)
        renderImgUploadFailed(ex)
        harakiri()
    }
  }


}
