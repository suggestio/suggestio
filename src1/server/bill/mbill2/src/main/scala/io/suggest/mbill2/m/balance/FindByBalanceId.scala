package io.suggest.mbill2.m.balance

import io.suggest.mbill2.m.common.ModelContainer
import io.suggest.mbill2.m.gid.{GidSlick, Gid_t}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 16:38
 * Description: Трейт для поиска по id контракта.
 */
trait FindByBalanceId extends BalanceIdSlick with ModelContainer with GidSlick {

  import profile.api._

  override type Table_t <: Table[El_t] with GidColumn with BalanceIdColumn

  def findByBalanceIdBuilder(balanceId: Gid_t) = {
    query
      .filter(_.balanceId === balanceId)
  }

  /** Найти все ряды с указанным id кошелька. */
  def findByBalanceId(balanceId: Gid_t) = {
    findByBalanceIdBuilder(balanceId)
      .result
  }


  def findByBalanceIdsBuilder(balanceIds: Iterable[Gid_t]) = {
    if (balanceIds.isEmpty) {
      query
    } else {
      query
        .filter(_.balanceId inSet balanceIds)
    }
  }

  /** Найти все ряды, у которых id кошелька находится в множестве указанных. */
  def findByBalanceIds(balanceIds: Iterable[Gid_t]) = {
    // Нанооптимизация: отработать пустой balanceIds без фактического запроса к БД.
    if (balanceIds.nonEmpty) {
      findByBalanceIdsBuilder(balanceIds)
        .result
    } else {
      DBIO.successful(Nil)
    }
  }

}
