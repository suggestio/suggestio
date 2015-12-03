package io.suggest.mbill2.m.contract

import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.IDriver
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.util.PgaNamesMaker

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 10:40
 * Description: Трейты для slick-поддержки частого поля contract_id.
 */


/** Трейт для добавления поддержки поля contract_id. */
trait ContractIdSlick extends IDriver {

  /** Название поля contract_id, обычно неизменно. */
  def CONTRACT_ID_FN = "contract_id"

  import driver.api._

  /** Просто колонка contract_id. */
  trait ContractIdColumn { that: Table[_] =>
    def contractId = column[Gid_t](CONTRACT_ID_FN)
  }

}


/** Поддержка внешнего ключа для поля contract_id. */
trait ContractIdSlickFk extends ContractIdSlick with ITableName {

  /** Название ограничения внешнего ключа contract_id. */
  protected def CONTRACT_ID_FK = PgaNamesMaker.fkey(TABLE_NAME, CONTRACT_ID_FN)

  /** DI-инстанс DI-контейнера таблицы контактов. */
  protected def mContracts: MContracts

  import driver.api._

  trait ContractIdFk extends ContractIdColumn { that: Table[_] =>
    def contract = foreignKey(CONTRACT_ID_FK, contractId, mContracts.contracts)(_.id)
  }

}


/** Описание индекса по полю contract_id.  */
trait ContractIdSlickIdx extends ContractIdSlick {

  /** Название индекса по полю contract_id. */
  protected def CONTRACT_ID_INX: String

  import driver.api._

  trait ContractIdIdx extends ContractIdColumn { that: Table[_] =>
    def contractIdInx = index(CONTRACT_ID_INX, contractId)
  }

}
