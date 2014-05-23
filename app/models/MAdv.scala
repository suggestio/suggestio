package models

import anorm._
import org.joda.time.{Period, DateTime}
import util.AnormJodaTime._
import util.AnormPgInterval._
import org.postgresql.util.PGInterval

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 16:59
 * Description: Модель для группы родственных таблиц adv_*.
 */
object MAdv {
  import SqlParser._

  /** Базовый парсер для колонок таблиц ad7ing_*. */
  val ROW_PARSER_BASE = get[Pk[Int]]("id") ~ get[String]("ad_id") ~ get[Float]("amount") ~
    get[Option[String]]("currency_code") ~ get[DateTime]("date_created") ~ get[Option[Float]]("comission_pc") ~
    get[PGInterval]("period")

}


/** Интерфейс всех экземпляров MAdv* моделей. */
trait MAdvI {
  def adId          : String
  def amount        : Float
  def currencyCodeOpt: Option[String]
  def comissionPc   : Option[Float]
  def period        : Period
  def dateCreated   : DateTime
  def id            : Pk[Int]
}
