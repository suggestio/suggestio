package io.suggest.mbill2.m.txn.comis

import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.IDriver
import io.suggest.mbill2.m.balance.{IMBalances, MBalances}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.util.PgaNamesMaker

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.12.15 15:26
 * Description: Трейты поддержки поля с id баланса узла-комиссионера, т.е. получателя комиссии с платежей.
 */

trait ComissionBalanceIdOptSlick extends IDriver {

  import driver.api._

  def COMISSION_BALANCE_ID_FN = "comission_balance_id"

  trait ComissionBalanceIdOpt { that: Table[_] =>
    def comissionBalanceIdOpt = column[Option[Gid_t]](COMISSION_BALANCE_ID_FN)
  }

}


trait ComissionBalanceIdOptFkSlick extends ComissionBalanceIdOptSlick with ITableName with IMBalances {

  import driver.api._

  def COMISSION_BALANCE_ID_FK = PgaNamesMaker.fkey(TABLE_NAME, COMISSION_BALANCE_ID_FN)

  trait ComissionBalanceIdOptFk extends ComissionBalanceIdOpt { that: Table[_] =>
    def comissionBalanceOpt = foreignKey(COMISSION_BALANCE_ID_FK, comissionBalanceIdOpt, mBalances.query)(_.id.?)
  }

}


trait ComissionBalanceIdOptInxSlick extends ComissionBalanceIdOptSlick with ITableName {

  import driver.api._

  def COMISSION_BALANCE_ID_INX = PgaNamesMaker.fkInx(TABLE_NAME, COMISSION_BALANCE_ID_FN)

  trait ComissionBalanceIdOptInx extends ComissionBalanceIdOpt { that: Table[_] =>
    def comissionBalanceIdInx = index(COMISSION_BALANCE_ID_INX, comissionBalanceIdOpt, unique = false)
  }

}
