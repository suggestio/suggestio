package io.suggest.swfs.client.play

import io.suggest.ahc.upload.{IMpUploadArgs, MpUploadSupportDflt}
import io.suggest.swfs.client.proto.put.{PutResponse, IPutRequest}
import io.suggest.util.MacroLogsImpl
import play.api.libs.ws.WSResponse

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 13:01
 * Description: Аддон для поддержки отправки файла в volume.
 * Нужно делать PUT или POST запрос, multipart, где файл в поле "file".
 */

trait Put extends ISwfsClientWs {

  /** Реализация аплоадера на базе того же "движка", который используется
    * для загрузки картинок в твиттер и вк. */
  object Uploader extends MpUploadSupportDflt with MacroLogsImpl {
    override def getUploadUrl(args: IMpUploadArgs): String = {
      args.url.get
    }

    override def isRespOk(args: IMpUploadArgs, resp: WSResponse): Boolean = {
      SwfsClientWs.isStatus2xx( resp.status )
    }

    override def mpFieldNameDflt = "file"
  }


  override def put(req: IPutRequest)(implicit ec: ExecutionContext): Future[PutResponse] = {
    val uplArgs = Uploader.uploadArgsSimple(
      file = req.file,
      ct   = req.contentType,
      url  = Some( req.toUrl ),
      fileName = req.getFileName
    )
    for {
      wsResp <- Uploader.mpUpload(uplArgs)
    } yield {
      wsResp.json
        .validate[PutResponse]
        .get
    }
  }

}
