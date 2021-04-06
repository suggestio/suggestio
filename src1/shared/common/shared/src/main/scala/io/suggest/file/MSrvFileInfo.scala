package io.suggest.file

import io.suggest.common.empty.EmptyUtil
import io.suggest.crypto.hash.HashesHex
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.n2.media.{MFileMeta, MFileMetaHash, MPictureMeta}
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.media.storage.MStorageInfo
import io.suggest.scalaz.StringValidationNel
import io.suggest.text.StringUtil
import io.suggest.ueq.UnivEqUtil._
import scalaz.syntax.validation._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 17:56
  * Description: Кросс-платформенная модель с инфой о файле, хранящемся на сервере.
  * В случае картинки подразумевается оригинал этой самой картинки.
  */
object MSrvFileInfo {

  def default = apply()

  /** Поддержка play-json для связи между клиентом и сервером. */
  implicit def srvFileInfoJson: OFormat[MSrvFileInfo] = (
    (__ \ "u").formatNullable[String] and
    (__ \ "a").formatNullable[String] and
    (__ \ "f").format[MFileMeta] and
    (__ \ "p").formatNullable[MPictureMeta]
      .inmap[MPictureMeta](
        EmptyUtil.opt2ImplMEmptyF( MPictureMeta ),
        EmptyUtil.implEmpty2Opt
      ) and
    (__ \ "s").formatNullable[MStorageInfo] and
    (__ \ "n").formatNullable[String]
  )(apply, unlift(unapply))


  /** Провалидировать инстанс [[MSrvFileInfo]] перед сохранением на сервер.
    * По факту, все поля не нужны, а обязательный nodeId живёт на другом уровне.
    *
    * @param fi Инстанс [[MSrvFileInfo]].
    * @return Результат валидации с почищенным инстансом [[MSrvFileInfo]].
    */
  def validateNodeIdOnly(fi: MSrvFileInfo): StringValidationNel[MSrvFileInfo] = {
    default.successNel
  }

  @inline implicit def univEq: UnivEq[MSrvFileInfo] = UnivEq.derive

}


/** Класс модели данных о файле, хранящемся на сервере.
  *
  * @param url Ссылка для скачивания файла.
  * @param name Пользовательское название файла, если есть.
  * @param fileMeta Метаданные файла, которые могут быть нужны на клиенте.
  */
case class MSrvFileInfo(
                         url            : Option[String]          = None,
                         name           : Option[String]          = None,
                         // TODO Всё, что ниже - это MEdgeMedia. Заменить на модель тут. Всё, что выше - это куски MEdge. Может вообще снести эту модель?
                         fileMeta       : MFileMeta               = MFileMeta.empty,
                         pictureMeta    : MPictureMeta            = MPictureMeta.empty,
                         storage        : Option[MStorageInfo]    = None,
                         // Для совместимости с версиями приложений <=4.2.0, здесь располагается nodeId, на новых - на уровень выше.
                         nodeId_legacy42: Option[String]          = None,
                       ) {

  /** Карта хэшей генерится на основе всех имеющихся в исходнике хэшей. */
  lazy val hashesHex: HashesHex = {
    MFileMetaHash.toHashesHex( fileMeta.hashesHex )
  }

  /** Объединить данные данного инстанса и данные из более свежего инстанса.
    * Сервер может не утруждать себя сбором данных, которые есть на клиенте.
    * Поэтому с сервера могут приходить неполные данные, и их следует мержить.
    *
    * Следует не забывать про nodeID, которого здесь нет.
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
        fileMeta = newInfo.fileMeta,
        pictureMeta =
          if (newInfo.pictureMeta.isEmpty) this.pictureMeta
          else newInfo.pictureMeta,
      )
    }
  }

}

