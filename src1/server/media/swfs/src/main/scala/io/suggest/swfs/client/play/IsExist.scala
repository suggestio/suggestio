package io.suggest.swfs.client.play


import io.suggest.swfs.client.proto.get.GetRequest

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 21:34
 * Description: Аддон для проверки поддержки существования указанного файла в хранилище.
 */
trait IsExist extends ISwfsClientWs {

  override def isExist(args: GetRequest): Future[Boolean] = {
    val url = args.toUrl

    val startMs = System.currentTimeMillis()
    lazy val logPrefix = s"isExists($startMs):"
    LOGGER.trace(s"$logPrefix starting, args = $args, url = $url")

    // Make and handle request.
    val fut = for {
      wsResp  <- wsClient.url(url).head()
    } yield {
      val s = wsResp.status
      LOGGER.trace(s"$logPrefix success, took ${System.currentTimeMillis() - startMs} ms\n ${wsResp.body}")

      s match {
        case 200 =>
          true
        case 404 =>
          false
        case other =>
          throw new IllegalStateException(s"Unexpected HTTP $other returned from weed volume")
      }
    }

    // Log possible errors.
    for (ex <- fut.failed)
      LOGGER.error(s"$logPrefix Failed to make request for $args, HEAD $url ", ex)

    fut
  }

}
