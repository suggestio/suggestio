package io.suggest.mbill2.m.item.tags

import com.google.inject.{Inject, Singleton}
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.common.InsertOneReturning
import io.suggest.mbill2.m.gid.{DeleteById, GetById, GidSlick, Gid_t}
import io.suggest.mbill2.m.item.{MItems, ItemIdInxSlick, ItemIdFkSlick}
import slick.lifted.ProvenShape

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.15 10:33
 * Description: slick-модель для перечисления тегов, в которых запрошено размещение.
 */
@Singleton
class MItemTags @Inject() (
  override protected val driver: ExPgSlickDriverT,
  override protected val mItems: MItems
)
  extends GidSlick
  with ItemIdFkSlick with ItemIdInxSlick
  with GetById
  with InsertOneReturning
  with DeleteById
{

  import driver.api._

  override val TABLE_NAME = mItems.TABLE_NAME + "_tags"

  override type Table_t = MItemTagsTable
  override type El_t    = MItemTag

  /** slick-описалово таблицы item_tags */
  class MItemTagsTable(tag: Tag)
    extends Table[MItemTag](tag, TABLE_NAME)
    with GidColumn
    with ItemIdFk with ItemIdInx
  {

    def face    = column[String]("face")
    def nodeId  = column[Option[String]]("node_id")

    override def * : ProvenShape[MItemTag] = {
      (itemId, face, nodeId, id.?) <> (
        MItemTag.tupled, MItemTag.unapply
      )
    }

  }

  override val query = TableQuery[MItemTagsTable]

  /** Апдейт значения экземпляра модели новым id. */
  override protected def _withId(el: MItemTag, id: Gid_t): MItemTag = {
    el.copy(id = Some(id))
  }

}


/** Экземпляр ряда item_tags. */
case class MItemTag(
  itemId  : Gid_t,
  face    : String,
  nodeId  : Option[String],
  id      : Option[Gid_t]    = None
)
