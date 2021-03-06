package io.suggest.up

import io.suggest.file.MJsFileInfo
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.media.MFileMeta
import io.suggest.pick.{BlobJsUtil, MimeConst}
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.routes.PlayRoute
import io.suggest.url.MHostUrl
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
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

  // chunk(), hasChunk() пока неявно реализуются внутри flow.js.

}


/** Реализация [[IUploadApi]] поверх HTTP/XHR. */
class UploadApiHttp extends IUploadApi with Log {

  /** Разрешать ли chunked upload? */
  final def ALLOW_CHUNKED = true

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

    // Фунция запуска основного реквеста. При наличии flow.js, она запускается после окончания upload'а.
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
              // Убрать X-Requested-With, т.к. CORS по дефолту запрещает этот заголовок.
              config = HttpClientConfig(
                baseHeaders = Map.empty,
              ),
              // Все данные для data-сервера передаются в UPLOAD-ссылке или напрямую внутри кластера.
              // Подразумевается, что data-сервер всегда на отдельном домене/поддомене, где куки недоступны даже в браузере.
              credentials = Some(false),
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
    {
      if (ALLOW_CHUNKED && file.blob.size > UploadConstants.FLOWJS_DONT_USE_BELOW_BYTELEN)
        FlowjsUtil.tryFlowJs( upData )
      else
        Failure(new NoSuchElementException)
    }
      // Если flowjs-инстанс собран, то надо переслать файл через flowjs, а doFileUpload() дёрнуть без тела.
      .fold [IHttpResultHolder[MUploadResp]] (
        {ex =>
          // Не удалось инициализировать flow.js, закачиваем по-старинке. Собрать обычное тело upload-запроса:
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

        {flowjs =>
          // TODO Тут нет отработки reloadIfUnauthorized() при заливке. Возможно, проверять сессию перед началом заливки?
          // Подписка на onProgress
          for (f <- onProgress)
            FlowjsUtil.subscribeProgress(flowjs, file, f)

          // Подписка на onComplete
          val flowjsUploadP = Promise[Unit]()
          FlowjsUtil.subscribeErrors(flowjs, flowjsUploadP)

          // Запустить аплоад.
          // TODO Нужно заполнить blob полями name и lastModifiedTime.
          flowjs.addFile( BlobJsUtil.ensureBlobAsFile( file.blob ) )

          FlowjsUtil.subscribeComplete(flowjs, flowjsUploadP)

          flowjs.upload()

          val flowjsUploadFut = flowjsUploadP.future

          // Уродливый класс с реализацией последовательной сцепки всех реквестов:
          new IHttpRespMapped[MUploadResp] {
            // Максимально отложить запуск финального реквеста:
            lazy val mainReqHolder = __doMainReq( None )

            var _httpResultHolder: IHttpResultHolder[HttpResp] = new IHttpRespHolder {
              override def abortOrFail(): Unit =
                flowjs.cancel()
              override lazy val resultFut = flowjsUploadFut.map { _ =>
                // По идее, этот код никогда не вызывается.
                new DummyHttpResp {
                  override def status = HttpConst.Status.OK
                  override def statusText = "OK"
                }
              }
            }

            override def httpResultHolder = _httpResultHolder

            override val resultFut: Future[MUploadResp] = {
              flowjsUploadFut.flatMap { _ =>
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
