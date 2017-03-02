package io.suggest.mbill2.m.order

import io.suggest.slick.profile.IProfile
import io.suggest.mbill2.m.gid.Gid_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:20
 * Description: Slick-поддержка частоиспользуемого поля order_id и смежных сущностей.
 */
trait OrderIdOptSlick extends IProfile with OrderIdFn {

  import profile.api._

  /** Добавить колонку orderId. */
  trait OrderIdOpt { that: Table[_] =>
    def orderIdOpt = column[Option[Gid_t]](ORDER_ID_FN)
  }

}


/** Аддон для поддержки внешнего ключа order_id. */
trait OrderIdOptFkSlick extends OrderIdOptSlick with OrderIdFkFn with IMOrders {

  import profile.api._

  /** Поддержка внешнего ключа таблицы по полю order_id. */
  trait OrderIdOptFk extends OrderIdOpt { that: Table[_] =>
    def orderOpt = foreignKey(ORDER_ID_FK, orderIdOpt, mOrders.query)(_.id.?)
  }

}


/** Аддон для slick-контейнера для поддержки индекса по order_id. */
trait OrderIdOptInxSlick extends OrderIdOptSlick with OrderIdInxFn {

  import profile.api._

  /** Поддержка индекса по полю order_id. */
  trait OrderIdOptInx extends OrderIdOpt { that: Table[_] =>
    def orderIdOptInx = index(ORDER_ID_INX, orderIdOpt, unique = false)
  }

}
