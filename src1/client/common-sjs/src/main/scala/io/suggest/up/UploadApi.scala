package io.suggest.up

import io.suggest.file.MJsFileInfo
import io.suggest.n2.media.MFileMeta
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.routes.PlayRoute
import io.suggest.url.MHostUrl
import org.scalajs.dom.FormData
import play.api.libs.json.Json

import scala.concurrent.Future

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
  def doFileUpload(upData: MHostUrl, file: MJsFileInfo, ctxIdOpt: Option[String]): HttpRespMapped[MUploadResp]

  // chunk(), hasChunk() пока неявно реализуются внутри resumable.js.

}


/** Реализация [[IUploadApi]] поверх HTTP/XHR. */
class UploadApiHttp extends IUploadApi {

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
        headers = HttpReqData.headersJsonSendAccept,
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


  override def doFileUpload(upData: MHostUrl, file: MJsFileInfo, ctxIdOpt: Option[String]): HttpRespMapped[MUploadResp] = {
    // Отправить как обычно, т.е. через multipart/form-data:
    val formData = new FormData()
    formData.append(
      name      = UploadConstants.MPART_FILE_FN,
      value     = file.blob,
      // TODO orNull: Может быть надо undefined вместо null?
      blobName  = file.fileName.orNull
    )

    val req = HttpReq(
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
          HttpConst.Headers.ACCEPT -> MimeConst.APPLICATION_JSON
        ),
        body    = formData
      )
    )

    HttpClient
      .execute( req )
      .mapResult {
        _.reloadIfUnauthorized()
         .successIf200
         .unJson[MUploadResp]
      }
  }

}
