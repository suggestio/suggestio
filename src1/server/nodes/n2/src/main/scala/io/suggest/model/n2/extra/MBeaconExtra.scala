package io.suggest.model.n2.extra

import java.util.UUID

import io.suggest.model.es.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.08.16 17:34
  * Description: Модель для хранения структуры данных, описывающих один bluetooth-маячок.
  */
object MBeaconExtra extends IGenEsMappingProps {

  /** Названия ES-полей модели. */
  object Fields {

    val UUID_FN   = "uu"
    val MAJOR_FN  = "mj"
    val MINOR_FN  = "mi"

  }

  import Fields._

  /** Поддержка сериализации/десериализации JSON. */
  implicit val FORMAT: OFormat[MBeaconExtra] = (
    (__ \ UUID_FN).format[String] and
    (__ \ MAJOR_FN).format[Int] and
    (__ \ MINOR_FN).format[Int]
  )(apply, unlift(unapply))

  import io.suggest.util.SioEsUtil._

  private def _fieldNumber(fn: String) = {
    FieldNumber(
      id              = fn,
      fieldType       = DocFieldTypes.integer,
      index           = FieldIndexingVariants.not_analyzed,
      include_in_all  = true
    )
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(UUID_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      _fieldNumber(MAJOR_FN),
      _fieldNumber(MINOR_FN)
    )
  }

}


/** Модель с данными одного ble-маячка. */
case class MBeaconExtra(
  uuidStr : String,
  major   : Int,
  minor   : Int
) {

  /** Получить инстанс java UUID. */
  lazy val uuid = UUID.fromString(uuidStr)

}
