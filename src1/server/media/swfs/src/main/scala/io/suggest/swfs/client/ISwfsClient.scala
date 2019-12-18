package io.suggest.swfs.client

import com.google.inject.ImplementedBy
import io.suggest.di.IExecutionContext
import io.suggest.swfs.client.play.SwfsClientWs
import io.suggest.swfs.client.proto.assign.{IAssignResponse, IAssignRequest, AssignRequest}
import io.suggest.swfs.client.proto.delete.{IDeleteResponse, IDeleteRequest}
import io.suggest.swfs.client.proto.get.{IGetRequest, GetResponse}
import io.suggest.swfs.client.proto.lookup.{ILookupError, ILookupResponse, ILookupRequest}
import io.suggest.swfs.client.proto.put.{IPutResponse, IPutRequest}

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
  def assign(args: IAssignRequest = AssignRequest()): Future[IAssignResponse]


  /**
   * Отправка файла в хранилище.
   *
   * @param args Параметры PUT-запроса.
   * @return Фьючерс с распарсенным ответом сервера.
   */
  def put(args: IPutRequest): Future[IPutResponse]


  /**
   * Удаление файла.
   *
   * @param args Данные для запроса удаления.
   * @return Фьючерс с результатом удаления:
   *         None -- файл, подлежащий удалению, не найден.
   */
  def delete(args: IDeleteRequest): Future[Option[IDeleteResponse]]


  /**
   * Чтение файла из swfs-хранилища.
   *
   * @param args Аргументы GET-запроса.
   * @return Фьючерс с результатом запроса:
   *         None -- файл не найден.
   *         Some() с содержимым ответа.
   */
  def get(args: IGetRequest): Future[Option[GetResponse]]


  /**
   * Поиск сетевого адреса volume-сервера по volume id.
   *
   * @param args Параметры запроса.
   * @return Фьючерс с результатом.
   *         Left() если ошибка какая-то.
   *         Right() с инфой по volume.
   */
  def lookup(args: ILookupRequest): Future[Either[ILookupError, ILookupResponse]]

  /**
   * Существует ли указанный файл в хранилище?
   *
   * @param args Экземпляр IGetRequest.
   * @return Фьючерс с true/false внутри.
   */
  def isExist(args: IGetRequest): Future[Boolean]

}
