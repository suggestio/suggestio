package io.suggest.swfs.client

import com.google.inject.ImplementedBy
import io.suggest.di.IExecutionContext
import io.suggest.swfs.client.play.SwfsClientWs
import io.suggest.swfs.client.proto.assign.{AssignRequest, AssignResponse}
import io.suggest.swfs.client.proto.delete.{DeleteRequest, DeleteResponse}
import io.suggest.swfs.client.proto.get.{GetRequest, GetResponse}
import io.suggest.swfs.client.proto.lookup.{LookupError, LookupRequest, ILookupResponse}
import io.suggest.swfs.client.proto.put.{PutRequest, PutResponse}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.10.15 22:30
 * Description: Интерфейс высокоуровнего клиента к seaweedfs.
 */
@ImplementedBy(classOf[SwfsClientWs])
trait ISwfsClient extends IExecutionContext {

  /**
   * Получить новый fid для последующего аплоада файла.
   *
   * @param args Параметры assign-запроса.
   * @return Фьючерс с результатом.
   */
  def assign(args: AssignRequest = AssignRequest()): Future[AssignResponse]


  /**
   * Отправка файла в хранилище.
   *
   * @param args Параметры PUT-запроса.
   * @return Фьючерс с распарсенным ответом сервера.
   */
  def put(args: PutRequest): Future[PutResponse]


  /**
   * Удаление файла.
   *
   * @param args Данные для запроса удаления.
   * @return Фьючерс с результатом удаления:
   *         None -- файл, подлежащий удалению, не найден.
   */
  def delete(args: DeleteRequest): Future[Option[DeleteResponse]]


  /**
   * Чтение файла из swfs-хранилища.
   *
   * @param args Аргументы GET-запроса.
   * @return Фьючерс с результатом запроса:
   *         None -- файл не найден.
   *         Some() с содержимым ответа.
   */
  def get(args: GetRequest): Future[Option[GetResponse]]


  /**
   * Поиск сетевого адреса volume-сервера по volume id.
   *
   * @param args Параметры запроса.
   * @return Фьючерс с результатом.
   *         Left() если ошибка какая-то.
   *         Right() с инфой по volume.
   */
  def lookup(args: LookupRequest): Future[Either[LookupError, ILookupResponse]]

  /**
   * Существует ли указанный файл в хранилище?
   *
   * @param args Экземпляр IGetRequest.
   * @return Фьючерс с true/false внутри.
   */
  def isExist(args: GetRequest): Future[Boolean]

}
