package io.suggest.up

import diode.ModelRO
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.ctx.ICtxIdStrOpt
import io.suggest.file.{MJsFileInfo, MSrvFileInfo}
import io.suggest.file.up.MUploadUrlData
import io.suggest.js.UploadConstants
import io.suggest.pick.MimeConst
import io.suggest.proto.HttpConst
import io.suggest.sjs.common.xhr.Xhr
import org.scalajs.dom.FormData
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 14:58
  * Description:
  */
trait IUploadApi {

  /** Произвести непосредственную заливку файла на сервер.
    *
    * @param upData Реквизиты для связи.
    * @param file Данные по файлу.
    * @return Фьючерс с ответом сервера.
    */
  def doFileUpload(upData: MUploadUrlData, file: MJsFileInfo): Future[MSrvFileInfo]

}


/** Реализация [[IUploadApi]] поверх HTTP/XHR. */
class UploadApiHttp[Conf <: ICtxIdStrOpt]( confRO: ModelRO[Conf] ) extends IUploadApi {

  override def doFileUpload(upData: MUploadUrlData, file: MJsFileInfo): Future[MSrvFileInfo] = {
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
