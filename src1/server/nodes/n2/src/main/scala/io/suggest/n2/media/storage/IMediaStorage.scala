package io.suggest.n2.media.storage

import javax.inject.{Inject, Singleton}
import io.suggest.compress.MCompressAlgo
import io.suggest.fio.{IDataSource, MDsReadArgs, WriteRequest}
import io.suggest.n2.media.storage.swfs.SwfsStorage
import io.suggest.url.MHostInfo
import play.api.inject.Injector

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 18:53
 * Description: Данные по backend-хранилищу, задействованному в хранении файлов.
 */
@Singleton
final class IMediaStorages @Inject() (
                                       injector  : Injector
                                     ) {

  /** Инжекция инсанса клиента. */
  def _clientInstance(storage: MStorage): IMediaStorageStatic = {
    storage match {
      case MStorages.SeaWeedFs =>
        injector.instanceOf[SwfsStorage]
      case MStorages.ClassPathResource =>
        injector.instanceOf[ClassPathStorage]
    }
  }

  /** Карта инстансов статических моделей поддерживаемых media-сторэджей.
    * Т.к. инстансы клиентов для картинок - сингтоны и часто дёргаются,
    * смысл тревожить инжектор на каждый чих - мало. Кэшируем инстансы в мапе. */
  lazy val _STORAGES_MAP: Map[MStorage, IMediaStorageStatic] = {
    MStorages.values
      .iterator
      .map { mStorType =>
        mStorType -> _clientInstance( mStorType )
      }
      .toMap
  }

  def client(storage: MStorage): IMediaStorageStatic =
    _STORAGES_MAP(storage)

}


/** Интерфейс для статической v2-модели, которое без замудрёного ООП. */
trait IMediaStorageStatic {

  /** Может ли система читать range'ы, а не данные целиком. */
  def canReadRanges: Boolean

  /** Асинхронное поточное чтение хранимого файла.
    *
    * @return Поток данных блоба + сопутствующие метаданные.
    */
  def read(args: MDsReadArgs): Future[IDataSource]

  /**
    * Запустить асинхронное стирание контента в backend-хранилище.
    * @return Фьючерс для синхронизации.
    */
  def delete(ptr: MStorageInfoData): Future[_]

  /** Выполнить сохранение (стриминг) блоба в хранилище.
    * @param ptr Асинхронный поток данных.
    * @return Фьючерс для синхронизации.
    */
  def write(ptr: MStorageInfoData, writeRequest: WriteRequest): Future[_]

  /** Есть ли в хранилище текущий файл? */
  def isExist(ptr: MStorageInfoData): Future[Boolean]

  /**
    * Подготовить новый указатель в текущем хранилище для загрузки очередного блобика.
    * @return Фьючерс с инстансом свежего указателя.
    */
  def assignNew(): Future[MAssignedStorage]

  /** Вернуть данные Assigned storage для указанных метаданных хранилища.
    *
    * @param ptr Media-указатель.
    * @return Фьючерс с инстансом MAssignedStorage.
    */
  def getStorageHost(ptr: MStorageInfoData): Future[Seq[MHostInfo]]

  /** Пакетный аналог для getAssignedStorage().
    *
    * @param ptrs Все указатели.
    * @return Фьючерс с картой хостов.
    */
  def getStoragesHosts(ptrs: Iterable[MStorageInfoData]): Future[Map[MStorageInfoData, Seq[MHostInfo]]]

}

