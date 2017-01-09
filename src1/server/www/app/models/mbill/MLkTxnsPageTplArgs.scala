package models.mbill

import io.suggest.mbill2.m.gid.Gid_t
import models.MNode
import io.suggest.mbill2.m.txn.{MTxn => MTxn2}
import io.suggest.mbill2.m.balance.{MBalance => MBalance2}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.16 21:33
  * Description: Контейнеры аргументов для рендера шаблонов списков транзакций.
  */


/** Интерфейс контейнера аргументов для шаблона [[views.html.lk.billing._txnsListTpl]]. */
trait ILkTxnsListTplArgs {
  def txns        : Seq[MTxn2]
  def balances    : Map[Gid_t, MBalance2]
}

/** Дефолтовая реализация модели [[ILkTxnsListTplArgs]]. */
case class MLkTxnsListTplArgs(
  override val txns         : Seq[MTxn2],
  override val balances     : Map[Gid_t, MBalance2]
)
  extends ILkTxnsListTplArgs


/** Контейнер аргументов для шаблона [[views.html.lk.billing.txnsPageTpl]]. */
trait ILkTxnsPageTplArgs extends ILkTxnsListTplArgs {
  def mnode       : MNode
  def currPage    : Int
  def txnsPerPage : Int
}

/** Дефолтовая реализация модели [[ILkTxnsPageTplArgs]]. */
case class MLkTxnsPageTplArgs(
  override val mnode        : MNode,
  override val txns         : Seq[MTxn2],
  override val balances     : Map[Gid_t, MBalance2],
  override val currPage     : Int,
  override val txnsPerPage  : Int
)
  extends ILkTxnsPageTplArgs
