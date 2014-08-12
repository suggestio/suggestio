package util.geo

import java.net.InetAddress
import java.util.Comparator
import models.{IpGeoBaseCity, IpGeoBaseRange}

import scala.annotation.tailrec
import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.14 14:03
 * Description: Утиль для поддержки БД, взятых из [[http://ipgeobase.ru/]].
 */
object IpGeoBase {

  /** Ссылка для скачивания текущей базы. */
  def ARCHIVE_DOWNLOAD_URL = configuration.getString("ipgeobase.archive.zip.url") getOrElse "http://ipgeobase.ru/files/db/Main/geo_files.zip"

}


/** Общая утиль для парсеров, которые описаны в этом файле. */
sealed trait CityIdParser extends JavaTokenParsers {
  def cityIdP: Parser[Int] = """\d{1,5}""".r ^^ { _.toInt }
}


/** Парсинг файла cidr_optim.txt, содержащего диапазоны ip-адресов и их принадлежность. */
object IpGeoBaseCidrParsers extends CityIdParser {

  def inetIntP: Parser[_] = """\d+""".r

  def ip4P: Parser[InetAddress] = {
    "(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})".r ^^ { InetAddress.getByName }
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

  def nameP: Parser[String] = "(?U)[\\w ]+".r

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

