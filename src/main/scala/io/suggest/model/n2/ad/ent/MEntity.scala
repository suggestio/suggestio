package io.suggest.model.n2.ad.ent

import io.suggest.common.empty.IIsNonEmpty
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.n2.ad.ent.text.TextEnt
import io.suggest.util.SioEsUtil._
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

    /** Поля для текстоты. */
    object Text {

      val TEXT_FN = "t"
    }
  }


  override def generateMappingProps: List[DocField] = {
    List(
      FieldNumber(Fields.ID_FN, index = FieldIndexingVariants.no, include_in_all = false, fieldType = DocFieldTypes.integer),
      FieldObject(Fields.Text.TEXT_FN, enabled = true, properties = TextEnt.generateMappingProps)
    )
  }


  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MEntity] = (
    (__ \ Fields.ID_FN).format[Int] and
    (__ \ Fields.Text.TEXT_FN).formatNullable[TextEnt]
  )(apply, unlift(unapply))

}


case class MEntity(
  id    : Int,
  text  : Option[TextEnt]
)
  extends IIsNonEmpty
{

  override def isEmpty = !nonEmpty

  override def nonEmpty: Boolean = {
    text.nonEmpty
  }

}
