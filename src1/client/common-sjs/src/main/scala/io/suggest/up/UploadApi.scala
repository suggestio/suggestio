package io.suggest.up

import diode.ModelRO
import io.suggest.ctx.ICtxIdStrOpt
import io.suggest.file.MJsFileInfo
import io.suggest.file.up.{MFile4UpProps, MUploadResp}
import io.suggest.js.UploadConstants
import io.suggest.pick.MimeConst
import io.suggest.proto.HttpConst
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.{HttpReq, HttpReqData, Xhr}
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

  def conf: ICtxIdStrOpt

  /** Подготовка к аплоаду: запрос реквизитов для аплоада с сервера.
    *
    * @param route Роута, за которой скрыт prepareUpload-экшен..
    * @param file4UpProps Данные по файлу, который планируется загружать.
    * @return Фьючерс с ответом сервера.
    */
  def prepareUpload(route: Route, file4UpProps: MFile4UpProps): Future[MUploadResp]


  /** Произвести непосредственную заливку файла на сервер.
    *
    * @param upData Реквизиты для связи.
    * @param file Данные по файлу.
    * @return Фьючерс с ответом сервера.
    */
  def doFileUpload(upData: MHostUrl, file: MJsFileInfo): Future[MUploadResp]

}


/** Реализация [[IUploadApi]] поверх HTTP/XHR. */
class UploadApiHttp[Conf <: ICtxIdStrOpt]( confRO: ModelRO[Conf] ) extends IUploadApi {

  override def conf: ICtxIdStrOpt = confRO.value

  /** Код тела подготовки к аплоаду и декодинга результата по HTTP.
    *
    * @param route Роута.
    * @param file4UpProps Данные файла.
    * @return Фьючерс с ответом сервера.
    */
  override def prepareUpload(route: Route, file4UpProps: MFile4UpProps): Future[MUploadResp] = {
    val req = HttpReq.routed(
      route = route,
      data = HttpReqData(
        headers = HttpReqData.headersJsonSendAccept,
        body = Json.toJson(file4UpProps).toString()
      )
    )
    val S = HttpConst.Status
    Xhr.execute( req )
      .respAuthFut
      .successIfStatus( S.CREATED, S.ACCEPTED, S.NOT_ACCEPTABLE )
      .unJson[MUploadResp]
  }


  override def doFileUpload(upData: MHostUrl, file: MJsFileInfo): Future[MUploadResp] = {
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
          conf.ctxIdOpt.fold(""){ ctxId => "&c=" + ctxId }
      },
      data = HttpReqData(
        headers = Map(
          HttpConst.Headers.ACCEPT -> MimeConst.APPLICATION_JSON
        ),
        body    = formData
      )
    )

    Xhr.execute( req )
      .respAuthFut
      .successIf200
      .unJson[MUploadResp]
  }

}
