package io.suggest.stat.m

import io.suggest.es.{IEsMappingProps, MappingDsl}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 15:12
  * Description: Суб-модель всякой очень общей статистики.
  */
object MCommon
  extends IEsMappingProps
{

  /** Имена es-полей модели. */
  object Fields {

    val COMPONENT_FN            = "component"
    val CLIENT_IP_FN            = "ip"
    val CLIENT_UID_FN           = "uid"
    val REQ_URI_FN              = "uri"
    val DOMAIN_3P_FN            = "domain3p"
    val IS_LOCAL_CLIENT_FN      = "isLocal"
    val GEN_FN                  = "gen"

  }


  import Fields._

  implicit val FORMAT: OFormat[MCommon] = (
    (__ \ COMPONENT_FN).format[Seq[MComponent]] and
    (__ \ CLIENT_IP_FN).formatNullable[String] and
    (__ \ CLIENT_UID_FN).formatNullable[String] and
    (__ \ REQ_URI_FN).formatNullable[String] and
    (__ \ DOMAIN_3P_FN).formatNullable[String] and
    (__ \ IS_LOCAL_CLIENT_FN).formatNullable[Boolean] and
    (__ \ GEN_FN).formatNullable[Long]
  )(apply, unlift(unapply))


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    Json.obj(
      COMPONENT_FN -> FKeyWord.indexedJs,
      CLIENT_IP_FN -> FIp.indexedJs,
      CLIENT_UID_FN -> FKeyWord.indexedJs,
      REQ_URI_FN -> FText.notIndexedJs,
      DOMAIN_3P_FN -> FKeyWord.indexedJs,
      IS_LOCAL_CLIENT_FN -> FBoolean.indexedJs,
      GEN_FN -> FNumber(
        typ = DocFieldTypes.Long,
        index = someTrue,
      ),
    )
  }

}


/** Класс экземпляров моделей общей статистики. */
case class MCommon(
  components      : Seq[MComponent],
  ip              : Option[String]        = None,
  clientUid       : Option[String]        = None,
  uri             : Option[String]        = None,
  domain3p        : Option[String]        = None,
  isLocalClient   : Option[Boolean]       = None,
  gen             : Option[Long]          = None
)
