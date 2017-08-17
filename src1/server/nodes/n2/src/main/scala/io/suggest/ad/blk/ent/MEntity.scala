package io.suggest.ad.blk.ent

import io.suggest.common.empty.IIsNonEmpty
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 17:22
 * Description: Модель MEntity пришла на смену AdOfferT, которая была кривоватой, и перепиливать её
 * перед перепиливанием на N2 не было особого смысла.
 *
 * AOBlock (из в прошлой архитектуры) упразднён, т.е. AdOfferT напряму видит TextEnt (бывш.AOStringField).
 * AOBlock.href переносится в meta.business.siteUrl, т.к. по факту он жил отдельно от AOBlock.
 */
object MEntity extends IGenEsMappingProps {

  object Fields {

    /** Имя поля числового идентификатора сущности. */
    val ID_FN = "n"

    /** Имя поля с опциональными экранными координатами объекта в пикселях. */
    val COORDS_ESFN       = "xy"

    /** Поля для текстоты. */
    object Text {

      val TEXT_FN = "t"

    }
  }


  override def generateMappingProps: List[DocField] = {
    List(
      FieldNumber( Fields.ID_FN, index = false, include_in_all = false, fieldType = DocFieldTypes.integer),
      FieldObject( Fields.Text.TEXT_FN, enabled = true, properties = TextEntJvm.generateMappingProps),
      FieldObject( Fields.COORDS_ESFN, enabled = false, properties = Nil)
    )
  }


  /** Поддержка JSON. */
  implicit val MENTITY_FORMAT: OFormat[MEntity] = (
    (__ \ Fields.ID_FN).format[Int] and
    (__ \ Fields.Text.TEXT_FN).formatNullable[TextEnt] and
    (__ \ Fields.COORDS_ESFN).formatNullable[MCoords2di]
  )(apply, unlift(unapply))

}


/** Entity -- это модель одного объекта на рекламной карточке.
  * Самый простой пример объекта -- текст, который привязан к каким-то координатам.
  *
  * @param id Entity id, т.е. некий уникальный и порядковый номер среди других entities этой же карточки.
  * @param text Опциональный текстовый контент, если есть.
  * @param coords Пиксельные координаты для позиционирования объекта на экране.
  */
case class MEntity(
                    id      : Int,
                    text    : Option[TextEnt],
                    coords  : Option[MCoords2di]
                  )
  extends IIsNonEmpty
{

  override def isEmpty = !nonEmpty

  override def nonEmpty: Boolean = {
    text.nonEmpty
  }

}
