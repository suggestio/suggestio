package io.suggest.swfs.client.play

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
  def data2wsBody(data: Enumerator[Array[Byte]]): WSBody = {
    if (STREAMED_BODY_SUPPORTED) {
      StreamedBody(data)
    } else {
      // TODO InMemoryBody(), код сверстки массивов взять из CassandraStorage, переместив его в IteeUtil в common-play или ещё куда-нить.
      ???
    }
  }

}

trait Put extends ISwfsClientWs {

  override def put(req: IPutRequest)(implicit ec: ExecutionContext): Future[PutResponse] = {
    lazy val logPrefix = s"put(${req.fid}):"
    val url = req.toUrl
    val method = "PUT"
    ws.url( url )
      .withBody( Put.data2wsBody(req.data) )
      .execute(method)
      .filter { resp =>
        LOGGER.trace( s"$logPrefix $method $url => ${resp.status} ${resp.statusText}\n ${resp.body}" )
        SwfsClientWs.isStatus2xx( resp.status )
      }
      .map { resp =>
        resp.json
          .validate[PutResponse]
          .get
      }
  }

}
