package util.adv

import akka.actor.Props
import io.suggest.adv.ext.model.im.INamedSize2di
import models.adv.ext.act._
import models.adv.js._
import models.adv._
import models.adv.js.ctx._
import models.event.ErrorInfo
import models.im.OutImgFmts
import models.mext.{UploadRefusedException, UploadPart, MpUploadArgs}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSResponse
import play.api.Play.{current, configuration}
import util.PlayMacroLogsImpl
import util.adv.ut.ExtTargetActorUtil
import util.async.FsmActor
import util.event.EventTypes
import ut._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 14:35
 * Description: ext adv API v2: Актор, занимающийся одной целью и только.
 * Пришел на смену ExtServiceActor, который перебирал цели в рамках одного сервиса.
 * Этот актор взаимодействует с сервисами через JS API и браузер пользователя.
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
  with ExtTargetActorUtil
  with ReplyTo
  with MediatorSendCommand
  with PlayMacroLogsImpl
  with EtaCustomArgsBase
  with CompatWsClient    // TODO
  with RenderAd2Img
  with S2sMpUploadRender
{

  import LOGGER._

  override protected var _state: FsmState = new DummyState

  /** Ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty
  override def receive = allStatesReceiver


  override def preStart(): Unit = {
    super.preStart()
    become(new EnsureClientReadyState(args.mctx0))
  }

  // TODO Занести внутрь state'ов.
  protected var fillCtxTry: Int = 0

  override def service = args.target.target.service

  /** Реализация модели из [[models.adv.ext.act.EtaCustomArgsBase]]. */
  case class CustomArgs(adRenderMaxSz: INamedSize2di) extends MCustomArgsT {
    // Готовая инфа по отрендеренной карточкчи обычно нужна по несколько раз вподряд. Или не нужна вообще.
    override lazy val madRenderInfo = super.madRenderInfo
  }

  protected var _customArgs: CustomArgs = {
    CustomArgs(
      adRenderMaxSz = service.advPostMaxSz(args.target.target.url)
    )
  }

  // FSM states

  class EnsureClientReadyState(mctx0: MJsCtx) extends FsmState {

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
        picture = Some( _customArgs.jsPicCtx )
      )
      // Собираем новый context. Поле target было выставленно в service-акторе.
      val mctx1 = mctx0.copy(
        mads    = Seq(madCtx),
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
        replyTo   = Some(replyTo)
      )
      sendCommand(cmd)
    }

    protected def _renderError(ans: Answer) = renderError("error.adv.ext.js.refused", ans.ctx2.error)

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
            _renderError(ans)
            harakiri()

          // JS'у недостаточно данных в контексте для создания публикации.
          case AnswerStatuses.FillContext =>
            if (fillCtxTry >= ExtTargetActor.MAX_FILL_CTX_TRIES) {
              error("Too many fill ctx tries. Stopping. Last answer was:\n  " + ans)
              _renderError(ans)
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
          picUrlOpt.isEmpty || picUrlOpt.exists(_.isEmpty)
        }
        if (maybeAdNeedNewPicUrl.nonEmpty) {
          // Требуется выставить новую ссылку на картинку в контекст
          val mad1 = maybeAdNeedNewPicUrl.get
          // Подбираем запрошенный/дефолтовый размер картинки и закидываем в _customArgs.
          val sizeAliasOpt = mad1.picture.flatMap(_.size)
          val sz1: INamedSize2di = {
            sizeAliasOpt
              .flatMap(service.postImgSzWithName)
              .getOrElse { service.advPostMaxSz( args.target.target.url ) }
          }
          trace(s"Requested to switch img size to $sz1 ($sizeAliasOpt)")
          _customArgs = _customArgs.copy(
            adRenderMaxSz = sz1
          )
          val madCtx1 = mad1.copy(
            picture = Some( _customArgs.jsPicCtx )
          )
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

  /**
   * Произвести заливку картинки на удалённый сервис с помощью ссылки.
   * @param uploadCtx Данные для s2s-загрузки.
   */
  class S2sRenderAd2ImgState(val mctx0: MJsCtx, madId: String, uploadCtx: S2sPictureUpload) extends RenderAd2ImgStateT {
    override def _mad = mad
    override def _adRenderArgs = _customArgs.adRenderArgs
    override def rendererError(ex: Throwable): Unit = {
      error(s"[$replyTo] Failed to render ad[${args.qs.adId}] into picture", ex)
      val err = ErrorInfo(
        msg  = "error.sio.internal",
        info = Some(s"[$replyTo] ${ex.getMessage}")
      )
      val rargs = evtRenderArgs(EventTypes.AdvExtTgError, err)
      renderEventReplace(rargs)
    }

    override def handleImgOk(okRes: Ad2ImgRenderOk): Unit = {
      val nextState = new S2sPutPictureState(mctx0, madId, uploadCtx, okRes.imgBytes)
      become(nextState)
    }
  }


  /** Состояние отсылки запроса сохранения картинки на удалённый сервер. */
  class S2sPutPictureState(val mctx0: MJsCtx, madId: String, uploadCtx: S2sPictureUpload, imgBytes: Array[Byte])
    extends S2sMpUploadStateT {

    override def upUrl = uploadCtx.url

    override def mkUpArgs: MpUploadArgs = {
      val fmt = imgFmt
      val upPart = UploadPart(
        data      = imgBytes,
        name      = uploadCtx.partName,
        fileName  = args.qs.adId + "-" + mad.versionOpt.getOrElse(0L) + "." + fmt.name,
        ct        = fmt.mime
      )
      MpUploadArgs(
        parts = Seq(upPart),
        url   = Some(uploadCtx.url)
      )
    }

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
    override def renderImgUploadRefused(refused: UploadRefusedException): Unit = {
      super.renderImgUploadRefused(refused)
      LOGGER.error(s"Cannot load image to remote server: " + refused.getMessage)
    }

    /** Не удалось связаться с запрошенным сервером. */
    override def renderImgUploadFailed(ex: Throwable): Unit = {
      super.renderImgUploadFailed(ex)
      LOGGER.error(s"[$replyTo] Failed to POST image to ${uploadCtx.url} as part '${uploadCtx.partName}'", ex)
    }

    def uploadedOk(wsResp: WSResponse): Unit = {
      debug(s"successfully POSTed ad image to remote server: HTTP ${wsResp.statusText}")
      trace(s"Remote server response is:\n ${wsResp.body}")
      val mctx2 = withPictureBody(wsResp.body)
      val nextState = new FillContextState(mctx2)
      become(nextState)
    }

    override def uploadFailed(ex: Throwable): Unit = {
      super.uploadFailed(ex)
      harakiri()
    }
  }

}
