package io.suggest.model.n2.media

import io.suggest.model.es.{IGenEsMappingProps, EsModelUtil}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.09.15 18:24
 * Description: Метаданные файла для модели [[MMedia]].
 */
object MFileMeta extends IGenEsMappingProps {

  val MIME_FN             = "mm"
  val SIZE_B_FN           = "sz"
  val IS_ORIGINAL_FN      = "orig"
  val SHA1_FN             = "s1"
  val DATE_CREATED_FN     = "dc"

  /** Поддержка JSON для модели. */
  implicit val FORMAT: OFormat[MFileMeta] = (
    (__ \ MIME_FN).format[String] and
    (__ \ SIZE_B_FN).format[Long] and
    (__ \ IS_ORIGINAL_FN).format[Boolean] and
    (__ \ SHA1_FN).formatNullable[String] and
    (__ \ DATE_CREATED_FN).format(EsModelUtil.Implicits.jodaDateTimeFormat)
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(MIME_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldNumber(SIZE_B_FN, fieldType = DocFieldTypes.long, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldBoolean(IS_ORIGINAL_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(SHA1_FN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldDate(DATE_CREATED_FN, index = null, include_in_all = false)
    )
  }

}


case class MFileMeta(
  mime          : String,
  sizeB         : Long,
  isOriginal    : Boolean,
  sha1          : Option[String],
  dateCreated   : DateTime = DateTime.now()
)
