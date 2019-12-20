package io.suggest.model.n2.extra

import java.util.UUID

import io.suggest.es.{IEsMappingProps, MappingDsl}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.08.16 17:34
  * Description: Модель для хранения структуры данных, описывающих один bluetooth-маячок.
  */
object MBeaconExtra
  extends IEsMappingProps
{

  /** Названия ES-полей модели. */
  object Fields {

    val UUID_FN   = "uu"
    val MAJOR_FN  = "mj"
    val MINOR_FN  = "mi"

  }

  import Fields._

  /** Поддержка сериализации/десериализации JSON. */
  implicit val beaconExtraJson: OFormat[MBeaconExtra] = (
    (__ \ UUID_FN).format[String] and
    (__ \ MAJOR_FN).format[Int] and
    (__ \ MINOR_FN).format[Int]
  )(apply, unlift(unapply))


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    val fInt = Json.toJsObject(
      FNumber(
        typ   = DocFieldTypes.Integer,
        index = someTrue,
      )
    )
    Json.obj(
      F.UUID_FN -> FKeyWord.indexedJs,
      F.MAJOR_FN -> fInt,
      F.MINOR_FN -> fInt,
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
