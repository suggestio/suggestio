package io.suggest.swfs.client.play

import io.suggest.ahc.upload.{MpUploadArgs, MpUploadSupportDflt}
import io.suggest.swfs.client.proto.put.{PutResponse, IPutRequest}
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 13:01
 * Description: Аддон для поддержки отправки файла в volume.
 * Нужно делать PUT или POST запрос, multipart, где файл в поле "file".
 */

trait Put extends ISwfsClientWs with MpUploadSupportDflt {

  override def getUploadUrl(args: MpUploadArgs): String = {
    args.url.get
  }

  override def isRespOk(args: MpUploadArgs, resp: WSResponse): Boolean = {
    SwfsClientWs.isStatus2xx( resp.status )
  }

  override def mpFieldNameDflt = "file"


  override def put(req: IPutRequest): Future[PutResponse] = {
    val uplArgs = uploadArgsSimple(
      file = req.file,
      ct   = req.contentType,
      url  = Some( req.toUrl ),
      fileName = req.getFileName
    )
    val startMs = System.currentTimeMillis()
    val putFut = mpUpload(uplArgs)

    lazy val logPrefix = s"put($startMs):"
    LOGGER.trace(s"$logPrefix Started PUT file for $req, file size = ${req.file.length()} bytes.")

    // Залоггировать ошибки
    putFut.failed.foreach { ex =>
      LOGGER.error(s"$logPrefix Failed to PUT: $req", ex)
    }

    // Десериализовать успешный ответ.
    for {
      wsResp <- putFut
    } yield {
      LOGGER.trace(s"$logPrefix Success, took ${System.currentTimeMillis() - startMs} ms\n ${wsResp.body}")
      wsResp.json
        .as[PutResponse]
    }
  }

}
