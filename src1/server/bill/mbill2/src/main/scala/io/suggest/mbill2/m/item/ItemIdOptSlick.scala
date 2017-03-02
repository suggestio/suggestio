package io.suggest.mbill2.m.item

import io.suggest.slick.profile.IProfile
import io.suggest.mbill2.m.gid.Gid_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.15 10:53
 * Description: Поддержка slick-поля item_id в таблицах.
 */
trait ItemIdOptSlick extends IProfile with ItemIdFn {

  import profile.api._

  trait ItemIdOpt { that: Table[_] =>
    def itemIdOpt = column[Option[Gid_t]](ITEM_ID_FN)
  }

}


trait ItemIdOptFkSlick extends ItemIdOptSlick with ItemIdFkFn with IMItems {

  import profile.api._

  trait ItemIdOptFk extends ItemIdOpt { that: Table[_] =>
    def itemOpt = foreignKey(ITEM_ID_FK, itemIdOpt, mItems.query)(_.id.?)
  }

}


trait ItemIdOptInxSlick extends ItemIdOptSlick with ItemIdInxFn {

  import profile.api._

  trait ItemIdOptInx extends ItemIdOpt { that: Table[_] =>
    def itemInx = index(ITEM_ID_INX, itemIdOpt)
  }

}


trait IItemIdOpt {
  def itemIdOpt: Option[Gid_t]
}
