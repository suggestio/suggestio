package util.adv.ut

import models.adv.ext.act.ExtActorEnv
import models.event.ErrorInfo
import models.mext.{UploadRefusedException, MpUploadArgs}
import play.api.libs.ws.WSResponse
import util.adv.IWsClient
import util.async.FsmActor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.EventTypes

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 15:52
 * Description: Поддержка сборки состояний для аплоада данных на сервис.
 */
trait S2sMpUpload extends FsmActor with ExtActorEnv with IWsClient {

  trait S2sMpUploadStateT extends FsmState {
    /** Аплоад точно удался. */
    def uploadedOk(wsResp: WSResponse): Unit
    /** Аплоад не удался. */
    def uploadFailed(ex: Throwable): Unit

    /** Формирование данных для сборки тела multipart. */
    def mkUpArgs: MpUploadArgs

    /** При переходе на это состояние надо запустить отправку картинки на удалённый сервер. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Собрать POST-запрос и запустить его на исполнение
      service.maybeMpUpload
        .get
        .mpUpload(mkUpArgs)
        .onComplete {
          case Success(wsResp) => self ! wsResp
          case other           => self ! other
        }
    }

    /** Ждём ответа от удалённого сервера с результатом загрузки картинки. */
    override def receiverPart: Receive = {
      // Успешно выполнена загрузка картинки на удалённый сервер. Надо перейти на следующее состояние.
      case wsResp: WSResponse =>
        uploadedOk(wsResp)

      // Запрос не удался или произошла ещё какая-то ошибка.
      case Failure(ex) =>
        uploadFailed(ex)
        
    }
  }

}


/** Реализация [[S2sMpUpload]] с рендером типичных ошибок на экран юзеру. */
trait S2sMpUploadRender extends S2sMpUpload with ExtTargetActorUtil {
  /** Состояние аплоада. */
  trait S2sMpUploadStateT extends super.S2sMpUploadStateT {
    
    /** Ссылка, которая была использована для аплоада. */
    def upUrl: String

    /** Удаленный сервер вернул ошибочный http-ответ. */
    def renderImgUploadRefused(refused: UploadRefusedException): Unit = {
      import refused.wsResp
      val _upUrl = upUrl
      val errMsg = new StringBuilder(wsResp.body.length + _upUrl.length + 128)
        .append("POST ").append(_upUrl).append('\n')
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

    /** Не удалось запустить/выполнить запрос. */
    def renderImgUploadFailed(ex: Throwable): Unit = {
      val err = ErrorInfo(
        msg  = "error.adv.ext.s2s.img.upload.failed",
        args = Seq(getDomain),
        info = Some(s"[$replyTo] ${ex.getMessage}")
      )
      val rargs = evtRenderArgs(EventTypes.AdvExtTgError, err)
      renderEventReplace(rargs)
    }

    override def uploadFailed(ex: Throwable): Unit = {
      // Если юзер обратится с описаловом, то там будет ключ ошибки. Экзепшен можно будет отследить по логам.
      ex match {
        case refused: UploadRefusedException =>
          renderImgUploadRefused(refused)
        case _ =>
          renderImgUploadFailed(ex)
      }
    }
  }

}
