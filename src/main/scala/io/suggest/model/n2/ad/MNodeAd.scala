package io.suggest.model.n2.ad

import io.suggest.common.EmptyProduct
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.n2.ad.ent.MEntity
import io.suggest.util.SioEsUtil.{FieldNestedObject, DocField}
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
object MNodeAd extends IGenEsMappingProps {

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

  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldNestedObject(Fields.Entities.ENTITIES_FN, enabled = true, properties = MEntity.generateMappingProps)
    )
  }

  implicit val FORMAT: OFormat[MNodeAd] = {
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
      )
      .inmap[MNodeAd](apply, _.entities)
  }


  def toEntMap(es: MEntity*): EntMap_t = {
    toEntMap1(es)
  }
  def toEntMap1(es: Seq[MEntity]): EntMap_t = {
    es.iterator
      .map { e => e.id -> e }
      .toMap
  }

}


case class MNodeAd(
  entities      : EntMap_t     = Map.empty
)
  extends EmptyProduct
