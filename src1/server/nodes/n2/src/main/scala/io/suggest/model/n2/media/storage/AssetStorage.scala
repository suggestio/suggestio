package io.suggest.model.n2.media.storage
import java.nio.file.ReadOnlyFileSystemException

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.suggest.compress.MCompressAlgo
import io.suggest.file.MimeUtilJvm
import io.suggest.fio.{IDataSource, WriteRequest}
import io.suggest.url.MHostInfo

import scala.concurrent.Future
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.01.2020 15:17
  * Description: Доступ к ассетам как к media-стораджу.
  * Используется для подгонки узла-файла под
  */
class AssetStorage extends IMediaStorageStatic {

  private def _readOnlyException =
    throw new ReadOnlyFileSystemException

  /** Асинхронное поточное чтение хранимого файла.
    *
    * @param ptr               Описание цели.
    * @param acceptCompression Допускать возвращать ответ в сжатом формате.
    * @return Поток данных блоба + сопутствующие метаданные.
    */
  override def read(ptr: MStorageInfoData, acceptCompression: Iterable[MCompressAlgo]): Future[IDataSource] = {
    new IDataSource {
      override lazy val contentType: String = {
        /*Try(
          MimeUtilJvm.probeContentType( file.toPath )
        )
          .getOrElse(  )*/
        ???
      }

      /** Содержимое файла.
        * Считается, что возвращает один и тот же инстанс Source[].
        */
      override def data: Source[ByteString, _] = ???

      /** Размер файла. */
      override def sizeB: Long = ???

      /** Если в запросе допускалось использование сжатия, то на выходе ответ может быть сжат. */
      override def compression: Option[MCompressAlgo] = ???
    }
    ???
  }

  /** Есть ли в хранилище текущий файл? */
  override def isExist(ptr: MStorageInfoData): Future[Boolean] = ???

  override def delete(ptr: MStorageInfoData): Future[_] =
    _readOnlyException

  override def write(ptr: MStorageInfoData, writeRequest: WriteRequest): Future[_] =
    _readOnlyException

  override def assignNew(): Future[MAssignedStorage] =
    _readOnlyException

  /** Вернуть данные Assigned storage для указанных метаданных хранилища.
    *
    * @param ptr Media-указатель.
    * @return Фьючерс с инстансом MAssignedStorage.
    */
  override def getStorageHost(ptr: MStorageInfoData): Future[Seq[MHostInfo]] = {
    // TODO Надо выводить текущий хост или иные хосты.
    Future.successful( Nil )
  }

  /** Пакетный аналог для getAssignedStorage().
    *
    * @param ptrs Все указатели.
    * @return Фьючерс с картой хостов.
    */
  override def getStoragesHosts(ptrs: Iterable[MStorageInfoData]): Future[Map[MStorageInfoData, Seq[MHostInfo]]] =
    Future.successful( Map.empty )

}
