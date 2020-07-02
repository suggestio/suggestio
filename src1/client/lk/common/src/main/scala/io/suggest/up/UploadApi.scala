package io.suggest.up

import io.suggest.file.MJsFileInfo
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.media.MFileMeta
import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.routes.PlayRoute
import io.suggest.url.MHostUrl
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom
import org.scalajs.dom.FormData
import org.scalajs.dom.ext.Ajax
import play.api.libs.json.Json

import scala.concurrent.{Future, Promise}
import scala.util.Failure

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 14:58
  * Description: UploadApi -- client-side API for server-side Upload controller.
  */
trait IUploadApi {

  /** Подготовка к аплоаду: запрос реквизитов для аплоада с сервера.
    *
    * @param route Роута, за которой скрыт prepareUpload-экшен..
    * @param fileMeta Данные по файлу, который планируется загружать.
    * @return Фьючерс с ответом сервера.
    */
  def prepareUpload(route: PlayRoute, fileMeta: MFileMeta): Future[MUploadResp]


  /** Произвести непосредственную заливку файла на сервер.
    *
    * @param upData Реквизиты для связи.
    * @param file Данные по файлу.
    * @return Фьючерс с ответом сервера.
    */
  def doFileUpload(upData: MHostUrl, file: MJsFileInfo, ctxIdOpt: Option[String] = None,
                   onProgress: Option[ITransferProgressInfo => Unit] = None): IHttpResultHolder[MUploadResp]

  // chunk(), hasChunk() пока неявно реализуются внутри resumable.js.

}


/** Реализация [[IUploadApi]] поверх HTTP/XHR. */
class UploadApiHttp extends IUploadApi with Log {

  /** Разрешать ли chunked upload? */
  final def ALLOW_CHUNKED = false

  /** Код тела подготовки к аплоаду и декодинга результата по HTTP.
    *
    * @param route Роута.
    * @param fileMeta Данные файла.
    * @return Фьючерс с ответом сервера.
    */
  override def prepareUpload(route: PlayRoute, fileMeta: MFileMeta): Future[MUploadResp] = {
    val req = HttpReq.routed(
      route = route,
      data = HttpReqData(
        headers = Map(
          HttpConst.Headers.accept( MimeConst.APPLICATION_JSON, MimeConst.TEXT_PLAIN ),
          HttpConst.Headers.CONTENT_TYPE -> MimeConst.APPLICATION_JSON,
        ),
        body = Json.toJson(fileMeta).toString()
      )
    )
    val S = HttpConst.Status
    HttpClient
      .execute( req )
      .respAuthFut
      .successIfStatus( S.CREATED, S.ACCEPTED, S.NOT_ACCEPTABLE )
      .unJson[MUploadResp]
  }


  /** Тут раздвоение реализаций: chunked-заливка или обычная заливка. */
  override def doFileUpload(upData: MHostUrl, file: MJsFileInfo, ctxIdOpt: Option[String] = None,
                            onProgress: Option[ITransferProgressInfo => Unit] = None): IHttpResultHolder[MUploadResp] = {

    // Фунция запуска основного реквеста. При наличии resumable.js, она запускается после окончания upload'а.
    def __doMainReq(formDataOpt: Option[Ajax.InputData]) = {
      HttpClient
        .execute(
          HttpReq(
            method = HttpConst.Methods.POST,
            url    = {
              // TODO Здесь дописывается &c=ctxId в хвост ссылки. А надо организовать сборку URL через jsRoutes. Для этого надо вместо ссылки брать подписанную JS-модель.
              HttpConst.Proto.CURR_PROTO +
                upData.host +
                upData.relUrl +
                ctxIdOpt.fold(""){ ctxId => "&c=" + ctxId }
            },
            data = HttpReqData(
              headers = Map(
                HttpConst.Headers.accept( MimeConst.APPLICATION_JSON, MimeConst.TEXT_PLAIN ),
              ),
              body = formDataOpt.orNull,
              onProgress = formDataOpt.flatMap(_ => onProgress),
            )
          )
        )
        .mapResult {
          _.reloadIfUnauthorized()
            .successIf200
            .unJson[MUploadResp]
        }
    }

    // Отправить как обычно, т.е. через multipart/form-data:
    val resumableOpt = if (ALLOW_CHUNKED) FlowjsUtil.tryFlowJs( upData ) else Failure(new NoSuchElementException)

    // Если resumable-объект собран, то надо переслать файл через resumable, а doFileUpload() дёрнуть без тела.
    resumableOpt.fold [IHttpResultHolder[MUploadResp]] (
      {ex =>
        // Не удалось инициализировать resumable.js, закачиваем по-старинке. Собрать обычное тело upload-запроса:
        if (ALLOW_CHUNKED)
          logger.error( ErrorMsgs.CHUNKED_UPLOAD_PREPARE_FAIL, ex, upData )

        val formData = new FormData()
        formData.append(
          name      = UploadConstants.MPART_FILE_FN,
          value     = file.blob,
          // TODO orNull: Может быть надо undefined вместо null?
          blobName  = file.fileName.orNull
        )

        __doMainReq( Some(formData) )
      },

      {resumable =>
        // TODO Тут нет отработки reloadIfUnauthorized() при заливке. Возможно, проверять сессию перед началом заливки?
        // Подписка на onProgress
        for (f <- onProgress)
          FlowjsUtil.subscribeProgress(resumable, file, f)

        // Подписка на onComplete
        val p = Promise[Unit]()
        FlowjsUtil.subscribeErrors(resumable, p)

        // Запустить аплоад.
        resumable.addFile( file.blob.asInstanceOf[dom.File] )

        // resumable.js: Если подписаться на complete до запуска, то событие срабатывает слишком рано.
        // TODO flow.js: Узнать, тоже страдает от этого?
        FlowjsUtil.subscribeComplete(resumable, p)

        resumable.upload()

        val resumableFut = p.future

        // Уродливый класс с реализацией последовательной сцепки всех реквестов:
        new IHttpRespMapped[MUploadResp] {
          // Максимально отложить запуск финального реквеста:
          lazy val mainReqHolder = __doMainReq( None )

          var _httpResultHolder: IHttpResultHolder[HttpResp] = new IHttpRespHolder {
            override def abortOrFail(): Unit =
              resumable.cancel()
            override lazy val resultFut = resumableFut.map { _ =>
              // По идее, этот код никогда не вызывается.
              new DummyHttpResp {
                override def status = HttpConst.Status.OK
                override def statusText = "OK"
              }
            }
          }

          override def httpResultHolder = _httpResultHolder

          override val resultFut: Future[MUploadResp] = {
            resumableFut.flatMap { _ =>
              val r2 = mainReqHolder
              _httpResultHolder = r2.httpResultHolder
              r2.resultFut
            }
          }
        }

      }
    )
  }


}
