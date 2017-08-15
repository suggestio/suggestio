package io.suggest.model.n2.ad.ent

import io.suggest.common.empty.{EmptyUtil, IIsNonEmpty}
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.model.n2.ad.ent.text.TextEnt
import io.suggest.es.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

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

      /** 2017.aug.15: Раньше координаты хранились прямо внутри .text.coords.
        * Вынос на уровень выше с учётом совместимости.
        */
        // TODO Удалить после MNode.resaveMany().
      private[ent] val OLD_COORDS_ESFN       = "coords"

      val TEXT_FN = "t"
    }
  }


  override def generateMappingProps: List[DocField] = {
    List(
      FieldNumber( Fields.ID_FN, index = false, include_in_all = false, fieldType = DocFieldTypes.integer),
      FieldObject( Fields.Text.TEXT_FN, enabled = true, properties = TextEnt.generateMappingProps),
      FieldObject( Fields.COORDS_ESFN, enabled = false, properties = Nil)
    )
  }


  /** Поддержка JSON. */
  implicit val MENTITY_FORMAT: OFormat[MEntity] = {
    val textPath = __ \ Fields.Text.TEXT_FN
    val coordsPath = __ \ Fields.COORDS_ESFN

    // TODO Костыли для совместимости со старым форматом: координаты внутри .text. Надо это будет удалить после MNode.resaveMany().
    val coordsReads = coordsPath.read[MCoords2di]
      .map { EmptyUtil.someF }
      .orElse {
        (textPath \ Fields.Text.OLD_COORDS_ESFN).readNullable[MCoords2di]
          .map { resOpt =>
            if (resOpt.nonEmpty)
              LOGGER.warn("Format.reads(): deprecated coords format found. Use MNode.resaveMany()")
            resOpt
          }
      }
    val coordsWrites = coordsPath.writeNullable[MCoords2di]
    val coordsFmt = OFormat(coordsReads, coordsWrites)

    (
      (__ \ Fields.ID_FN).format[Int] and
      textPath.formatNullable[TextEnt] and
      coordsFmt
    )(apply, unlift(unapply))
  }

}


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
