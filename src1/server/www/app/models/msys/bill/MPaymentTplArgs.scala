package models.msys.bill

import io.suggest.mbill2.m.contract.MContract
import io.suggest.model.n2.node.MNode
import play.api.data.Form

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.16 16:12
  * Description: Модель аргументов для вызова шаблона совершения sys-платежа.
  */
trait IPaymentTplArgs {

  /** Маппинг формы. */
  def bf: Form[MPaymentFormResult]

  /** Доступные для выбора валюты. */
  def currencyOpts: Seq[(String, String)]

  def mnode: MNode

  def mcontract: MContract

}

case class MPaymentTplArgs(
  override val bf               : Form[MPaymentFormResult],
  override val currencyOpts  : Seq[(String, String)],
  override val mnode            : MNode,
  override val mcontract        : MContract
)
  extends IPaymentTplArgs
