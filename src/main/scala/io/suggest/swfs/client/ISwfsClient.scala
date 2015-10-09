package io.suggest.swfs.client

import com.google.inject.ImplementedBy
import io.suggest.swfs.client.play.SwfsClientWs
import io.suggest.swfs.client.proto.assign.{IAssignResponse, IAssignRequest, AssignRequest}
import io.suggest.swfs.client.proto.delete.{IDeleteResponse, IDeleteRequest}
import io.suggest.swfs.client.proto.put.{IPutResponse, IPutRequest}

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
   * @param args Параметры assign-запроса.
   * @return Фьючерс с результатом.
   */
  def assign(args: IAssignRequest = AssignRequest.empty)
            (implicit ec: ExecutionContext): Future[IAssignResponse]


  /**
   * Отправка файла в хранилище.
   * @param args Параметры PUT-запроса.
   * @return Фьючерс с распарсенным ответом сервера.
   */
  def put(args: IPutRequest)(implicit ec: ExecutionContext): Future[IPutResponse]


  /**
   * Удаление файла.
   * @param args Данные для удаления.
   * @return Фьючерс с результатом удаления.
   */
  def delete(args: IDeleteRequest)(implicit ec: ExecutionContext): Future[IDeleteResponse]

}
