package models

import anorm._
import org.joda.time.DateTime
import util.AnormJodaTime._
import util.AnormPgArray._
import java.sql.Connection
import java.util.Currency

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

  val PROD_ADN_ID_PARSER = get[String]("prod_adn_id")

  /** Базовый парсер для колонок таблиц ad7ing_*. */
  val ROW_PARSER_BASE = get[Pk[Int]]("id") ~ get[String]("ad_id") ~ AMOUNT_PARSER ~ CURRENCY_CODE_PARSER ~
    get[DateTime]("date_created") ~ get[Option[Float]]("comission") ~ ADV_MODE_PARSER ~ get[Boolean]("on_start_page") ~
    get[DateTime]("date_start") ~ get[DateTime]("date_end") ~ PROD_ADN_ID_PARSER ~ get[String]("rcvr_adn_id")

  val COUNT_PARSER = get[Long]("c")
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


trait MAdvStatic[T] extends SqlModelStatic[T] {

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

  /**
   * Найти ряды по карточке и адресату запроса размещения.
   * @param adId id карточки.
   * @param rcvrId id получателя.
   * @param policy Политика блокировок.
   * @return Список подходящих рядов в неопределённом порядке.
   */
  def findByAdIdAndRcvr(adId: String, rcvrId: String, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE ad_id = {adId} AND rcvr_adn_id = {rcvrId}", policy, 'adId -> adId, 'rcvrId -> rcvrId)
  }

  def findByAdIdAndRcvrs(adId: String, rcvrIds: Traversable[String], policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE ad_id = {adId} AND rcvr_adn_id = ANY({rcvrIds})", policy, 'rcvrIds -> strings2pgArray(rcvrIds), 'ad_id -> adId)
  }

  /** Найти все ряда, содержащие указанного получателя в соотв. колонке.
    * @param rcvrAdnId id узла-получателя.
    * @param policy Политика блокировки рядов.
    * @return Список рядов, адресованных указанному получателю, в неопр.порядке.
    */
  def findByRcvr(rcvrAdnId: String, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE rcvr_adn_id = {rcvrAdnId}", policy, 'rcvrAdnId -> rcvrAdnId)
  }

  /**
   * Есть ли в текущей adv-модели ряд, который относится к указанной рекламной карточке
   * @param adId id рекламной карточки.
   * @return true, если в таблице есть хотя бы один подходящий ряд.
   */
  def hasAdvUntilNow(adId: String)(implicit c: Connection): Boolean = {
    SQL("SELECT count(*) > 0 AS bool FROM " + TABLE_NAME + " WHERE ad_id = {adId} AND date_end >= now() LIMIT 1")
      .on('adId -> adId)
      .as(SqlModelStatic.boolColumnParser single)
  }

  /**
   * Найти всех продьюсеров (рекламодателей) для указанного ресивера.
   * @param rcvrAdnId adn id узла-ресивера.
   * @return Список adn id продьюсеров (узлов-рекламодателей) в неопределённом порядке
   */
  def findAllProducersForRcvr(rcvrAdnId: String)(implicit c: Connection): List[String] = {
    SQL("SELECT DISTINCT prod_adn_id FROM " + TABLE_NAME + " WHERE rcvr_adn_id = {rcvrAdnId} AND date_end >= now()")
     .on('rcvrAdnId -> rcvrAdnId)
     .as(MAdv.PROD_ADN_ID_PARSER *)
  }


  /**
   * Посчитать кол-во рядов, относящихся к указанному ресиверу.
   * @param rcvrAdnId adn id узла-ресивера.
   * @return Неотрицательно кол-во.
   */
  def countForRcvr(rcvrAdnId: String)(implicit c: Connection): Long = {
    SQL("SELECT count(*) AS c FROM " + TABLE_NAME + " WHERE rcvr_adn_id = {rcvrAdnId}")
      .on('rcvrAdnId -> rcvrAdnId)
      .as(MAdv.COUNT_PARSER single)
  }
}
