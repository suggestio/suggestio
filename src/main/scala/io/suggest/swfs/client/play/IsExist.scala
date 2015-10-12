package io.suggest.swfs.client.play

import io.suggest.swfs.client.proto.get.IGetRequest

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 21:34
 * Description: Аддон для проверки поддержки существования указанного файла в хранилище.
 */
trait IsExist extends ISwfsClientWs {

  override def isExist(args: IGetRequest)(implicit ec: ExecutionContext): Future[Boolean] = {
    val url = args.toUrl
    for {
      wsResp  <- ws.url(url).head()
    } yield {
      wsResp.status match {
        case 200 =>
          true
        case 404 =>
          false
        case other =>
          throw new IllegalStateException(s"Unexpected HTTP $other returned from weed volume")
      }
    }
  }

}
