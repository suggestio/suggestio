package io.suggest.loc.geo.ipgeobase

import java.io._
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import javax.inject.Inject
import io.suggest.ahc.util.HttpGetToFile
import io.suggest.async.AsyncUtil
import io.suggest.es.model.{EsIndexUtil, IEsModelDiVal}
import io.suggest.util.JMXBase
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.elasticsearch.action.bulk.{BulkProcessor, BulkRequest, BulkResponse}
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.16 16:51
  * Description: Система импорта/обновления индексов IP GeoBase.
  */
class IpgbImporter @Inject() (
  mIndexes            : MIndexes,
  mCitiesTmpFactory   : MCitiesTmpFactory,
  mIpRangesTmpFactory : MIpRangesTmpFactory,
  httpGetToFile       : HttpGetToFile,
  asyncUtil           : AsyncUtil,
  mCommonDi           : IEsModelDiVal
)
  extends MacroLogsImpl
{

  import mCommonDi._
  import LOGGER._

  /** Ссылка для скачивания текущей базы. */
  private def ARCHIVE_DOWNLOAD_URL = configuration.getOptional[String]("ipgeobase.archive.url")
    .getOrElse("http://ipgeobase.ru/files/db/Main/geo_files.zip")

  /** Поверхностная проверка скачанного архива на профпригодность. */
  private def ARCHIVE_MAGIC_NUMBER = configuration.getOptional[Int]("ipgeobase.archive.magic")
    .getOrElse(0x504b0304)

  /** Минимально допустимая длина архива с данными. */
  private def ARCHIVE_MIN_LENGTH = configuration.getOptional[Int]("ipgeobase.archive.size.min")
    // Было 1`900`000, но это как-то жирновато совсем.
    .getOrElse(300000)

  /** Имя файл, содержащего диапазоны.*/
  private def IP_RANGES_FILENAME = configuration.getOptional[String]("ipgeobase.cidr_optim.filename")
    .getOrElse("cidr_optim.txt")

  /** Кодировка файла с дампами диапазонов. Исторически там win-1251.
    * Корректное значение названия кодировки надо брать из колонки java.nio нижеуказанной ссылки.
    * @see [[http://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html]]. */
  private def IP_RANGES_FILE_ENCODING = configuration.getOptional[String]("ipgeobase.cidr_optim.encoding")
    .getOrElse("windows-1251")

  /** Название файла, в котором лежит карта городов. */
  private def CITIES_FILENAME = configuration.getOptional[String]("ipgeobase.cities.filename")
    .getOrElse("cities.txt")

  /** Кодировка содержимого файла городов. */
  private def CITIES_FILE_ENCODING = configuration.getOptional[String]("ipgeobase.cities.encoding")
    .getOrElse(IP_RANGES_FILE_ENCODING)

  /** Кол-во item'ов в очереди на удаление. */
  private def BULK_QUEUE_LEN = 900


  /** Скачать файл с дампом базы в tmp. */
  def download(): Future[File] = {
    val dlUrlStr = ARCHIVE_DOWNLOAD_URL
    // TODO Нужно ограничивать max-size для возвращаемых данных. 5 метров макс. будет достаточно.
    val downloader = httpGetToFile.Downloader(dlUrlStr, followRedirects = true)
    val resultFut = downloader.request()
    for (ex <- resultFut.failed) {
      info(s"download(): Failed to fetch $dlUrlStr into file.", ex)
    }

    for {
      resp <- resultFut
      archiveFile = resp.file
      // Метод `>` не проверяет 200 OK. Нужно вручную проверить, что скачался именно архив.
      if {
        // Тестируем архив по методике http://www.java2s.com/Code/Java/File-Input-Output/DeterminewhetherafileisaZIPFile.htm
        !archiveFile.isDirectory  &&  archiveFile.canRead  &&  archiveFile.length() > ARCHIVE_MIN_LENGTH  && {
          val in = new DataInputStream(
            new BufferedInputStream(
              new FileInputStream(archiveFile)
            )
          )
          val magic = try {
            in.readInt()
          } finally {
            in.close()
          }
          val result = magic == ARCHIVE_MAGIC_NUMBER
          trace(s"download(): Testing fetched file ${archiveFile.getAbsolutePath}: $result")
          result
        }
      }
    } yield {
      trace(s"download(): Downloaded file size = ${archiveFile.length} bytes.")
      archiveFile
    }
  }


  /**
   * Распаковать архив с базой во временную директорию.
   * @param archiveFile Файл с дампом базы, подлежащий распаковке.
   * @return Директория с распакованными файлами.
   */
  def unpack(archiveFile: File): File = {
    val dirname = FilenameUtils.getBaseName(archiveFile.getName) + ".unpack"
    val dir = new File(archiveFile.getParentFile, dirname)
    if (!dir.mkdir()) {
      throw new RuntimeException("Cannot create directory " + dirname)
    }

    // В оригинале тут был try-catch, catch удалял директорию, но и никогда не вызывался.
    // Теперь же удаление директории в идёт рамках основной логики импорта.
    val cmdArgs = Array[String]("unzip", "-d", dir.getAbsolutePath, archiveFile.getAbsolutePath)
    val p = Runtime.getRuntime.exec(cmdArgs)
    val result = p.waitFor()
    if (result != 0) {
      throw new RuntimeException("Failed to unpack archive: return code = " + result)
    }
    dir
  }


  /** Сброка BulkProcessor'а для быстрого и массового импорта данных. */
  def createBulkProcessor(): BulkProcessor = {
    val counter = new AtomicInteger(0)

    val logPrefix = s"${getClass.getSimpleName}[${System.currentTimeMillis}]:"

    val listener = new BulkProcessor.Listener {
      /** Перед отправкой каждого bulk-реквеста... */
      override def beforeBulk(executionId: Long, request: BulkRequest): Unit = {
        trace(s"$logPrefix $executionId Before bulk import of ${request.numberOfActions} docs...")
      }

      /** Документы в очереди успешно удалены. */
      override def afterBulk(executionId: Long, request: BulkRequest, response: BulkResponse): Unit = {
        val countImported = response.getItems.length
        trace(s"$logPrefix $executionId Successfully imported $countImported, ${response.buildFailureMessage()}")
        counter.addAndGet(countImported)
      }

      /** Ошибка bulk-удаления. */
      override def afterBulk(executionId: Long, request: BulkRequest, failure: Throwable): Unit = {
        error(s"$logPrefix Failed to execute bulk req with ${request.numberOfActions} actions!", failure)
      }
    }

    // Собираем асинхронный bulk-процессор, т.к. элементов может быть ну очень много.
    BulkProcessor
      .builder(esClient, listener)
      .setBulkActions(BULK_QUEUE_LEN)
      .build()
  }


  /** Нужно скачать базу, распаковать, импортнуть города, импортнуть диапазоны, оптимизировать таблицы. */
  def updateIpBase(): Future[_] = {
    // Запустить скачивание архива с базой.
    val startedAtMs = System.currentTimeMillis()
    lazy val logPrefix = s"updateIpBase($startedAtMs):"

    // Запустить скачивание
    val downloadFut = download()

    // Получаем имена временных индексов и файлов за пределами for{} для возможности удаления их за пределами for{}:
    // Подготовить имя для нового индекса.
    val newIndexName = EsIndexUtil.newIndexName( mIndexes.INDEX_ALIAS_NAME )

    // Распаковка архива во временную директорию, удаление исходной папки.
    val unpackedDirFut = for (archiveFile <- downloadFut) yield {
      // Распаковать в директорию, удалить скачанный архив.
      trace(s"$logPrefix Unpacking downloaded archive $archiveFile")
      try {
        unpack(archiveFile)
      } finally {
        trace(s"$logPrefix Deleting downloaded archive $archiveFile")
        archiveFile.delete()
      }
    }

    // Теперь большой for, описывающий асинхронную логику заливки данных в базу.
    val doFut = for {
      // Дождаться успешной распаковки скачанного архива...
      unpackedDir <- unpackedDirFut

      // Создание нового ES-индекса
      createIndexFut = {
        debug(s"$logPrefix Will create new index: $newIndexName")
        mIndexes.createIndex(newIndexName)
      }

      // Дождаться готовности нового индекса...
      _ <- createIndexFut

      bp = createBulkProcessor()

      // Наконец импортировать данные в новый индекс.
      citiesImportFut = importCities(unpackedDir, bp, newIndexName)

      rangesImportFut = importIpRanges(unpackedDir, bp, newIndexName)

      _ <- citiesImportFut
      _ <- rangesImportFut

      // TODO Нужен отдельный ExecutionContext. А он пока только в play есть...
      _ <- Future {
        bp.awaitClose(1, TimeUnit.MINUTES)
      }(asyncUtil.singleThreadIoContext)

      // Привести индекс к рабочему состоянию.
      optimizeFut      = {
        mIndexes.optimizeAfterBulk(newIndexName)
      }
      // Параллельно: узнать имя старого индекса (старых индексов)
      oldIndexNamesFut = mIndexes.getAliasedIndexName()

      // Дождаться окончания оптимизациии и чтения имени старого индекса
      _             <- optimizeFut
      oldIndexNames <- oldIndexNamesFut

      // Переключить всю систему на новый индекс.
      _             <- mIndexes.resetIndexAliasTo(newIndexName)

      // Удалить старые индексы, если есть.
      _             <- Future.traverse(oldIndexNames)(mIndexes.deleteIndex)
    } yield {
      info(logPrefix + s"Done, took = ${System.currentTimeMillis - startedAtMs} ms.")
    }

    // Удалить временную директорию, когда всё будет закончено (не важно, ошибка или всё ок).
    doFut.onComplete { _ =>
      for (unpackedDir <- unpackedDirFut) {
        if (unpackedDir.exists()) {
          trace(s"$logPrefix Deleting temporary unpacked dir ${unpackedDir.getAbsolutePath} ...")
          FileUtils.deleteDirectory(unpackedDir)
        }
      }
    }

    // Если была ошибка во время doFut, то удалить новый индекс.
    for (ex <- doFut.failed) {
      error(s"$logPrefix FAILED. Deleting NEW index, it may be corrupted.", ex)
      mIndexes.deleteIndex(newIndexName)
    }

    // Вернуть основной фьючерс.
    doFut
  }


  /** Импорт таблицы городов. */
  def importCities(dir: File, bp: BulkProcessor, newIndexName: String): Future[_] = {
    val mCitiesTmp = mCitiesTmpFactory.create(newIndexName)
    val putMappingFut = mCitiesTmp.putMapping()

    val startedAtMs = System.currentTimeMillis
    lazy val logPrefix = s"importCities($startedAtMs):"
    val citiesFile = new File(dir, CITIES_FILENAME)

    debug(s"$logPrefix Bulk loading cities into ES...")

    val parsers = new CityParsers
    val p = parsers.cityLineP

    for (_ <- putMappingFut) yield {
      val linesTotal = Source.fromFile(citiesFile, CITIES_FILE_ENCODING)
        .getLines()
        .foldLeft(1) { (counter, cityLine) =>
          val pr = parsers.parse(p, cityLine)
          if (pr.successful) {
            val mcity = pr.get
            bp.add {
              mCitiesTmp.prepareIndexNoVsn(mcity).request()
            }
          } else {
            warn(s"${logPrefix}Failed to parse line $counter file=${citiesFile.getAbsolutePath}:\n$cityLine\n$pr")
          }
          if (counter % 300 == 0)
            trace(s"${logPrefix}Still importing... ($counter)")
          counter + 1
        }

      info(s"$logPrefix$linesTotal lines total. Took ${System.currentTimeMillis - startedAtMs} ms.")
    }
  }


  /**
   * Импорт диапазонов ip-адресов. Используется Pg COPY table FROM source.
   * @param dir директория с распакованными файлами.
   */
  def importIpRanges(dir: File, bp: BulkProcessor, newIndexName: String): Future[_] = {
    val mRangesTmp = mIpRangesTmpFactory.create(newIndexName)
    val putMappingFut = mRangesTmp.putMapping()

    val startedAtMs = System.currentTimeMillis
    lazy val logPrefix = s"importIpRanges($startedAtMs):"
    val cidrFile = new File(dir, IP_RANGES_FILENAME)
    debug(s"$logPrefix Will read $cidrFile ...")


    // Собираем инстанс парсера для всех строк:
    val parsers = new CidrParsers
    val p = parsers.cidrLineP

    for (_ <- putMappingFut) yield {
      // Делаем итератор для обхода неограниченно большого файла:
      val linesTotal = Source.fromFile(cidrFile, IP_RANGES_FILE_ENCODING)
        .getLines()
        .foldLeft(1) { (counter, cidrLine) =>
          val pr = parsers.parse(p, cidrLine)
          if (pr.successful) {
            val mrange = pr.get
            bp.add {
              mRangesTmp.prepareIndexNoVsn(mrange).request()
            }
          } else {
            warn(s"$logPrefix Failed to parse line $counter file=${cidrFile.getAbsolutePath}:\n$cidrLine\n$pr")
          }
          if (counter % 20000 == 0)
            trace(s"$logPrefix Still converting... ($counter)")
          counter + 1
        }

      info(s"$logPrefix $linesTotal lines converted total. Took ${System.currentTimeMillis - startedAtMs} ms.")
    }
  }

}


trait IpgbImporterJmxMBean {
  def updateIpBase(): String
}

final class IpgbImporterJmx @Inject()(
                                       injector                 : Injector,
                                       override implicit val ec : ExecutionContext
                                     )
  extends JMXBase
  with IpgbImporterJmxMBean
  with MacroLogsDyn
{

  override def jmxName = "io.suggest:type=ipgeobase,name=" + classOf[IpgbImporter].getSimpleName

  private def ipgbInjector = injector.instanceOf[IpgbImporter]

  override def updateIpBase(): String = {
    ipgbInjector.updateIpBase()
      .onComplete {
        case Success(r) =>
          LOGGER.debug("JMX: ipgeobase import done ok: " + r)
        case Failure(ex) =>
          LOGGER.error("JMX: ipgeobase import failed", ex)
      }
    "Started in background."
  }

}
