package io.suggest.swfs.client

import com.google.inject.ImplementedBy
import io.suggest.swfs.client.play.SwfsClientWs
import io.suggest.swfs.client.proto.assign.{AssignResponse, AssignRequest}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.10.15 22:30
 * Description: Интерфейс высокоуровнего клиента к seaweedfs.
 */
@ImplementedBy(classOf[SwfsClientWs])
trait ISwfsClient {

  /**
   * Получить новый fid для последующего аплоада файла.
   * @return Фьючерс с результатом.
   */
  def assign(args: AssignRequest = AssignRequest())
            (implicit ec: ExecutionContext): Future[AssignResponse]

}
