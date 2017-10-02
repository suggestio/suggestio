package io.suggest.crypto.hash

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 19:18
  * Description: Поддержка elasticsearch для псевдо-модели [[HashesHex]].
  */
object HashesHexEs extends IGenEsMappingProps {

  object Fields {
    val HASH_TYPE_FN = "t"
    val HEX_VALUE_FN = "x"
  }

  implicit val MHASHES_HEX_ROW_FORMAT_ES: OFormat[HashHex] = {
    val F = Fields
    (
      (__ \ F.HASH_TYPE_FN).format[MHash] and
      (__ \ F.HEX_VALUE_FN).format[String]
    )({ (k, v) => (k, v) }, identity[(MHash, String)])
  }

  /** Поддержка play-json для [[HashesHex]]. */
  implicit val MHASHES_HEX_FORMAT_ES: Format[HashesHex] = {
    implicitly[Format[Iterable[(MHash, String)]]]
      .inmap[Map[MHash, String]](
        { vs => vs.toMap },
        identity
      )
  }


  /** Список ES-полей модели. */
  override def generateMappingProps: List[DocField] = {
    val F = Fields
    List(
      FieldKeyword(F.HASH_TYPE_FN, index = true, include_in_all = false),
      FieldKeyword(F.HEX_VALUE_FN, index = true, include_in_all = true)
    )
  }

}
