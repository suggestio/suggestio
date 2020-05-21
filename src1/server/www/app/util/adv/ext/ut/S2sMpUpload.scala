package util.adv.ext.ut

import io.suggest.ahc.upload.{MpUploadArgs, UploadRefusedException}
import io.suggest.di.IWsClient
import io.suggest.fsm.FsmActor
import io.suggest.primo.IToPublicString
import models.adv.ext.act.ExtActorEnv
import models.event.{ErrorInfo, MEventTypes}
import models.mproj.IMCommonDi
import play.api.libs.ws.WSResponse

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 15:52
 * Description: Поддержка сборки состояний для аплоада данных на сервис.
 */
trait S2sMpUpload
  extends FsmActor
    with ExtActorEnv
    with IWsClient
    with IMCommonDi
{
  import mCommonDi.ec

  trait S2sMpUploadStateT extends FsmState {
    /** Аплоад точно удался. */
    def uploadedOk(wsResp: WSResponse): Unit
    /** Аплоад не удался. */
    def uploadFailed(ex: Throwable): Unit = {
      LOGGER.error("Failed to upload file to remote server", ex)
    }

    /** Формирование данных для сборки тела multipart. */
    def mkUpArgs: MpUploadArgs

    /** Быстрый доступ к Service Upload API. */
    def mpUploadClient = serviceHelper.maybeMpUpload.get

    /** При переходе на это состояние надо запустить отправку картинки на удалённый сервер. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Собрать POST-запрос и запустить его на исполнение
      // 22.may.2015: Future{} чтобы удобно отрабатывать ранние синхронные исключения.
      // Если файл части запроса не удается прочитать, то будет exception ещё до начала запроса.
      val argsFut = Future {
        mkUpArgs
      }
      val upFut = argsFut flatMap { args =>
        mpUploadClient.mpUpload(args)
      }
      upFut.onComplete {
        case Success(wsResp) => self ! wsResp
        case other           => self ! other
      }
      upFut.onComplete { tryRes =>
        uploadCompleted(tryRes)
      }
    }

    def uploadCompleted(res: Try[WSResponse]): Unit = {}

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
trait S2sMpUploadRender
  extends S2sMpUpload
    with AdvExtTargetActorUtil
{

  /** Состояние аплоада. */
  trait S2sMpUploadStateTErr extends super.S2sMpUploadStateT {
    
    /** Ссылка, которая была использована для аплоада. */
    // TODO Этот upUrl вообще мимо кассы и только для логгирования используется. Эта ссылка может быть не известна актору вообще.
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
      val rargs = evtRenderArgs(MEventTypes.AdvExtTgError, err)
      renderEventReplace(rargs)
      // Записать в инфу логи проблемы.
      _logRefused(refused)
    }

    protected def _logRefused(refused: UploadRefusedException): Unit = {
      LOGGER.warn {
        val sb = new StringBuilder(512)
        sb.append("uploadFailed():\n")
          .append("  ").append(refused.msg)
        for {
          (k, vs) <- refused.wsResp.headers
        } {
          sb.append("\n    ").append(k).append(": ")
          vs.foreach { v =>
            sb.append(v).append(';')
          }
        }
        sb.toString()
      }
    }

    /** Не удалось запустить/выполнить запрос. */
    def renderImgUploadFailed(ex: Throwable): Unit = {
      val err = ErrorInfo(
        msg  = "error.adv.ext.s2s.img.upload.failed",
        args = Seq(getDomain),
        info = Some(s"[$replyTo] ${IToPublicString.getPublicString(ex)}")
      )
      val rargs = evtRenderArgs(MEventTypes.AdvExtTgError, err)
      renderEventReplace(rargs)
    }

    override def uploadFailed(ex: Throwable): Unit = {
      super.uploadFailed(ex)
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
