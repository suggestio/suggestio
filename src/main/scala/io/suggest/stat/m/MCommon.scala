package io.suggest.stat.m

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.model.es.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.model.es.EsModelUtil.Implicits.jodaDateTimeFormat

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 15:12
  * Description: Суб-модель всякой очень общей статистики.
  */
object MCommon extends IGenEsMappingProps with IEmpty {

  override type T = MCommon

  /** Имена es-полей модели. */
  object Fields {

    val CLIENT_IP_FN            = "ip"
    val CLIENT_UID_FN           = "uid"
    val REQ_URI_FN              = "uri"
    val DOMAIN_3P_FN            = "domain3p"
    val IS_LOCAL_CLIENT_FN      = "isLocal"
    val GEN_FN                  = "gen"

  }


  import Fields._

  implicit val FORMAT: OFormat[MCommon] = (
    (__ \ CLIENT_IP_FN).formatNullable[String] and
    (__ \ CLIENT_UID_FN).formatNullable[String] and
    (__ \ REQ_URI_FN).formatNullable[String] and
    (__ \ DOMAIN_3P_FN).formatNullable[String] and
    (__ \ IS_LOCAL_CLIENT_FN).formatNullable[Boolean] and
    (__ \ GEN_FN).formatNullable[Long]
  )(apply, unlift(unapply))

  def empty = MCommon()


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldIp(CLIENT_IP_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(CLIENT_UID_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(REQ_URI_FN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(DOMAIN_3P_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldBoolean(IS_LOCAL_CLIENT_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldNumber(GEN_FN, fieldType = DocFieldTypes.long, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

}


/** Класс экземпляров моделей общей статистики. */
case class MCommon(
  ip              : Option[String]        = None,
  clientUid       : Option[String]        = None,
  uri             : Option[String]        = None,
  domain3p        : Option[String]        = None,
  isLocalClient   : Option[Boolean]       = None,
  gen             : Option[Long]          = None
)
  extends EmptyProduct
