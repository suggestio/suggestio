package io.suggest.file

import io.suggest.common.empty.EmptyUtil
import io.suggest.crypto.hash.{HashesHex, MHash}
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.err.ErrorConstants
import io.suggest.model.n2.media.{MFileMeta, MFileMetaHash, MPictureMeta}
import io.suggest.msg.ErrorMsgs
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
  implicit def srvFileInfoJson: OFormat[MSrvFileInfo] = (
    (__ \ "n").format[String] and
    (__ \ "u").formatNullable[String] and
    (__ \ "a").formatNullable[String] and
    (__ \ "f").formatNullable[MFileMeta] and
    (__ \ "p").formatNullable[MPictureMeta]
      .inmap[MPictureMeta](
        EmptyUtil.opt2ImplMEmptyF( MPictureMeta ),
        EmptyUtil.implEmpty2Opt
      )
  )(apply, unlift(unapply))


  /** Провалидировать инстанс [[MSrvFileInfo]] перед сохранением на сервер.
    * По факту, все поля кроме nodeId не нужны.
    *
    * @param fi Инстанс [[MSrvFileInfo]].
    * @return Результат валидации с почищенным инстансом [[MSrvFileInfo]].
    */
  def validateC2sForStore(fi: MSrvFileInfo): StringValidationNel[MSrvFileInfo] = {
    Validation
      .liftNel(fi.nodeId)(
        {nodeId =>
          !nodeId.matches("^[a-z0-9A-Z_-]{10,50}$")
        },
        "e.nodeid." + ErrorConstants.Words.INVALID + ": " + fi.nodeId
      )
      .map { nodeId =>
        MSrvFileInfo( nodeId = nodeId )
      }
  }

  @inline implicit def univEq: UnivEq[MSrvFileInfo] = UnivEq.derive

}


/** Класс модели данных о файле, хранящемся на сервере.
  *
  * @param nodeId Уникальный id узла, в т.ч. эфемерный.
  * @param url Ссылка для скачивания файла.
  * @param name Пользовательское название файла, если есть.
  * @param fileMeta Метаданные файла, которые могут быть нужны на клиенте.
  */
case class MSrvFileInfo(
                         nodeId     : String,
                         url        : Option[String]      = None,
                         // TODO Поля ниже очень сильно дублируют n2 MFileMeta (MMedia.file). Сходу не удаётся унифицировать из-за поля date_created.
                         name       : Option[String]      = None,
                         fileMeta   : Option[MFileMeta]   = None,
                         pictureMeta: MPictureMeta        = MPictureMeta.empty,
                       ) {

  /** Карта хэшей генерится на основе всех имеющихся в исходнике хэшей. */
  lazy val hashesHex: HashesHex = {
    fileMeta.fold( Map.empty[MHash, String] ) { fMeta =>
      MFileMetaHash.toHashesHex( fMeta.hashesHex )
    }
  }

  /** Объединить данные данного инстанса и данные из более свежего инстанса.
    * Сервер может не утруждать себя сбором данных, которые есть на клиенте.
    * Поэтому с сервера могут приходить неполные данные, и их следует мержить.
    *
    * @param newInfo Обновлённый инстанс [[MSrvFileInfo]], содержащий более актуальные данные.
    * @return Объединённый экземпляр [[MSrvFileInfo]].
    */
  def updateFrom(newInfo: MSrvFileInfo): MSrvFileInfo = {
    if (newInfo ===* this) {
      // По идее, такого быть не должно: обновление без обновлений должно быть отфильтровано где-то выше по стактрейсу.
      throw new IllegalArgumentException( ErrorMsgs.SHOULD_NEVER_HAPPEN )
    } else {
      // БЕЗ copy(), чтобы при добавлении новых полей тут сразу подсвечивалась ошибка.
      MSrvFileInfo(
        nodeId = newInfo.nodeId,
        // Велосипед для фильтрации корректных ссылок.
        url = {
          val urls = (newInfo.url #:: url #:: LazyList.empty)
            .flatten
          urls
            .headOption
            .flatMap { _ =>
              StringUtil.firstStringMakesSence(urls.toSeq: _*)
            }
        },
        name = newInfo.name
          .orElse( name ),
        fileMeta = newInfo.fileMeta
          .fold( this.fileMeta )(_ => newInfo.fileMeta),
        pictureMeta =
          if (newInfo.pictureMeta.isEmpty) this.pictureMeta
          else newInfo.pictureMeta,
      )
    }
  }

}

