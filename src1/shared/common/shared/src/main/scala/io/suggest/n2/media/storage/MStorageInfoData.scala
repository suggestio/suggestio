package io.suggest.n2.media.storage

import io.suggest.common.empty.EmptyUtil
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.swfs.fid.Fid
import japgolly.univeq._
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
    def DATA_FN = "d"
    def HOST_FN = "h"
  }

  implicit def mediaStorageInfoDataJson: OFormat[MStorageInfoData] = {
    val F = Fields
    (
      (__ \ F.DATA_FN).format[String] and
      (__ \ F.HOST_FN).formatNullable[Seq[String]]
        .inmap[Seq[String]](
          EmptyUtil.opt2ImplEmptyF( Seq.empty ),
          hosts => Option.when(hosts.nonEmpty)(hosts)
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
      F.DATA_FN -> FKeyWord.indexedJs,
      // Хосты индексируем просто на будущее. Это не для всех хранилищ, и индекс будет очень маленький и не уникальный.
      F.HOST_FN -> FKeyWord.indexedJs,
    )
  }

}


/** @param hosts Данные хоста, если необходимо.
  *              Поле предусмотрено для HostPath-хранилища, которое может быть ограничено по хостам.
  * @param data Текстовый указатель
  *             Для swfs - это fid.
  *             Для ассета - это неизменяемая часть названия.
  *             Для хост-файла - это путь до файла, опционально с указанием хоста: /path/to/file, ./file, s2:/path/to
  */
final case class MStorageInfoData(
                                   data      : String,
                                   hosts     : Seq[String]      = Nil,
                                 ) {

  /** Кэширование распарсенного SeaweedFS FID. */
  lazy val swfsFid: Option[Fid] =
    Fid.parse( data )

}
