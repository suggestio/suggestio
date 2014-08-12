package models

import java.net.InetAddress
import java.sql.{SQLException, Connection}

import anorm._
import util.{PlayLazyMacroLogsImpl, SqlModelSave}
import util.AnormInetAddress._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.14 16:15
 * Description: Модель для хранения диапазонов ip-адресов на основе cidr_optim.txt.
 */

// TODO Хотелось бы CIDR-таблицу с подсетями, а не диапазоны. Это будет работать быстрее и есть меньше ресурсов.

object IpGeoBaseRange extends SqlModelStatic with SqlTruncate with SqlAnalyze with SqlIndexName with PlayLazyMacroLogsImpl {

  import LOGGER._

  override type T = IpGeoBaseRange

  override val TABLE_NAME = "ipgeobase_range"

  override val rowParser = {
    import SqlParser._
    get[String]("country_iso2") ~ get[Option[Int]]("city_id") ~ get[InetAddress]("start") ~ get[InetAddress]("end") map {
      case countryIso2 ~ cityId ~ start ~ end =>
        IpGeoBaseRange(start = start, end = end, countryIso2 = countryIso2, cityId = cityId)
    }
  }

  /** Колонки, покрываемые индексами. */
  def INDEXED_COLUMNS = List[String]("start", "end")

  /**
   * Поиск ip в диапазонах в таблице.
   * @param ip ip-адрес.
   * @return Список подходящих диапазонов в неопределённом порядке. Обычно, там 1 или 0 результатов.
   */
  def findForIp(ip: InetAddress)(implicit c: Connection): List[IpGeoBaseRange] = {
    SQL(s"""SELECT * FROM $TABLE_NAME WHERE "start" <= {ip} AND {ip} <= "end"""")
      .on('ip -> ip)
      .as(rowParser *)
  }

  /** Создать индексы, дропнутые ранее через dropIndexes(). */
  def createIndexes(implicit c: Connection) {
    INDEXED_COLUMNS foreach { colName =>
      val iname = indexName(colName)
      SQL(s"""CREATE UNIQUE INDEX $iname ON $TABLE_NAME USING btree ("$colName")""")
        .execute()
    }
  }

  /** Дропнуть индексы таблицы. Очень ускоряет работу при перезаливке данных в таблицу без использования COPY. */
  def dropIndexes(implicit c: Connection) {
    INDEXED_COLUMNS foreach { colName =>
      val iname = indexName(colName)
      try {
        SQL("DROP INDEX " + iname)
          .execute()
      } catch {
        case ex: SQLException =>
          warn("Suppressing error during DROP INDEX " + iname + ". Index do not exist?", ex)
      }
    }
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
