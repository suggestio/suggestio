package util.geo

import java.net.{Inet4Address, InetAddress}
import java.util.Comparator
import models.IpGeoBaseRange
import org.jboss.netty.handler.ipfilter.{CIDR4, CIDR}

import scala.annotation.tailrec
import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.14 14:03
 * Description: Утиль для поддержки БД, взятых из [[http://ipgeobase.ru/]].
 */
object IpGeoBase {

  // TODO Надо придумать, как впиливать несколько CIDR для диапазона.

  /** Подобрать cidr для заданного диапазона ip-адресов. Стартовый адрес используется как base address.
    * @param ipStart стартовый ip-адрес
    * @param ipEnd финальный ip-адрес.
    * @param comparator Инстанс компаратора ip-адресов. При массовом сравнении можно выносить многоразовый компаратор
    *                   за пределы цикла сравнения.
    * @return Экземпляр CIDR или exception, если что-то не так. IllegalArgumentException если не удаётся подобрать CIDR.
    */
  def range2cidr(ipStart: InetAddress, ipEnd: InetAddress, comparator: Comparator[InetAddress] = new InetAddressComparator): CIDR = {
    range2cidrTry(ipStart, ipEnd, 32, comparator)
  }

  @tailrec final def range2cidrTry(base: InetAddress, end: InetAddress, netmask: Int, comparator: Comparator[InetAddress]): CIDR = {
    val cidr = CIDR.newCIDR(base, netmask)
    val cmpResult = comparator.compare(cidr.getEndAddress, end)
    if (cmpResult == 0)
      cidr
    else if (cmpResult < 0)
      range2cidrTry(base, end, netmask - 1, comparator)
    else
      throw new IllegalArgumentException(s"Unable to convert $base - $end into CIDR.")
  }

}


/** Парсинг файла cidr_optim.txt, содержащего диапазоны ip-адресов и их принадлежность. */
object IpGeoBaseCidrParsers extends JavaTokenParsers {

  def inetIntP: Parser[_] = """\d+""".r

  def ip4P: Parser[InetAddress] = "(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})".r ^^ {
    InetAddress.getByName
  }

  def countryIso2P: Parser[String] = "[A-Z]{2}".r

  def cityIdP: Parser[Int] = """\d{1,5}""".r ^^ { _.toInt }
  def cityIdOptP: Parser[Option[Int]] = ("-" ^^^ None) | (cityIdP ^^ Some.apply)

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

object IpGeoBaseCityParsers extends JavaTokenParsers {

  override protected val whiteSpace: Regex = "\\t".r

  def floatP: Parser[Double] = "-?\\d+\\.\\d+".r ^^ { _.toDouble }

  def nameP: Parser[String] = "(?U)[\\w ]+".r

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

