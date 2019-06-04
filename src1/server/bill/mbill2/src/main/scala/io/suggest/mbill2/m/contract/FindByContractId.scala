package io.suggest.mbill2.m.contract

import io.suggest.mbill2.m.common.ModelContainer
import io.suggest.mbill2.m.gid.GidSlick

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 16:38
 * Description: Трейт для поиска по id контракта.
 */
trait FindByContractId extends ContractIdSlick with GidSlick with ModelContainer {

  import profile.api._

  override type Table_t <: Table[El_t] with GidColumn with ContractIdColumn


  def findByContractIdSql(contractId: Id_t) = {
    query
      .filter(_.contractId === contractId)
  }

  def findByContractId(contractId: Id_t) = {
    findByContractIdSql(contractId)
      .result
  }

  def countByContractId(contractId: Id_t): DBIOAction[Int, NoStream, Effect.Read] = {
    findByContractIdSql(contractId)
      .size
      .result
  }

}
