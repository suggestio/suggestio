package models

import java.net.InetAddress
import java.sql.Connection

import anorm._
import util.anorm.AnormInetAddress
import util.{PlayLazyMacroLogsImpl, SqlModelSave}
import AnormInetAddress._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.14 16:15
 * Description: Модель для хранения диапазонов ip-адресов на основе cidr_optim.txt.
 */

// TODO Хотелось бы CIDR-таблицу с подсетями, а не диапазоны. Это будет работать быстрее и есть меньше ресурсов.

object IpGeoBaseRange extends SqlModelStatic with SqlTruncate with SqlAnalyze with SqlIndexName with PlayLazyMacroLogsImpl with SqlVacuumAnalyze {

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


  /**
   * Генерация SQL-команды для pg COPY IN (COPY FROM).
   * @param delim разделитель, используемый для колонок.
   * @return SQL-строка, пригодная для отправки в org.postgresql.copy.CopyManager.copyIn().
   */
  def copySqlCmd(delim: String, nullStr: String) = s"COPY $TABLE_NAME FROM STDIN WITH DELIMITER '$delim' NULL '$nullStr'"

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

}


import IpGeoBaseRange._


final case class IpGeoBaseRange(
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

  /**
   * Выдать строку данных для отправки на вход в pg COPY IN.
   * @param delim Разделитель полей.
   * @return Строка, пригодная для отправки в качестве одной строки COPY.
   */
  def exportForPgCopy(delim: String, nullStr: String): String = {
    // Конвертим Option[Int] в строку императивно, с минимумом мусора в памяти.
    val cityIdStr = if (cityId.isDefined)  cityId.get.toString  else  nullStr
    new StringBuilder(64)
      .append(countryIso2).append(delim)
      .append(cityIdStr).append(delim)
      .append(start.getHostAddress).append(delim)
      .append(end.getHostAddress)
      .toString()
  }

  override def saveUpdate(implicit c: Connection): Int = 0
}
