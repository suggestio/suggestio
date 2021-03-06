package models.msys.bill

import io.suggest.mbill2.m.balance.MBalance
import io.suggest.mbill2.m.contract.MContract
import io.suggest.n2.node.MNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.15 14:01
 * Description: Модель аргументов шаблона sys-биллинга одного узла [[views.html.sys1.bill.forNodeTpl]].
 */
trait IForNodeTplArgs {

  def mnode: MNode

  /** Контракт узла, если есть. */
  def mContractOpt: Option[MContract]

  /** Валютные депозиты узла в рамках контракта. */
  def mBalances: Seq[MBalance]

  /** id узла CBCA, который используется как источник тарифа по-умолчанию. */
  def cbcaNodeOpt: Option[MNode]
}


/** Дефолтовая реализация модели [[IForNodeTplArgs]]. */
case class MForNodeTplArgs(
  override val mnode        : MNode,
  override val mContractOpt : Option[MContract],
  override val mBalances    : Seq[MBalance],
  override val cbcaNodeOpt  : Option[MNode]
)
  extends IForNodeTplArgs

