package io.suggest.mbill2.m.order

import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.IDriver
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.util.PgaNamesMaker

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:20
 * Description: Slick-поддержка частоиспользуемого поля order_id и смежных сущностей.
 */
trait OrderIdFn {

  /** Название поля order_id в текущей таблице. */
  def ORDER_ID_FN = "order_id"

}

trait OrderIdSlick extends IDriver with OrderIdFn {

  import driver.api._

  /** Добавить колонку orderId. */
  trait OrderId { that: Table[_] =>
    def orderId = column[Gid_t](ORDER_ID_FN)
  }

}


trait OrderIdFkFn extends OrderIdFn with ITableName {
  /** Название внешнего ключа для поля order_id. */
  def ORDER_ID_FK = PgaNamesMaker.fkey(TABLE_NAME, ORDER_ID_FN)
}


/** Аддон для поддержки внешнего ключа order_id. */
trait OrderIdFkSlick extends OrderIdSlick with OrderIdFkFn with IMOrders {

  import driver.api._

  /** Поддержка внешнего ключа таблицы по полю order_id. */
  trait OrderIdFk extends OrderId { that: Table[_] =>
    def order = foreignKey(ORDER_ID_FK, orderId, mOrders.query)(_.id)
  }

}


trait OrderIdInxFn extends OrderIdFn with ITableName {

  /** Название индекса по полю order_id на стороне БД. */
  def ORDER_ID_INX = PgaNamesMaker.fkInx(TABLE_NAME, ORDER_ID_FN)

}

/** Аддон для slick-контейнера для поддержки индекса по order_id. */
trait OrderIdInxSlick extends OrderIdSlick with OrderIdInxFn {

  import driver.api._

  /** Поддержка индекса по полю order_id. */
  trait OrderIdInx extends OrderId { that: Table[_] =>
    def orderIdInx = index(ORDER_ID_INX, orderId, unique = false)
  }

}
