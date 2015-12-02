package io.suggest.mbill2.m.contract

import com.google.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.dt.DateCreatedSlick
import io.suggest.mbill2.m.gid.GidSlick
import org.joda.time.DateTime
import slick.lifted.ProvenShape

import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 15:48
 * Description: Модель контрактов со slick-ядром.
 * Контракты -- т.е. юридические прокладки, связывающие ноды и биллинг.
 * Номер договора прописывается на узле, несколько узлов могут иметь один и тот же номер договора.
 */
object MContract extends ITableName {

  /** Псевдослучайное число от 101 до 999. Избегаем нулей, чтобы не путали с буквой 'O'. */
  def crand(): Int = {
    (new Random).nextInt(898) + 101
  }

  override val TABLE_NAME     = "contract"

  // Названия полей (столбцов).
  def CRAND_FN                = "crand"
  def HIDDEN_INFO_FN          = "hidden_info"
  def SUFFIX_FN               = "suffix"

  def CRAND_INX               = s"${TABLE_NAME}_${CRAND_FN}_idx"

}


/** slick-модель таблицы контрактов. */
@Singleton
class MContracts @Inject()(
  override protected val driver     : ExPgSlickDriverT
)
  extends GidSlick
  with DateCreatedSlick
{

  import MContract._
  import driver.api._

  /** slick-описание таблицы контрактов. */
  class MContractsTable(tag: Tag)
    extends Table[MContract](tag, TABLE_NAME)
    with GidColumn
    with DateCreatedColumn
  {

    def crand       = column[Int](CRAND_FN)
    def hiddenInfo  = column[Option[String]](HIDDEN_INFO_FN, O.Length(1024))
    def suffix      = column[Option[String]](SUFFIX_FN, O.Length(8))

    def crandInx    = index(CRAND_INX, crand, unique = false)

    override def * : ProvenShape[MContract] = {
      (id.?, crand, dateCreated, hiddenInfo, suffix) <> ((MContract.apply _).tupled, MContract.unapply)
    }

  }

  /** Доступ к запросам модели. */
  val contracts = TableQuery[MContractsTable]

}


/**
 * Экземпляр контракта.
 * @param dateCreated Дата создания (подписания).
 * @param crand Случайное число для выявления опечаток в номере договора.
 * @param id Основной id договора.
 */
case class MContract(
  id            : Option[Long]    = None,
  crand         : Int             = MContract.crand(),
  dateCreated   : DateTime        = DateTime.now(),
  hiddenInfo    : Option[String]  = None,
  suffix        : Option[String]  = None
)
