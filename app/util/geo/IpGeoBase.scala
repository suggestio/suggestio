package util.geo

import java.io._
import java.net.{URL, InetAddress}
import java.sql.Connection
import java.util.Comparator
import models.{CronTask, IpGeoBaseCity, IpGeoBaseRange}
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection
import play.api.db.{HasInternalConnection, DB}
import util.{CronTasksProvider, PlayMacroLogsImpl}

import scala.annotation.tailrec
import scala.io.Source
import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers
import scala.concurrent.duration._
import play.api.Play.{current, configuration}
import dispatch._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.14 14:03
 * Description: Утиль для поддержки БД, взятых из [[http://ipgeobase.ru/]].
 */
object IpGeoBaseImport extends PlayMacroLogsImpl with CronTasksProvider {

  import LOGGER._

  /** Активация импорта требует явного включения этой функции в конфиге. */
  def IS_ENABLED: Boolean = configuration.getBoolean("ipgeobase.import.enabled") getOrElse false

  /** Ссылка для скачивания текущей базы. */
  def ARCHIVE_DOWNLOAD_URL = configuration.getString("ipgeobase.archive.url") getOrElse "http://ipgeobase.ru/files/db/Main/geo_files.zip"

  /** Поверхностная проверка скачанного архива на профпригодность. */
  def ARCHIVE_MAGIC_NUMBER = configuration.getInt("ipgeobase.archive.magic") getOrElse 0x504b0304

  /** Минимально допустимая длина архива с данными. */
  def ARCHIVE_MIN_LENGTH = configuration.getInt("ipgeobase.archive.size.min") getOrElse 1900000

  /** Имя файл, содержащего диапазоны.*/
  def IP_RANGES_FILENAME = configuration.getString("ipgeobase.cidr_optim.filename") getOrElse "cidr_optim.txt"

  /** Кодировка файла с дампами диапазонов. Исторически там win-1251.
    * Корректное значение названия кодировки надо брать из колонки java.nio нижеуказанной ссылки.
    * @see [[http://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html]]. */
  def IP_RANGES_FILE_ENCODING = configuration.getString("ipgeobase.cidr_optim.encoding") getOrElse "windows-1251"

  /** Название файла, в котором лежит карта городов. */
  def CITIES_FILENAME = configuration.getString("ipgeobase.cities.filename") getOrElse "cities.txt"

  /** Кодировка содержимого файла городов. */
  def CITIES_FILE_ENCODING = configuration.getString("ipgeobase.cities.encoding") getOrElse IP_RANGES_FILE_ENCODING


  override def cronTasks: TraversableOnce[CronTask] = {
    if (IS_ENABLED) {
      // TODO Нужно обновлять 1-2 раза в день максимум, а не после каждого запуска.
      val task = CronTask(
        startDelay = 20 seconds,
        every = 1 day,
        displayName = "updateIpBase()"
      ) {
        updateIpBase()
      }
      Seq(task)
    } else {
      Nil
    }
  }

  /** Скачать файл с дампом базы в tmp. */
  def download(): Future[File] = {
    val dlUrlStr = ARCHIVE_DOWNLOAD_URL
    // TODO Нужно проверять ETag и Last-Modified. Выставлять If-Modified-Since и If-None-Match. Это улучшит работу.
    val urlFile = new URL(dlUrlStr).getFile
    val fileBasename = FilenameUtils.getBaseName(urlFile)
    val filenameExt = FilenameUtils.getExtension(urlFile)
    val archiveFile = File.createTempFile(fileBasename,  "." + filenameExt)
    // TODO Нужно ограничивать max-size для возвращаемых данных. 5 метров макс. будет достаточно.
    val resultFut = Http( url(dlUrlStr) > as.File(archiveFile) )
      // Метод `>` не проверяет 200 OK. Нужно вручную проверить, что скачался именно архив.
      .filter { _ =>
        // Тестируем архив по методике http://www.java2s.com/Code/Java/File-Input-Output/DeterminewhetherafileisaZIPFile.htm
        !archiveFile.isDirectory  &&  archiveFile.canRead  &&  archiveFile.length() > ARCHIVE_MIN_LENGTH  && {
          val in = new DataInputStream(new BufferedInputStream(new FileInputStream(archiveFile)))
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
      .map { _ =>
        trace(s"download(): Downloaded file size = ${archiveFile.length} bytes.")
        archiveFile
      }
    resultFut onFailure { case ex =>
      archiveFile.delete()
      info(s"download(): Failed to fetch $dlUrlStr into file ${archiveFile.getAbsolutePath} . Tmp file deleted.")
    }
    resultFut
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
    try {
      val cmdArgs = Array[String]("unzip", "-d", dir.getAbsolutePath, archiveFile.getAbsolutePath)
      val p = Runtime.getRuntime.exec(cmdArgs)
      val result = p.waitFor()
      if (result != 0) {
        throw new RuntimeException("Failed to unpack archive: return code = " + result)
      }
      dir

    } catch {
      case ex: Throwable =>
        FileUtils.deleteDirectory(dir)
        throw ex
    }
  }


  /** Импорт таблицы городов. */
  def importCities(dir: File)(implicit c: Connection): Unit = {
    lazy val logPrefix = "importCities(): "
    val citiesFile = new File(dir, CITIES_FILENAME)
    debug(logPrefix + "Truncating table...")
    IpGeoBaseCity.truncateTable
    val copyTmpFile = File.createTempFile("importIpRangesCopyTo", ".tsv")
    debug(logPrefix + "Converting rows for import via COPY FROM. Temp file will be " + copyTmpFile)
    try {
      val delim = "|"
      val pw = new PrintWriter(copyTmpFile)
      try {
        val linesIter = Source.fromFile(citiesFile, CITIES_FILE_ENCODING).getLines()
        val p = IpGeoBaseCityParsers.cityLineP
        val linesTotal = linesIter.foldLeft(1) {
          (counter, cityLine) =>
            val pr = IpGeoBaseCityParsers.parse(p, cityLine)
            if (pr.successful) {
              val str = pr.get.exportForPgCopy(delim)
              pw.println(str)
            } else {
              warn(s"${logPrefix}Failed to parse line $counter file=${citiesFile.getAbsolutePath}:\n$cityLine\n$pr")
            }
            if (counter % 300 == 0)
              trace(s"${logPrefix}Still importing... ($counter)")
            counter + 1
        }
        info(s"$logPrefix$linesTotal lines total.")
      } finally {
        pw.close()
      }
      val cm = new CopyManager(getPgConnection)
      val sql = IpGeoBaseCity.copyCmd(delim)
      val is = new FileInputStream(copyTmpFile)
      try {
        cm.copyIn(sql, is)
      } finally {
        is.close()
      }
    } finally {
      copyTmpFile.delete()
    }
    trace(logPrefix + "Calling ANALYZE...")
    IpGeoBaseCity.analyze
  }

  /** Отковырять pg connection из play connection. Play использует враппер над jdbc connection'ом. */
  @tailrec private def getPgConnection(implicit c: Connection): BaseConnection = {
    c match {
      case bc: BaseConnection =>
        bc
      case hic: HasInternalConnection =>
        getPgConnection(hic.getInternalConnection())
    }
  }

  /**
   * Импорт диапазонов ip-адресов. Используется Pg COPY table FROM source.
   * @param dir директория с распакованными файлами.
   */
  def importIpRanges(dir: File)(implicit c: Connection): Unit = {
    lazy val logPrefix = "importIpRanges(): "
    val cidrFile = new File(dir, IP_RANGES_FILENAME)
    debug(logPrefix + "Truncating table...")
    IpGeoBaseRange.truncateTable
    // Делаем итератор для обхода неограниченно большого файла:
    val linesIter =  Source.fromFile(cidrFile, IP_RANGES_FILE_ENCODING).getLines()
    // Напрямую писать в postgres 160k+ рядов - это архидолго и проблематично. Поэтому используем COPY TO.
    // Для этого дампим всё распарсенное и отмаппленное во временный файл в качестве буфера.
    val copyTmpFile = File.createTempFile("importIpRangesCopyTo", ".tsv")
    try {
      debug(logPrefix + "Starting to covert ip range psql COPY TO...")
      val copyDelim = "|"
      val nullStr = "__NULL__"
      val pw = new PrintWriter(copyTmpFile)
      try {
        // Собираем инстанс парсера для всех строк:
        val p = IpGeoBaseCidrParsers.cidrLineP
        val linesTotal = linesIter.toStream.foldLeft(1) {
          (counter, cidrLine) =>
            val pr = IpGeoBaseCidrParsers.parse(p, cidrLine)
            if (pr.successful) {
              val line4copy = pr.get.exportForPgCopy(copyDelim, nullStr)
              pw.println(line4copy)
            } else {
              warn(s"${logPrefix}Failed to parse line $counter file=${cidrFile.getAbsolutePath}:\n$cidrLine\n$pr")
            }
            if (counter % 20000 == 0)
              trace(s"${logPrefix}Still converting... ($counter)")
            counter + 1
        }
        info(s"$logPrefix$linesTotal lines converted total.")
      } finally {
        pw.close()
      }
      // Подготовка данных для импорта через COPY завершена. Делаем же...
      debug(logPrefix + "Staring copyIn() for import " + copyTmpFile)
      val cm = new CopyManager(getPgConnection)
      val is = new FileInputStream(copyTmpFile)
      try {
        val sql = IpGeoBaseRange.copySqlCmd(copyDelim, nullStr)
        cm.copyIn(sql, is)
      } finally {
        is.close()
      }
    } finally {
      copyTmpFile.delete()
    }
    debug(logPrefix + "Calling ANALYZE...")
    IpGeoBaseRange.analyze
  }


  /** Нужно скачать базу, распаковать, импортнуть города, импортнуть диапазоны, оптимизировать таблицы. */
  def updateIpBase(): Future[_] = {
    lazy val logPrefix = "updateIpBase(): "
    val resultFut = download() map { archiveFile =>
      // Распаковать в директорию, удалить скачанный архив.
      trace(logPrefix + "Unpacking downloaded archive " + archiveFile)
      try {
        unpack(archiveFile)
      } finally {
        trace(logPrefix + "Deleting downloaded archive " + archiveFile)
        archiveFile.delete()
      }
    } map { unpackedDir =>
      // Подготовится к импорту таблиц: снести индексы, очистить от данных.
      try {
        DB.withTransaction { implicit c =>
          debug(logPrefix + "Importing cities...")
          importCities(unpackedDir)
          debug(logPrefix + "Importing ip ranges...")
          importIpRanges(unpackedDir)
        }
      } finally {
        trace(s"Deleting temporary unpacked dir ${unpackedDir.getAbsolutePath} ...")
        FileUtils.deleteDirectory(unpackedDir)
      }
      info(logPrefix + "Done!")
    }
    resultFut onFailure {
      case ex: Throwable =>
        error("Failed to updateIpBase()", ex)
    }
    resultFut
  }

}


/** Общая утиль для парсеров, которые описаны в этом файле. */
sealed trait CityIdParser extends JavaTokenParsers {
  def cityIdP: Parser[Int] = """\d{1,5}""".r ^^ { _.toInt }
}


/** Парсинг файла cidr_optim.txt, содержащего диапазоны ip-адресов и их принадлежность. */
object IpGeoBaseCidrParsers extends CityIdParser {

  def inetIntP: Parser[_] = """\d+""".r

  def ip4P: Parser[InetAddress] = {
    "((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})".r ^^ { InetAddress.getByName }
  }

  def countryIso2P: Parser[String] = "[A-Z]{2}".r

  def cityIdOptP: Parser[Option[Int]] = ("-" ^^^ None) | (cityIdP ^^ Some.apply)

  /** Парсер одной строки файла. */
  def cidrLineP: Parser[IpGeoBaseRange] = {
    // Защищаемся от дубликации инстансов парсеров для колонок с одинаковыми типами.
    val iip = inetIntP
    val ip4p = ip4P
    (iip ~> iip ~> ip4p ~ ("-" ~> ip4p) ~ countryIso2P ~ cityIdOptP) ^^ {
      case ipStart ~ ipEnd ~ countryIso2 ~ cityIdOpt =>
        IpGeoBaseRange(start = ipStart, end = ipEnd, countryIso2 = countryIso2, cityId = cityIdOpt)
    }
  }

}


/** Утиль для парсинга cities.txt. Таблица в этом файле содержит просто географию городов. */
object IpGeoBaseCityParsers extends CityIdParser {

  /** Колонки таблицы разделяются одиночными табами. */
  override protected val whiteSpace: Regex = "\\t".r

  def floatP: Parser[Double] = "-?\\d+\\.\\d+".r ^^ { _.toDouble }

  def nameP: Parser[String] = s"[^$whiteSpace]+".r

  /** Парсер одной строки в файле (парсинг одного города). */
  def cityLineP: Parser[IpGeoBaseCity] = {
    // Оптимизация: используем единые инстансы для повторяющихся парсеров
    val floatp = floatP
    val namep = nameP
    // Собираем парсер и маппер строки.
    cityIdP ~ namep ~ namep ~ namep ~ floatp ~ floatp ^^ {
      case cityId ~ cityName ~ region ~ fedDistrict ~ lat ~ lon =>
        IpGeoBaseCity(id = cityId, cityName = cityName, region = region, lat = lat, lon = lon)
    }
  }

}


/** Компаратор для сортировки ip-адресов (4 и 6).
  * @see [[http://stackoverflow.com/a/13756288]]. */
class InetAddressComparator extends Comparator[InetAddress] {

  override def compare(adr1: InetAddress, adr2: InetAddress): Int = {
    val ba1 = adr1.getAddress
    val ba2 = adr2.getAddress
    // general ordering: ipv4 before ipv6
    if(ba1.length < ba2.length) {
      -1
    } else if(ba1.length > ba2.length) {
      1
    } else {
      byByteCmp(ba1, ba2, 0)
    }
  }

  @tailrec final def byByteCmp(ba1: Array[Byte], ba2: Array[Byte], i: Int): Int = {
    if (i < ba1.length) {
      val b1 = unsignedByteToInt(ba1(i))
      val b2 = unsignedByteToInt(ba2(i))
      if (b1 == b2)
        byByteCmp(ba1, ba2, i + 1)
      else if (b1 < b2)
        -1
      else
        1
    } else {
      0
    }
  }

  def unsignedByteToInt(b: Byte): Int = {
    b & 0xFF
  }

}

