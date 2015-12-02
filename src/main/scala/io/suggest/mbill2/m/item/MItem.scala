package io.suggest.mbill2.m.item

import com.google.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.gid.{GidSlick, IGid}
import io.suggest.mbill2.m.item.status.{ItemStatusSlick, MItemStatus, IMItemStatus}
import io.suggest.mbill2.m.item.typ.{MItemTypeSlick, MItemType, IMItemType}
import io.suggest.mbill2.m.order.{IOrderId, MOrders, OrderIdFkSlick}
import io.suggest.mbill2.m.price._
import slick.lifted.ProvenShape

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:06
 * Description: Модель item'ов одного заказа. Это как бы абстрактная модель,
 */

/** Slick-заготовка для items-моделей, в частности для [[MItems]]. */
trait MItemsBaseT
  extends GidSlick
  with PriceSlick
  with CurrencyCodeSlick
  with AmountSlick
  with ITableName
  with OrderIdFkSlick
  with ItemStatusSlick
  with MItemTypeSlick
{

  import driver.api._

  /** В реализациях название будет дописываться и заменяться на val. */
  override def TABLE_NAME = "item"

  /** Загатовка slick-таблицы item. */
  trait MItemsTableT
    extends GidColumn
    with PriceColumn
    with CurrencyCodeColumn
    with CurrencyColumn
    with AmountColumn
    with OrderIdColumn
    with OrderIdFk
    with ItemStatusColumn
    with ItemTypeColumn
  { that: Table[_] =>
  }

}


/** DI-контейнер для slick-модели абстрактных item'ов. */
@Singleton
class MItems @Inject() (
  override protected val driver   : ExPgSlickDriverT,
  override protected val mOrders  : MOrders
)
  extends MItemsBaseT
{

  override val TABLE_NAME = super.TABLE_NAME

  import driver.api._

  /** Реализация абстрактной slick-таблицы item'ов. */
  class MItemsTable(tag: Tag)
    extends Table[MItem](tag, TABLE_NAME)
    with MItemsTableT
  {

    override def * : ProvenShape[MItem] = {
      (orderId, iType, status, price, id.?) <> (
        MItem.tupled, MItem.unapply
      )
    }

  }

  val items = TableQuery[MItemsTable]

}


/** Интерфейс экземпляра модели. */
trait IItem
  extends IGid
  with IOrderId
  with IMPrice
  with IMItemType
  with IMItemStatus


/** Экземпляр модели (ряда абстрактной таблицы item'ов). */
case class MItem(
  override val orderId      : Long,
  override val iType        : MItemType,
  override val status       : MItemStatus,
  override val price        : MPrice,
  override val id           : Option[Long] = None
)
  extends IItem
