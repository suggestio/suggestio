package io.suggest.loc.geo.ipgeobase

import java.io._
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import javax.inject.Inject
import io.suggest.ahc.util.HttpGetToFile
import io.suggest.async.AsyncUtil
import io.suggest.es.MappingDsl
import io.suggest.es.model.{EsIndexUtil, EsModel}
import io.suggest.util.JmxBase
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.elasticsearch.action.bulk.{BulkProcessor, BulkRequest, BulkResponse}
import play.api.Configuration
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
final class IpgbImporter @Inject() (
                                     injector            : Injector,
                                   )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mIndexes = injector.instanceOf[MIndexes]
  private lazy val mCitiesTmpFactory = injector.instanceOf[MCitiesTmpFactory]
  private lazy val mIpRangesTmpFactory = injector.instanceOf[MIpRangesTmpFactory]
  private lazy val httpGetToFile = injector.instanceOf[HttpGetToFile]
  private lazy val asyncUtil = injector.instanceOf[AsyncUtil]
  private lazy val configuration = injector.instanceOf[Configuration]
  implicit private lazy val esClient = injector.instanceOf[org.elasticsearch.client.Client]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

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
    for (ex <- resultFut.failed)
      LOGGER.info(s"download(): Failed to fetch $dlUrlStr into file.", ex)

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
          LOGGER.trace(s"download(): Testing fetched file ${archiveFile.getAbsolutePath}: $result")
          result
        }
      }
    } yield {
      LOGGER.trace(s"download(): Downloaded file size = ${archiveFile.length} bytes.")
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
        LOGGER.trace(s"$logPrefix $executionId Before bulk import of ${request.numberOfActions} docs...")
      }

      /** Документы в очереди успешно удалены. */
      override def afterBulk(executionId: Long, request: BulkRequest, response: BulkResponse): Unit = {
        val countImported = response.getItems.length
        LOGGER.trace(s"$logPrefix $executionId Successfully imported $countImported, ${response.buildFailureMessage()}")
        counter.addAndGet(countImported)
      }

      /** Ошибка bulk-удаления. */
      override def afterBulk(executionId: Long, request: BulkRequest, failure: Throwable): Unit = {
        LOGGER.error(s"$logPrefix Failed to execute bulk req with ${request.numberOfActions} actions!", failure)
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
    val newIndexName = EsIndexUtil.newIndexName( MIndexes.INDEX_ALIAS_NAME )

    // Распаковка архива во временную директорию, удаление исходной папки.
    val unpackedDirFut = for (archiveFile <- downloadFut) yield {
      // Распаковать в директорию, удалить скачанный архив.
      LOGGER.trace(s"$logPrefix Unpacking downloaded archive $archiveFile")
      try {
        unpack(archiveFile)
      } finally {
        LOGGER.trace(s"$logPrefix Deleting downloaded archive $archiveFile")
        archiveFile.delete()
      }
    }

    implicit val dsl = MappingDsl.Implicits.mkNewDsl

    // Теперь большой for, описывающий асинхронную логику заливки данных в базу.
    val doFut = for {
      // Дождаться успешной распаковки скачанного архива...
      unpackedDir <- unpackedDirFut

      // Создание нового ES-индекса
      createIndexFut = {
        LOGGER.debug(s"$logPrefix Will create new index: $newIndexName")
        esModel.createIndex(newIndexName, mIndexes.indexSettingsCreate)
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
      optimizeFut      = esModel.optimizeAfterBulk(newIndexName, mIndexes.indexSettingsAfterBulk)

      // Параллельно: узнать имя старого индекса (старых индексов)
      oldIndexNamesFut = esModel.getAliasedIndexName( MIndexes.INDEX_ALIAS_NAME )

      // Дождаться окончания оптимизациии и чтения имени старого индекса
      _             <- optimizeFut
      oldIndexNames <- oldIndexNamesFut

      // Переключить всю систему на новый индекс.
      _             <- esModel.resetAliasToIndex(
        indexName  = newIndexName,
        aliasName  = MIndexes.INDEX_ALIAS_NAME,
      )

      // Удалить старые индексы, если есть.
      _             <- Future.traverse(oldIndexNames)(esModel.deleteIndex)
    } yield {
      LOGGER.info(logPrefix + s"Done, took = ${System.currentTimeMillis - startedAtMs} ms.")
    }

    // Удалить временную директорию, когда всё будет закончено (не важно, ошибка или всё ок).
    doFut.onComplete { _ =>
      for (unpackedDir <- unpackedDirFut) {
        if (unpackedDir.exists()) {
          LOGGER.trace(s"$logPrefix Deleting temporary unpacked dir ${unpackedDir.getAbsolutePath} ...")
          FileUtils.deleteDirectory(unpackedDir)
        }
      }
    }

    // Если была ошибка во время doFut, то удалить новый индекс.
    for (ex <- doFut.failed) {
      LOGGER.error(s"$logPrefix FAILED. Deleting NEW index, it may be corrupted.", ex)
      esModel.deleteIndex( newIndexName )
    }

    // Вернуть основной фьючерс.
    doFut
  }


  /** Импорт таблицы городов. */
  def importCities(dir: File, bp: BulkProcessor, newIndexName: String)(implicit dsl: MappingDsl): Future[_] = {
    import esModel.api._

    val mCitiesTmp = mCitiesTmpFactory.create(newIndexName)
    val putMappingFut = mCitiesTmp.putMapping()

    val startedAtMs = System.currentTimeMillis
    lazy val logPrefix = s"importCities($startedAtMs):"
    val citiesFile = new File(dir, CITIES_FILENAME)

    LOGGER.debug(s"$logPrefix Bulk loading cities into ES...")

    val parsers = new CityParsers
    val p = parsers.cityLineP

    for (_ <- putMappingFut) yield {
      val src = Source.fromFile( citiesFile, CITIES_FILE_ENCODING )
      val linesTotal = try {
        src
          .getLines()
          .foldLeft(1) { (counter, cityLine) =>
            val pr = parsers.parse(p, cityLine)
            if (pr.successful) {
              val mcity = pr.get
              val inxReq = mCitiesTmp
                .prepareIndexNoVsn(mcity)
                .request()
              bp.add( inxReq )
            } else {
              LOGGER.warn(s"${logPrefix}Failed to parse line $counter file=${citiesFile.getAbsolutePath}:\n$cityLine\n$pr")
            }
            if (counter % 500 == 0)
              LOGGER.trace(s"${logPrefix}Still importing... ($counter)")
            counter + 1
          }
      } finally {
        src.close()
      }

      LOGGER.info(s"$logPrefix$linesTotal lines total. Took ${System.currentTimeMillis - startedAtMs} ms.")
    }
  }


  /**
   * Импорт диапазонов ip-адресов. Используется Pg COPY table FROM source.
   * @param dir директория с распакованными файлами.
   */
  def importIpRanges(dir: File, bp: BulkProcessor, newIndexName: String)(implicit dsl: MappingDsl): Future[_] = {
    import esModel.api._

    val mRangesTmp = mIpRangesTmpFactory.create( newIndexName )
    val putMappingFut = mRangesTmp.putMapping()

    val startedAtMs = System.currentTimeMillis
    lazy val logPrefix = s"importIpRanges($startedAtMs):"
    val cidrFile = new File(dir, IP_RANGES_FILENAME)
    LOGGER.debug(s"$logPrefix Will read $cidrFile ...")


    // Собираем инстанс парсера для всех строк:
    val parsers = new CidrParsers
    val p = parsers.cidrLineP

    for (_ <- putMappingFut) yield {
      // Делаем итератор для обхода неограниченно большого файла:

      val src = Source.fromFile(cidrFile, IP_RANGES_FILE_ENCODING)
      val linesTotal = try {
        src
          .getLines()
          .foldLeft(1) { (counter, cidrLine) =>
            val pr = parsers.parse(p, cidrLine)
            if (pr.successful) {
              val mrange = pr.get
              bp.add {
                mRangesTmp
                  .prepareIndexNoVsn( mrange )
                  .request()
              }
            } else {
              LOGGER.warn(s"$logPrefix Failed to parse line $counter file=${cidrFile.getAbsolutePath}:\n$cidrLine\n$pr")
            }
            if (counter % 20000 == 0)
              LOGGER.trace(s"$logPrefix Still converting... ($counter)")
            counter + 1
          }
      } finally {
        src.close()
      }

      LOGGER.info(s"$logPrefix $linesTotal lines converted total. Took ${System.currentTimeMillis - startedAtMs} ms.")
    }
  }

}


trait IpgbImporterJmxMBean {
  def updateIpBase(): String
}

final class IpgbImporterJmx @Inject()(
                                       injector                 : Injector,
                                       implicit private val ec  : ExecutionContext,
                                     )
  extends JmxBase
  with IpgbImporterJmxMBean
  with MacroLogsDyn
{
  import JmxBase._

  override def _jmxType = Types.IPGEOBASE

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
