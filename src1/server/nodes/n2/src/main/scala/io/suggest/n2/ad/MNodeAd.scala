package io.suggest.n2.ad

import io.suggest.ad.blk.ent.MEntity
import io.suggest.ad.blk.BlockMeta
import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.n2.ad.rd.RichDescr
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 13:56
 * Description: Поле, хранящее данные рекламной карточки в MNode.
 * Живёт вне extras из-за обширности рекламной карточки.
 *
 * При отсутствие данных внутри, это неявно-пустая модель.
 */
object MNodeAd extends IEmpty {

  override type T = MNodeAd

  /** Поля этой под-модели. */
  object Fields {

    /** Поля для отображаемых на карточке сущностей  */
    object Entities {
      val ENTITIES_FN = "of"
    }

    /** Поля развёрнутого описания. */
    object RDescr {
      val RDESCR_FN = "rd"
    }

    object BlockMeta {
      val BLOCK_META_FN = "bm"
    }

  }


  implicit val FORMAT: OFormat[MNodeAd] = (
    (__ \ Fields.Entities.ENTITIES_FN).formatNullable[Seq[MEntity]]
      .inmap [Map[Int, MEntity]] (
        {ents =>
          if (ents.isEmpty) {
            Map.empty
          } else {
            toEntMap1( ents.get )
          }
        },
        {entsMap =>
          if (entsMap.isEmpty) {
            None
          } else {
            val ents = entsMap.valuesIterator.toSeq
            Some(ents)
          }
        }
      ) and
    (__ \ Fields.RDescr.RDESCR_FN).formatNullable[RichDescr] and
    (__ \ Fields.BlockMeta.BLOCK_META_FN).formatNullable[BlockMeta]
  )(apply, unlift(unapply))


  def toEntMap(es: MEntity*): EntMap_t = {
    toEntMap1(es)
  }
  def toEntMap1(es: IterableOnce[MEntity]): EntMap_t = {
    es.iterator
      .map { e => e.id -> e }
      .toMap
  }

  /** Пустой экземпляр модели, расшарен между кучей инстансов [[io.suggest.n2.node.MNode]]. */
  override val empty: MNodeAd = {
    new MNodeAd() {
      override def nonEmpty = false
    }
  }

}


case class MNodeAd(
  entities      : EntMap_t              = Map.empty,    // TODO Сделать Option[MEntity.type=document]
  richDescr     : Option[RichDescr]     = None,
  blockMeta     : Option[BlockMeta]     = None
)
  extends EmptyProduct
