package io.suggest.swfs.client.play

import com.google.inject.Inject
import io.suggest.swfs.client.ISwfsClient
import io.suggest.swfs.client.proto.assign.{AssignRequest, AssignResponse}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.10.15 19:53
 * Description: Seaweedfs client для построения swfs-моделей на его базе.
 * Модели живут на стороне play, поэтому в качестве клиента жестко привязан WSClient c DI.
 */

class SwfsClientWs @Inject() (ws: WSClient) extends ISwfsClient {

  override def assign(args: AssignRequest)(implicit ec: ExecutionContext): Future[AssignResponse] = {
    ???
  }

}
