package io.suggest.mbill2.m.balance

import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.IDriver
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.util.PgaNamesMaker

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 17:13
 * Description: Поддержка поля balance_id для slick-моделей, ссылающихся на кошельки.
 */
trait BalanceIdSlick extends IDriver {

  import driver.api._

  def BALANCE_ID_FN = "balance_id"

  trait BalanceIdColumn { that: Table[_] =>
    def balanceId = column[Gid_t](BALANCE_ID_FN)
  }


}


trait BalanceIdFkSlick extends BalanceIdSlick with ITableName {

  import driver.api._

  /** Название внешнего ключа для balance_id. */
  def BALANCE_ID_FK = PgaNamesMaker.fkey(TABLE_NAME, BALANCE_ID_FN)

  /** Доступ к DI-экземпляру slick-модели балансов. */
  protected def mBalances: MBalances

  trait BalanceIdFk extends BalanceIdColumn { that: Table[_] =>
    def balance = foreignKey(BALANCE_ID_FK, balanceId, mBalances.balances)(_.id)
  }

}


trait BalanceIdInxSlick extends BalanceIdSlick {

  import driver.api._

  def BALANCE_ID_INX: String

  trait BalanceIdInx extends BalanceIdColumn { that: Table[_] =>
    def balanceInx = index(BALANCE_ID_INX, balanceId, unique = false)
  }

}
