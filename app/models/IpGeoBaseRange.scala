package models

import java.net.InetAddress
import java.sql.Connection

import anorm._
import util.SqlModelSave
import util.AnormInetAddress._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.14 16:15
 * Description: Модель для хранения диапазонов ip-адресов на основе cidr_optim.txt.
 */

// TODO Хотелось бы CIDR-таблицу с подсетями, а не диапазоны. Это будет работать быстрее и есть меньше ресурсов.

object IpGeoBaseRange extends Truncate {

  override type T = IpGeoBaseRange

  val TABLE_NAME = "ipgeobase_range"

  val rowParser = {
    import SqlParser._
    get[String]("country_iso2") ~ get[Option[Int]]("city_id") ~ get[InetAddress]("start") ~ get[InetAddress]("end") map {
      case countryIso2 ~ cityId ~ start ~ end =>
        IpGeoBaseRange(start = start, end = end, countryIso2 = countryIso2, cityId = cityId)
    }
  }

  def findForIp(ip: InetAddress)(implicit c: Connection): List[IpGeoBaseRange] = {
    SQL(s"""SELECT * FROM $TABLE_NAME WHERE start <= {ip} AND {ip} <= "end"""")
      .on('ip -> ip)
      .as(rowParser *)
  }

}


import IpGeoBaseRange._


case class IpGeoBaseRange(
  start       : InetAddress,
  end         : InetAddress,
  countryIso2 : String,
  cityId      : Option[Int]
) extends SqlModelSave[IpGeoBaseRange] with IpGeoBaseCityIdOpt {

  override def hasId = false

  override def saveInsert(implicit c: Connection): IpGeoBaseRange = {
    SQL(s"""INSERT INTO $TABLE_NAME(country_iso2, city_id, "start", "end") VALUES({countryIso2}, {cityId}, {start}, {end})""")
      .on('countryIso2 -> countryIso2, 'cityId -> cityId, 'start -> start, 'end -> end)
      .executeInsert(rowParser single)
  }

  override def saveUpdate(implicit c: Connection): Int = 0
}
