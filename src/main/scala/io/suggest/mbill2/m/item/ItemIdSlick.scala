package io.suggest.mbill2.m.item

import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.IDriver
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.util.PgaNamesMaker

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.15 10:53
 * Description: Поддержка slick-поля item_id в таблицах.
 */
trait ItemIdSlick extends IDriver {

  import driver.api._

  def ITEM_ID_FN = "item_id"

  trait ItemIdColumn { that: Table[_] =>
    def itemId = column[Gid_t](ITEM_ID_FN)
  }

}


trait ItemIdFkSlick extends ItemIdSlick with ITableName {

  import driver.api._

  /** Название внешнего ключа. */
  def ITEM_ID_FK = PgaNamesMaker.fkey(TABLE_NAME, ITEM_ID_FN)

  /** DI-экземпляр slick-модели [[MItems]]. */
  protected def mItems: MItems

  trait ItemIdFk extends ItemIdColumn { that: Table[_] =>
    def item = foreignKey(ITEM_ID_FK, itemId, mItems.items)(_.id)
  }

}


trait ItemIdInxSlick extends ItemIdSlick with ITableName {

  import driver.api._

  def ITEM_ID_INX = PgaNamesMaker.fkInx(TABLE_NAME, ITEM_ID_FN)

  trait ItemIdInx extends ItemIdColumn { that: Table[_] =>
    def itemInx = index(ITEM_ID_INX, itemId)
  }

}


trait IItemId {
  def itemId: Gid_t
}
