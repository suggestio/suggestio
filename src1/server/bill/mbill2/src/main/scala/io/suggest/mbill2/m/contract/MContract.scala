package io.suggest.mbill2.m.contract

import java.time.OffsetDateTime

import javax.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.mbill2.m.common.InsertOneReturning
import io.suggest.mbill2.m.dt.DateCreatedSlick
import io.suggest.mbill2.m.gid.{DeleteById, GetById, GidSlick, Gid_t}
import io.suggest.slick.profile.pg.SioPgSlickProfileT
import slick.lifted.ProvenShape
import play.api.Configuration
import play.api.inject.Injector

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
final class MContracts @Inject()(
                                  injector: Injector,
                                  override protected val profile    : SioPgSlickProfileT,
                                )
  extends GidSlick
  with DateCreatedSlick
  with GetById
  with InsertOneReturning
  with DeleteById
{

  import MContract._
  import profile.api._

  override type Table_t = MContractsTable
  override type El_t = MContract

  private def configuration = injector.instanceOf[Configuration]

  /** Дефолтовый суффикс контракта, может быть использован при создании инстанса MContract. */
  lazy val SUFFIX_DFLT = configuration.getOptional[String]("bill.contract.suffix.dflt").getOrElse("CEO")

  /** slick-описание таблицы контрактов. */
  class MContractsTable(tag: Tag)
    extends Table[MContract](tag, TABLE_NAME)
    with GidColumn
    with DateCreated
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
  override lazy val query = TableQuery[MContractsTable]

  override protected def _withId(el: MContract, id: Gid_t): MContract = {
    el.copy(id = Some(id))
  }


  def updateOne(el: El_t) = {
    query
      .filter { _.id === el.id.get }
      .map { v => (v.dateCreated, v.hiddenInfo, v.suffix) }
      .update((el.dateCreated, el.hiddenInfo, el.suffix))
  }

}



/**
 * Экземпляр контракта.
 * @param dateCreated Дата создания (подписания).
 * @param crand Случайное число для выявления опечаток в номере договора.
 * @param id Основной id договора.
 */
final case class MContract(
  id            : Option[Gid_t]   = None,
  crand         : Int             = MContract.crand(),
  dateCreated   : OffsetDateTime  = OffsetDateTime.now(),
  hiddenInfo    : Option[String]  = None,
  suffix        : Option[String]  = None
)
  extends LegalContractIdT
{
  override protected def contractId: Gid_t = {
    id.get
  }
}
