package io.suggest.model.n2.media

import java.time.OffsetDateTime

import io.suggest.es.model.IGenEsMappingProps
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
    (__ \ DATE_CREATED_FN).format[OffsetDateTime]
  )(apply, unlift(unapply))


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldText(MIME_FN, index = true, include_in_all = true),
      FieldNumber(SIZE_B_FN, fieldType = DocFieldTypes.long, index = true, include_in_all = false),
      FieldBoolean(IS_ORIGINAL_FN, index = true, include_in_all = false),
      FieldKeyword(SHA1_FN, index = false, include_in_all = false),
      FieldDate(DATE_CREATED_FN, index = true, include_in_all = false)
    )
  }

}


case class MFileMeta(
  mime          : String,
  sizeB         : Long,
  isOriginal    : Boolean,
  sha1          : Option[String],
  dateCreated   : OffsetDateTime = OffsetDateTime.now()
)
