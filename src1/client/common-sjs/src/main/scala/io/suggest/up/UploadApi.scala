package io.suggest.up

import diode.ModelRO
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.ctx.ICtxIdStrOpt
import io.suggest.file.{MJsFileInfo, MSrvFileInfo}
import io.suggest.file.up.{MFile4UpProps, MUploadResp}
import io.suggest.js.UploadConstants
import io.suggest.pick.MimeConst
import io.suggest.proto.HttpConst
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.url.MHostUrl
import org.scalajs.dom.FormData
import org.scalajs.dom.ext.AjaxException
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
  def doFileUpload(upData: MHostUrl, file: MJsFileInfo): Future[MSrvFileInfo]

}


/** Реализация [[IUploadApi]] поверх HTTP/XHR. */
class UploadApiHttp[Conf <: ICtxIdStrOpt]( confRO: ModelRO[Conf] ) extends IUploadApi {

  /** Код тела подготовки к аплоаду и декодинга результата по HTTP.
    *
    * @param route Роута.
    * @param file4UpProps Данные файла.
    * @return Фьючерс с ответом сервера.
    */
  override def prepareUpload(route: Route, file4UpProps: MFile4UpProps): Future[MUploadResp] = {
    for {
      // Запустить XHR...
      respJson <- {
        val H = HttpConst.Headers
        val applicationJson = MimeConst.APPLICATION_JSON
        Xhr.send(
          route = route,
          headers = Seq(
            H.ACCEPT        -> applicationJson,
            H.CONTENT_TYPE  -> applicationJson    // TODO + "; charset=utf8" ?
          ),
          body = Json.toJson(file4UpProps).toString()
        )
          .map { xhr  =>
            xhr.responseText
          }
          // 20х и 406 содержат body в одинаковом формате. Перехватить HTTP Not acceptable:
          .recover {
            // Сервер разные коды прислывает, но мы сами коды игнорим, важен - контент.
            case aex: AjaxException if aex.xhr.status == HttpConst.Status.NOT_ACCEPTABLE =>
              aex.xhr.response.asInstanceOf[String]
          }
      }
    } yield {
      // Распарсить ответ.
      Json
        .parse(respJson)
        .as[MUploadResp]
    }
  }


  override def doFileUpload(upData: MHostUrl, file: MJsFileInfo): Future[MSrvFileInfo] = {
    // Отправить как обычно, т.е. через multipart/form-data:
    val fd = new FormData()
    fd.append(
      name      = UploadConstants.MPART_FILE_FN,
      value     = file.blob,
      // TODO orNull: Может быть надо undefined вместо null?
      blobName  = file.fileName.orNull
    )

    for {
      // Запускаем XHR...
      xhr <- Xhr.sendRaw(
        method  = HttpConst.Methods.POST,
        // TODO Здесь дописывается &c=ctxId в хвост ссылки. А надо организовать сборку URL через jsRoutes. Для этого надо вместо ссылки брать подписанную JS-модель.
        url     = "//" + upData.host + upData.relUrl + confRO.value.ctxIdOpt.fold(""){ ctxId => "&c=" + ctxId },
        headers = Seq(
          HttpConst.Headers.ACCEPT        -> MimeConst.APPLICATION_JSON,
          HttpConst.Headers.CONTENT_TYPE  -> MimeConst.MULTIPART_FORM_DATA
        ),
        body    = fd
      )

    } yield {
      // Ответ сервера надо бы распарсить.
      Json
        .parse( xhr.responseText )
        .as[MSrvFileInfo]
    }
  }

}