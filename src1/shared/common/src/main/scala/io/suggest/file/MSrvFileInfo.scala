package io.suggest.file

import io.suggest.color.MHistogram
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.crypto.hash.{HashesHex, MHash}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.crypto.hash.HashesHex.MHASHES_HEX_FORMAT_TRASPORT
import io.suggest.err.ErrorConstants
import io.suggest.scalaz.StringValidationNel
import io.suggest.text.StringUtil
import io.suggest.ueq.UnivEqUtil._

import scalaz.Validation

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
    MSrvFileInfo(
      nodeId = ""
    )
  }

  /** Поддержка play-json для связи между клиентом и сервером. */
  implicit val MSRV_FILE_INFO: OFormat[MSrvFileInfo] = (
    (__ \ "n").format[String] and
    (__ \ "u").formatNullable[String] and
    (__ \ "s").formatNullable[Long] and
    (__ \ "a").formatNullable[String] and
    (__ \ "m").formatNullable[String] and
    (__ \ "h").formatNullable[Map[MHash, String]]
      .inmap[Map[MHash, String]](
        EmptyUtil.opt2ImplEmpty1F(Map.empty),
        { mapa => if (mapa.isEmpty) None else Some(mapa) }
      ) and
    (__ \ "c").formatNullable[MHistogram] and
    (__ \ "w").formatNullable[MSize2di]
  )(apply, unlift(unapply))


  /** Провалидировать инстанс [[MSrvFileInfo]] перед сохранением на сервер.
    * По факту, все поля кроме nodeId не нужны.
    *
    * @param fi Инстанс [[MSrvFileInfo]].
    * @return Результат валидации с почищенным инстансом [[MSrvFileInfo]].
    */
  def validateC2sForStore(fi: MSrvFileInfo): StringValidationNel[MSrvFileInfo] = {
    Validation
      .liftNel(fi.nodeId)( {nodeId => !nodeId.matches("^[a-z0-9A-Z_-]{10,50}$") }, "e.nodeid." + ErrorConstants.Words.INVALID )
      .map { nodeId =>
        MSrvFileInfo( nodeId = nodeId )
      }
  }

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
                         url        : Option[String]      = None,
                         // TODO Поля ниже очень сильно дублируют n2 MFileMeta (MMedia.file). Сходу не удаётся унифицировать из-за поля date_created.
                         sizeB      : Option[Long]        = None,
                         name       : Option[String]      = None,
                         mimeType   : Option[String]      = None,
                         hashesHex  : HashesHex           = Map.empty,
                         colors     : Option[MHistogram]  = None,
                         whPx       : Option[MSize2di]    = None
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
        url = {
          val urls = newInfo.url ++ url
          urls.headOption.flatMap { _ =>
            StringUtil.firstStringMakesSence(urls.toSeq: _*)
          }
        },
        sizeB = newInfo.sizeB
          .orElse(sizeB),
        name = newInfo.name
          .orElse( name ),
        mimeType = newInfo.mimeType
          .orElse( mimeType ),
        hashesHex = (newInfo.hashesHex :: hashesHex :: Nil)
          .find(_.nonEmpty)
          .getOrElse(hashesHex),
        colors = newInfo.colors.orElse( colors ),
        whPx = newInfo.whPx
          .orElse(whPx)
      )
    }
  }

}

