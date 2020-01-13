package io.suggest.n2.media.storage
import java.io.File
import java.nio.file.{Paths, ReadOnlyFileSystemException}

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import io.suggest.compress.MCompressAlgo
import io.suggest.file.MimeUtilJvm
import io.suggest.fio.{IDataSource, WriteRequest}
import io.suggest.pick.MimeConst
import io.suggest.routes.RoutesJvmConst
import io.suggest.url.MHostInfo
import io.suggest.util.logs.MacroLogsImplLazy
import javax.inject.Inject
import play.api.Environment
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.01.2020 15:17
  * Description: Доступ к ассетам как к media-стораджу.
  * Используется для подгонки узла-файла под
  */
class ClassPathStorage @Inject()(
                                  injector: Injector,
                                )
  extends IMediaStorageStatic
  with MacroLogsImplLazy
{

  private lazy val env = injector.instanceOf[Environment]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  /** Classpath не доступен для записи.
    * По-хорошему, нужно это как-то разруливать на уровне типов компилятора, чтобы было сразу всё ясно.
    */
  private def _readOnlyException =
    throw new ReadOnlyFileSystemException

  private def _fileOf(ptr: MStorageInfoData) =
    new File(RoutesJvmConst.ASSETS_PUBLIC_ROOT, ptr.data)

  def readSync(ptr: MStorageInfoData, acceptCompression: Iterable[MCompressAlgo]): IDataSource = {
    lazy val logPrefix = s"readSync(${ptr.data}):"

    // Нужно залесть в getResource*() или иные методы, и оттуда достать всё необходимое.
    val file = _fileOf(ptr)
    val cpPath = file.getPath

    // Найти пожатый вариант согласно запрошенному acceptCompression.
    (for {
      compressAlgoOpt <- {
        // Если в acceptCompression пусто, то нет смысла проверять файловые расширения на сжатость
        val isMayBeCompressed = acceptCompression.nonEmpty && {
          // Клиент допускает какой-либо пожатый ответ. Проверить, может ли быть сжатым файл с таким именем?
          val fileName = file.getName
          val fileExtensionPos = fileName.lastIndexOf('.')
          (fileExtensionPos > 0) && {
            // Сверить расширение искомого файла: может файл всегда сжат?
            val fileExt = fileName.substring( fileExtensionPos )
            val isNeverCompress = fileExt.matches("(?i)\\.(apk|ip(a|sw)|zip|woff2?|jpe?g|png|gif|ico|gz|br|md5|sha1)$")
            !isNeverCompress
          }
        }
        val last = Iterator.single( Option.empty[MCompressAlgo] )
        // Запрещать поиск сжатых версий файла (ресурса), если имя файла (ресурса) намекает, что формат файла всегда сжатый.
        if (isMayBeCompressed && acceptCompression.nonEmpty) {
          acceptCompression
            .iterator
            .map(Some.apply) ++ last
        } else {
          last
        }
      }

      compressedPath = compressAlgoOpt.fold(cpPath) {
        compAlgo =>
          s"${cpPath}.${compAlgo.fileExtension}"
      }

      res <- env.resource( compressedPath ).iterator
    } yield {
      (compressedPath, res, compressAlgoOpt)
    })
      // Взять первый попавшийся доступный ресурс, посжатый или нет:
      .nextOption()
      .fold [IDataSource] {
        // Не найден ожидаемый ресурс в classpath. Надо ругаться:
        val msg = s"$logPrefix Resource not found in assets/classpath: $cpPath"
        LOGGER.warn(msg)
        throw new NoSuchElementException(msg)

      } { case (resCpPath, resUrl, compAlgoOpt) =>
        val path = Paths.get( resUrl.toURI )
        LOGGER.trace(s"$logPrefix Serving resource $resCpPath compress=${compAlgoOpt getOrElse ""}: $path")

        new IDataSource {
          override lazy val contentType: String = {
            // TODO Opt определять MIME-тип на основе имени запрошенного файла оригинала? По идее, в CLASSPATH ошибочного мусора быть не должно.
            Try {
              MimeUtilJvm.probeContentType( path )
            }
              .fold(
                {ex =>
                  LOGGER.error(s"$logPrefix Failed to detect MIME for resource $resCpPath", ex)
                  MimeConst.APPLICATION_OCTET_STREAM
                },
                {mimeOpt =>
                  mimeOpt getOrElse {
                    LOGGER.warn(s"$logPrefix Cannot detect content type for classpath path: $path")
                    MimeConst.APPLICATION_OCTET_STREAM
                  }
                }
              )
          }

          override def data: Source[ByteString, _] = {
            StreamConverters.fromInputStream(
              () => resUrl.openStream(),
            )
          }

          override lazy val sizeB = resUrl.openConnection().getContentLengthLong
          override def compression = compAlgoOpt
        }
      }
  }

  /** Асинхронное поточное чтение хранимого файла.
    *
    * @param ptr Описание цели.
    * @param acceptCompression Допускать возвращать ответ в сжатом формате.
    * @return Поток данных блоба + сопутствующие метаданные.
    */
  override def read(ptr: MStorageInfoData, acceptCompression: Iterable[MCompressAlgo]): Future[IDataSource] =
    Future( readSync(ptr, acceptCompression) )


  def isExistsSync(ptr: MStorageInfoData): Boolean = {
    val cpPath = _fileOf(ptr).getPath
    env.resource( cpPath ).nonEmpty
  }

  /** Есть ли в хранилище текущий файл? */
  override def isExist(ptr: MStorageInfoData): Future[Boolean] =
    Future( isExistsSync(ptr) )


  override def delete(ptr: MStorageInfoData): Future[_] =
    _readOnlyException

  override def write(ptr: MStorageInfoData, writeRequest: WriteRequest): Future[_] =
    _readOnlyException

  override def assignNew(): Future[MAssignedStorage] =
    _readOnlyException


  override def getStorageHost(ptr: MStorageInfoData): Future[Seq[MHostInfo]] =
    Future.successful( Nil )

  override def getStoragesHosts(ptrs: Iterable[MStorageInfoData]): Future[Map[MStorageInfoData, Seq[MHostInfo]]] =
    Future.successful( Map.empty )

}
