package io.suggest.n2.media.storage

import io.suggest.common.empty.EmptyUtil
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.swfs.fid.Fid
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.12.2019 17:06
  * Description: Модель-контейнер мета-данных по любому хранилищу в контексте этого хранилища.
  * Возможно, в будущем тут надо будет организовать хранение различных данных.
  */
object MStorageInfoData extends IEsMappingProps {

  object Fields {
    def META_FN = "d"
    def SHARDS_FN = "h"
  }

  implicit def mediaStorageInfoDataJson: OFormat[MStorageInfoData] = {
    val F = Fields
    (
      (__ \ F.META_FN).format[String] and
      (__ \ F.SHARDS_FN).formatNullable[Set[String]]
        .inmap[Set[String]](
          EmptyUtil.opt2ImplEmptyF( Set.empty ),
          shards => Option.when( shards.nonEmpty )(shards)
        )
    )(apply, unlift(unapply))
  }


  @inline implicit def univEq: UnivEq[MStorageInfoData] = UnivEq.derive

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      // Индексируем идентификатор файла в хранилище, чтобы можно было делать GC по хранилищу:
      // получать от хранилища листинг всех файлов, прогнать каждый файл по поиску, выявить и удалить лишние файлы из хранилища.
      F.META_FN -> FKeyWord.indexedJs,
      // Хосты индексируем просто на будущее. Это не для всех хранилищ, и индекс будет очень маленький и не уникальный.
      F.SHARDS_FN -> FKeyWord.indexedJs,
    )
  }


  def meta = GenLens[MStorageInfoData](_.meta)
  def shards = GenLens[MStorageInfoData](_.shards)

}


/** @param shards Данные шарды или хоста в рамках хранилища, если необходимо.
  *               Для SWFS -- это volume id (например "3" для FID="3,425adeff53").
  *               Для HostPath-хранилищ -- id хостов.
  * @param meta Текстовый указатель (метаданные хранилища).
  *             Для swfs - это fid.
  *             Для ассета - это неизменяемая часть названия.
  */
final case class MStorageInfoData(
                                   meta      : String,
                                   shards    : Set[String]      = Set.empty,
                                 ) {

  /** Кэширование распарсенного SeaweedFS FID. */
  lazy val swfsFid: Option[Fid] =
    Fid.parse( meta )

  override def toString: String = {
    val sb = new StringBuilder(32, meta)

    if (shards.nonEmpty) {
      sb.append('[')
      shards.foreach { host =>
        sb.append(host)
          .append(",")
      }
      sb.setLength(sb.length() - 1)
      sb.append(']')
    }

    sb.toString()
  }

}
