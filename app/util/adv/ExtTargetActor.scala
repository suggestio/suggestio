package util.adv

import java.io.ByteArrayOutputStream

import akka.actor.Props
import controllers.routes
import io.suggest.util.UrlUtil
import models.Context
import models.adv.js._
import models.adv.{JsExtTarget, MExtReturn, MExtReturns, IExtAdvTargetActorArgs}
import models.adv.js.ctx._
import models.blk.{SzMult_t, OneAdQsArgs}
import models.im.OutImgFmts
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.ByteArrayBody
import play.api.http.HeaderNames
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.{WSResponse, WS}
import play.api.Play.current
import util.PlayMacroLogsImpl
import util.async.FsmActor
import util.img.WkHtmlUtil

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

  /** Дефолтовое значение szMult для рендера карточки, когда другого значения нет под рукой. */
  def szMultDflt: SzMult_t = 2.0F


  /** Формат картинки, из которой рендерится карточка. */
  def imgFmt = OutImgFmts.PNG

}


import ExtTargetActor._


case class ExtTargetActor(args: IExtAdvTargetActorArgs)
  extends FsmActor
  with PlayMacroLogsImpl
{

  import LOGGER._

  override protected var _state: FsmState = new DummyState

  /** Ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty
  override def receive = allStatesReceiver


  override def preStart(): Unit = {
    super.preStart()
    become(new PrepareZeroContextState(args.mctx0))
  }


  /** Значение replyTo в запросах клиенту. */
  val replyTo = self.path.name

  /**
   * Сгенерить аргументы для рендера карточки в картинку.
   * @param szMult Мультипликатор размера.
   * @return Экземпляр [[models.blk.OneAdQsArgs]], готовый к эксплуатации.
   */
  def getAdRenderArgs(szMult: SzMult_t = szMultDflt) = {
    OneAdQsArgs(
      adId = args.qs.adId,
      szMult = szMult
    )
  }

  /**
   * Сгенерить абсолютную ссылку на картинку отрендеренной карточки.
   * @param szMult Мультипликатор размера.
   * @return Строка-ссылка.
   */
  def getExtAdImgAbsUrl(szMult: SzMult_t = szMultDflt): String = {
    val args = getAdRenderArgs(szMult)
    Context.SC_URL_PREFIX + routes.MarketShowcase.onlyOneAd(args).url
  }


  /**
   * Генерация абс.ссылок на выдачу s.io.
   * @param ret Куда делается возврат.
   * @return Абсолютный URL в виде строки.
   */
  def getScUrl(ret: MExtReturn): String = {
    ret.builder()
      .setAdnId( args.target.target.adnId )
      .setFocusedAdId( args.request.mad.idOrNull )
      .setFocusedProducerId( args.request.mad.producerId )
      .toAbsUrl
  }


  // FSM states

  /** Состояние начальной подготовки контекста.
    * Пока асинхронных и ресурсоёмких операций у нас тут нет, поэтому всё необходимое происходит в afterBecome(). */
  class PrepareZeroContextState(mctx0: MJsCtx) extends FsmState {

    /** Запустить сборку данных и нового контекста. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить заполнение контекста исходными данными по цели.
      val madCtx = MAdCtx(
        id      = args.request.mad.id.get,
        content = MAdContentCtx.apply( args.request.mad ),
        scUrl   = Some( getScUrl(MExtReturns.ToAd) ),
        // Сразу вставить URL картинки в контекст.
        picture = {
          val szMult = szMultDflt
          val sz0 = args.request.mad.blockMeta
          val sz1 = PictureSizeCtx(
            width  = (sz0.width * szMult).toInt,
            height = (sz0.height * szMult).toInt
          )
          val adArgs = getAdRenderArgs(szMult)
          val url = Context.SC_URL_PREFIX + routes.MarketShowcase.onlyOneAdAsImage(adArgs).url
          Some(MPictureCtx(
            size   = Some(sz1),
            sioUrl = Some(url)
          ))
        }
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
        domain  = Seq( UrlUtil.url2dkey( targetFull.url ) ),
        status  = None,
        error   = None
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
      context.parent ! JsCommand(
        jsBuilder = HandleTargetAsk(mctx0, replyTo = Some(replyTo)),
        sendMode  = CmdSendModes.Queued
      )
    }

    override def receiverPart: Receive = {
      // Супервизор прислал ws-ответ от js по текущему таргету.
      case Answer(_, mctx1) if mctx1.status.nonEmpty =>
        mctx1.status.get match {
          // Публикация удалась. Актор должен обрадовать юзера и тихо завершить работу.
          case AnswerStatuses.Success =>
            // TODO отрендерить error на экран
            trace("Success received from js. Finishing...")
            harakiri()

          // Непоправимая ошибка на стороне js. Актор должен огорчить юзера, и возможно ещё что-то сделать.
          case AnswerStatuses.Error =>
            // TODO Отрендерить success на экран юзеру.
            warn("Error received from JS: " + mctx1.error)
            harakiri()

          // JS'у недостаточно данных в контексте для создания публикации.
          case AnswerStatuses.FillContext =>
            trace("Fill context requested")
            become(new FillContextState(mctx1))
        }
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
        // TODO Добавить поддержку запроса ещё неск.карточек.
        // Хз что делать. Видимо всё исправно уже.
        new HandleTargetState(mctx0)
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

    // TODO нужно вычислять на основе данных, присланных клиентом.
    def szMult: SzMult_t = szMultDflt

    /** При переключении на состояние надо запустить в фоне рендер карточки. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить в фоне генерацию картинки и отправку её на удалённый сервер.
      val renderArgs = getAdRenderArgs(szMult)
      WkHtmlUtil.renderAd2img(renderArgs, args.request.mad.blockMeta, imgFmt)
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
        error(s"Failed to render ad[${args.qs.adId}] into picture", ex)
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

    /** При переходе на это состояние надо запустить отправку картинки на удалённый сервер. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Собрать POST-запрос и запустить его на исполнение
      val boundary = "----------BOUNDARY--" + args.request.mad.id.getOrElse("BoUnDaRy-_-")
      val entity = MultipartEntityBuilder.create()
        .setBoundary(boundary)
      val fmt = imgFmt
      val partCt = ContentType.create(fmt.mime)
      val partFilename = args.qs.adId + "-" + args.request.mad.versionOpt.getOrElse(0L) + "." + fmt.name
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
        // TODO Сообщить юзеру о провале.
        harakiri()

      // Запрос не удался или произошла ещё какая-то ошибка.
      case Failure(ex) =>
        error(s"Failed to POST image to ${uploadCtx.url} as part '${uploadCtx.partName}'", ex)
        // TODO Сообщить юзеру о неудаче. Или попробовать ещё разок, а?
        harakiri()
    }
  }


}
