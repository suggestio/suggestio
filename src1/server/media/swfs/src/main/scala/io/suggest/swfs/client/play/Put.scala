package io.suggest.swfs.client.play

import io.suggest.ahc.upload.{MpUploadArgs, MpUploadSupportDflt}
import io.suggest.proto.http.HttpConst
import io.suggest.swfs.client.proto.put.{PutRequest, PutResponse}
import play.api.libs.ws.WSResponse
import japgolly.univeq._

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
    SwfsClientWs.isStatus2xx( resp.status ) ||
    // Если файл перезалит, но не изменился, то weed возвращает 304:
    (resp.status ==* HttpConst.Status.NOT_MODIFIED)
  }

  override def mpFieldNameDflt = "file"


  override def put(req: PutRequest): Future[PutResponse] = {
    val uplArgs = uploadArgsSimple(
      file = req.file,
      ct   = req.contentType,
      url  = Some( req.toUrl ),
      fileName = req.getFileName
    )
    val startMs = System.currentTimeMillis()
    val putFut = mpUpload(uplArgs)

    lazy val logPrefix = s"put($startMs):"
    lazy val fileLen = req.file.length()
    LOGGER.trace(s"$logPrefix Started PUT file for $req, file size = $fileLen bytes.")

    // Залоггировать ошибки
    for (ex <- putFut.failed)
      LOGGER.error(s"$logPrefix Failed to PUT: $req", ex)

    // Десериализовать успешный ответ.
    for {
      wsResp <- putFut
    } yield {
      def tookMs = System.currentTimeMillis() - startMs

      if (wsResp.status ==* HttpConst.Status.NOT_MODIFIED) {
        LOGGER.warn(s"$logPrefix 304 Not modified. You've re-uploaded same file again ($fileLen bytes) into [${req.fid}] in $tookMs ms.\n file = ${req.file}")
        PutResponse( fileLen )
      } else {
        LOGGER.trace(s"$logPrefix Success, took $tookMs ms\n ${wsResp.body}")
        wsResp.json
          .as[PutResponse]
      }
    }
  }

}
