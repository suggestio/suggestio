package io.suggest.sc.app

import io.suggest.crypto.hash.HashesHex
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.2020 15:32
  * Description: Аргументы для получения манифеста установки приложения с сервера.
  */
object MScAppManifestQs {

  object Fields {
    def ON_NODE_ID = "n"
    def HASHES_HEX = "h"
  }

  implicit def scAppManifestQsJson: OFormat[MScAppManifestQs] = {
    import HashesHex._
    val F = Fields
    (
      (__ \ F.ON_NODE_ID).formatNullable[String] and
      (__ \ F.HASHES_HEX).format[HashesHex]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MScAppManifestQs] = UnivEq.derive

}


/** Модель аргументов для манифеста.
  *
  * @param onNodeId id узла.
  * @param hashesHex Хэш (хэши) файла.
  *                  Нужны для решения проблем с кэширующей CDN или иными вариантами кэширования.
  */
final case class MScAppManifestQs(
                                   onNodeId       : Option[String]    = None,
                                   hashesHex      : HashesHex         = Map.empty,
                                 )
