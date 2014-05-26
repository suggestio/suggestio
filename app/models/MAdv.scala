package models

import anorm._
import org.joda.time.{Period, DateTime}
import util.AnormJodaTime._
import util.AnormPgInterval._
import org.postgresql.util.PGInterval
import java.sql.Connection
import java.util.Currency
import org.joda.time.format.ISOPeriodFormat

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 16:59
 * Description: Модель для группы родственных таблиц adv_*.
 */
object MAdv {
  import SqlParser._

  val ADV_MODE_PARSER = get[String]("mode").map(MAdvModes.withName)

  val AMOUNT_PARSER = get[Float]("amount")

  val CURRENCY_CODE_PARSER = get[String]("currency_code")
  val CURRENCY_PARSER = CURRENCY_CODE_PARSER.map { cc =>
    Currency.getInstance(cc)
  }

  /** Базовый парсер для колонок таблиц ad7ing_*. */
  val ROW_PARSER_BASE = get[Pk[Int]]("id") ~ get[String]("ad_id") ~ AMOUNT_PARSER ~ CURRENCY_CODE_PARSER ~
    get[DateTime]("date_created") ~ get[Option[Float]]("comission") ~ ADV_MODE_PARSER ~ get[Boolean]("on_start_page") ~
    get[DateTime]("date_start") ~ get[DateTime]("date_end") ~ get[String]("prod_adn_id") ~ get[String]("rcvr_adn_id")

}


/** Интерфейс всех экземпляров MAdv* моделей. */
trait MAdvI {
  def adId          : String
  def amount        : Float
  def currencyCode  : String
  def comission     : Option[Float]
  def dateCreated   : DateTime
  def id            : Pk[Int]
  def mode          : MAdvMode
  def onStartPage   : Boolean
  def dateStatus    : DateTime
  def dateStart     : DateTime
  def dateEnd       : DateTime
  def prodAdnId     : String
  def rcvrAdnId     : String
}


object MAdvModes extends Enumeration {
  type MAdvMode = Value
  val OK      = Value("o")
  val REQ     = Value("r")
  val REFUSED = Value("e")
}


trait MAdvStatic[T] extends SiowebSqlModelStatic[T] {

  /**
   * Поиск по колонке adId, т.е. по id рекламной карточки.
   * @param adId id рекламной карточки, которую размещают.
   * @return Список найленных рядов в неопределённом порядке.
   */
  def findByAdId(adId: String)(implicit c: Connection): List[T] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE ad_id = {adId}")
      .on('adId -> adId)
      .as(rowParser *)
  }
}
