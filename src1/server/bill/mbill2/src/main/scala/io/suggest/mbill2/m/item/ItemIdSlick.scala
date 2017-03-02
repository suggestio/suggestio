package io.suggest.mbill2.m.item

import io.suggest.common.m.sql.ITableName
import io.suggest.slick.profile.IProfile
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.util.PgaNamesMaker

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.15 10:53
 * Description: Поддержка slick-поля item_id в таблицах.
 */
trait ItemIdFn {
  def ITEM_ID_FN = "item_id"
}

trait ItemIdSlick extends IProfile with ItemIdFn {

  import profile.api._

  trait ItemIdColumn { that: Table[_] =>
    def itemId = column[Gid_t](ITEM_ID_FN)
  }

}


trait ItemIdFkFn extends ItemIdFn with ITableName {
  /** Название внешнего ключа. */
  def ITEM_ID_FK = PgaNamesMaker.fkey(TABLE_NAME, ITEM_ID_FN)
}
trait ItemIdFkSlick extends ItemIdSlick with ItemIdFkFn with IMItems {

  import profile.api._

  trait ItemIdFk extends ItemIdColumn { that: Table[_] =>
    def item = foreignKey(ITEM_ID_FK, itemId, mItems.query)(_.id)
  }

}


trait ItemIdInxFn extends ItemIdFn with ITableName {
  def ITEM_ID_INX = PgaNamesMaker.fkInx(TABLE_NAME, ITEM_ID_FN)
}
trait ItemIdInxSlick extends ItemIdSlick with ItemIdInxFn {

  import profile.api._

  trait ItemIdInx extends ItemIdColumn { that: Table[_] =>
    def itemInx = index(ITEM_ID_INX, itemId)
  }

}


trait IItemId {
  def itemId: Gid_t
}
