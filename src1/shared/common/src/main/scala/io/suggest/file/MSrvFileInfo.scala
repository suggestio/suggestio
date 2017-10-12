package io.suggest.file

import io.suggest.color.MHistogram
import io.suggest.common.empty.EmptyUtil
import io.suggest.crypto.hash.MHash
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.crypto.hash.HashesHex.MHASHES_HEX_FORMAT_TRASPORT
import io.suggest.text.StringUtil
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 17:56
  * Description: Кросс-платформенная модель с инфой о файле, хранящемся на сервере.
  * В случае картинки подразумевается оригинал этой самой картинки.
  */
object MSrvFileInfo {

  /** Собрать невалидный пустой инстанс. Использовать только когда ОЧЕНЬ надо. */
  def empty: MSrvFileInfo = {
    val s = ""
    MSrvFileInfo(
      nodeId    = s,
      url       = s
    )
  }

  /** Поддержка play-json для связи между клиентом и сервером. */
  implicit val MSRV_FILE_INFO: OFormat[MSrvFileInfo] = (
    (__ \ "n").format[String] and
    (__ \ "u").format[String] and
    (__ \ "s").formatNullable[Long] and
    (__ \ "a").formatNullable[String] and
    (__ \ "m").formatNullable[String] and
    (__ \ "h").formatNullable[Map[MHash, String]]
      .inmap[Map[MHash, String]](
        EmptyUtil.opt2ImplEmpty1F(Map.empty),
        { mapa => if (mapa.isEmpty) None else Some(mapa) }
      ) and
    (__ \ "c").formatNullable[MHistogram]
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
                         sizeB      : Option[Long]        = None,
                         name       : Option[String]      = None,
                         mimeType   : Option[String]      = None,
                         hashesHex  : Map[MHash, String]  = Map.empty,
                         colors     : Option[MHistogram]  = None
                       ) {

  /** Объединить данные данного инстанса и данные из более свежего инстанса.
    * Сервер может не утруждать себя сбором данных, которые есть на клиенте.
    * Поэтому с сервера могут приходить неполные данные, и их следует мержить.
    *
    * @param newInfo Обновлённый инстанс [[MSrvFileInfo]], содержащий более актуальные данные.
    * @return Объединённый экземпляр [[MSrvFileInfo]].
    */
  def updateFrom(newInfo: MSrvFileInfo): MSrvFileInfo = {
    if (newInfo ===* this) {
      this
    } else {
      // БЕЗ copy(), чтобы при добавлении новых полей тут сразу подсвечивалась ошибка.
      MSrvFileInfo(
        nodeId = newInfo.nodeId,
        // Велосипед для фильтрации корректных ссылок.
        // Сервер, на ранних этапах запиливания кода, может возвращать TO*DO-мусор вместо ссылок.
        url = StringUtil.firstStringMakesSence( newInfo.url, url )
          .getOrElse(url),
        sizeB = newInfo.sizeB
          .orElse(sizeB),
        name = newInfo.name
          .orElse( name ),
        mimeType = newInfo.mimeType
          .orElse( mimeType ),
        hashesHex = Seq(newInfo.hashesHex, hashesHex)
          .find(_.nonEmpty)
          .getOrElse(hashesHex),
        colors = newInfo.colors.orElse( colors )
      )
    }
  }

}

