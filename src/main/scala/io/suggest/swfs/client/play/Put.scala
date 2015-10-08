package io.suggest.swfs.client.play

import io.suggest.itee.IteeUtil
import io.suggest.swfs.client.proto.put.{PutResponse, IPutRequest}
import io.suggest.util.MacroLogsDyn
import play.api.libs.iteratee.Enumerator
import play.api.libs.ws.{InMemoryBody, WSBody, StreamedBody}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 13:01
 * Description: Аддон для поддержки отправки файла в volume.
 * Нужно делать PUT или POST запрос, multipart, где файл в поле "file".
 */
object Put extends MacroLogsDyn {

  /** play 2.4.x и ранее имели загрушку, падающую в консрукторе, вместо нормальной реализации. */
  val STREAMED_BODY_SUPPORTED: Boolean = {
    try {
      StreamedBody( null )
      true
    } catch {
      case ex: NotImplementedError =>
        LOGGER.info("ws.StreamedBody request is not supported: " + ex.getMessage)
        false
    }
  }

  /** Приведение енумератора данных к валидному аргументу ws.withBody(). */
  def data2wsBody(data: Enumerator[Array[Byte]])(implicit ec: ExecutionContext): Future[WSBody] = {
    if (STREAMED_BODY_SUPPORTED) {
      val sb = StreamedBody(data)
      Future successful sb

    } else {
      IteeUtil.dumpBlobs(data)
        .map { InMemoryBody.apply }
    }
  }

}

trait Put extends ISwfsClientWs {

  override def put(req: IPutRequest)(implicit ec: ExecutionContext): Future[PutResponse] = {
    val wsBodyFut = Put.data2wsBody( req.data )
    lazy val logPrefix = s"put(${req.fid}):"
    val url = req.toUrl
    val method = "PUT"
    for {
      wsBody <- wsBodyFut

      resp <- {
        val fut = ws.url(url)
          .withBody( wsBody )
          .execute(method)
        // В фоне залоггировать результат запроса, если трассировка активна.
        if (LOGGER.underlying.isTraceEnabled) {
          fut onSuccess { case resp =>
            LOGGER.trace(s"$logPrefix $method $url => ${resp.status} ${resp.statusText}\n ${resp.body}")
          }
        }
        // Вернуть фьючерс запроса.
        fut
      }

      if SwfsClientWs.isStatus2xx( resp.status )

    } yield {
      resp.json
        .validate[PutResponse]
        .get
    }
  }

}
