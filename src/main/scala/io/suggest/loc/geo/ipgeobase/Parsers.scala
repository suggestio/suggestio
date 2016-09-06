package io.suggest.loc.geo.ipgeobase

import io.suggest.model.geo.GeoPoint

import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.16 18:44
  * Description: Парсеры содержимого файлов для системы импорта.
  */

/** Общая утиль для парсеров, которые описаны в этом файле. */
sealed trait CityIdParser extends JavaTokenParsers {
  def cityIdP: Parser[CityId_t] = """\d{1,5}""".r ^^ { StringToCityId }
}


/** Парсинг файла cidr_optim.txt, содержащего диапазоны ip-адресов и их принадлежность. */
class CidrParsers extends CityIdParser {

  def inetIntP: Parser[_] = """\d+""".r

  /** Изначально, результат возвращался в виде InetAddress, но при переезде на ES всё-таки
    * было решено, что InetAddress -- это сложноватая mutable-state вещь, и лучше просто строкой. */
  def ip4P: Parser[String] = {
    "((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})".r //^^ { InetAddress.getByName }
  }

  def countryIso2P: Parser[String] = "[A-Z]{2}".r

  def cityIdOptP: Parser[Option[CityId_t]] = ("-" ^^^ None) | (cityIdP ^^ Some.apply)

  /** Парсер одной строки файла. */
  def cidrLineP: Parser[MIpRange] = {
    // Защищаемся от дубликации инстансов парсеров для колонок с одинаковыми типами.
    val iip = inetIntP
    val ip4p = ip4P
    (iip ~> iip ~> ip4p ~ ("-" ~> ip4p) ~ countryIso2P ~ cityIdOptP) ^^ {
      case ipStart ~ ipEnd ~ countryIso2 ~ cityIdOpt =>
        MIpRange(
          countryIso2 = countryIso2,
          ipRange     = Seq(ipStart, ipEnd),
          cityId      = cityIdOpt
        )
    }
  }

}


/** Утиль для парсинга cities.txt. Таблица в этом файле содержит просто географию городов. */
class CityParsers extends CityIdParser {

  /** Колонки таблицы разделяются одиночными табами. */
  override protected val whiteSpace: Regex = "\\t".r

  def floatP: Parser[Double] = "-?\\d+\\.\\d+".r ^^ { _.toDouble }

  def nameP: Parser[String] = s"[^$whiteSpace]+".r

  /** Парсер одной строки в файле (парсинг одного города). */
  def cityLineP: Parser[MCity] = {
    // Оптимизация: используем единые инстансы для повторяющихся парсеров
    val floatp = floatP
    val namep = nameP
    // Собираем парсер и маппер строки.
    cityIdP ~ namep ~ namep ~ namep ~ floatp ~ floatp ^^ {
      case cityId ~ cityName ~ region ~ fedDistrict ~ lat ~ lon =>
        MCity(
          cityId    = cityId,
          cityName  = cityName,
          region    = Option(region),
          center    = GeoPoint(lat = lat, lon = lon)
        )
    }
  }

}

