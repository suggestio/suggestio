package io.suggest.file

import io.suggest.common.empty.EmptyUtil
import io.suggest.crypto.hash.MHash
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.crypto.hash.HashesHex.MHASHES_HEX_FORMAT_TRASPORT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 17:56
  * Description: Кросс-платформенная модель с инфой о файле, хранящемся на сервере.
  * В случае картинки подразумевается оригинал этой самой картинки.
  */
object MSrvFileInfo {

  /** Поддержка play-json для связи между клиентом и сервером. */
  implicit val MSRV_FILE_INFO: OFormat[MSrvFileInfo] = (
    (__ \ "n").format[String] and
    (__ \ "u").format[String] and
    (__ \ "s").formatNullable[Int] and
    (__ \ "a").formatNullable[String] and
    (__ \ "m").formatNullable[String] and
    (__ \ "h").formatNullable[Map[MHash, String]]
      .inmap[Map[MHash, String]](
        EmptyUtil.opt2ImplEmpty1F(Map.empty),
        { mapa => if (mapa.isEmpty) None else Some(mapa) }
      )
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MSrvFileInfo] = UnivEq.derive

}


/** Класс модели данных о файле, хранящемся на сервере.
  *
  * @param nodeId Уникальный id узла, в т.ч. эфемерный.
  * @param url Ссылка для скачивания файла.
  * @param sizeB Размер файла в байтах.
  * @param name Пользовательское название файла, если есть.
  * @param mimeType MIME-тип файла.
  * @param hashesHex Хэши файла, если есть.
  */
case class MSrvFileInfo(
                         nodeId     : String,
                         url        : String,
                         sizeB      : Option[Int],
                         name       : Option[String],
                         mimeType   : Option[String],
                         hashesHex  : Map[MHash, String]
                       )

