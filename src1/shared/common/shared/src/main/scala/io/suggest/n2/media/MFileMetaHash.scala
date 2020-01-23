package io.suggest.n2.media

import io.suggest.common.empty.EmptyUtil
import io.suggest.crypto.hash.{HashHex, HashesHex, MHash}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.primo.id.IId
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 19:18
  * Description: ES-модель для представления хеша файла.
  * Является ES-аналогом для модели [[io.suggest.crypto.hash.HashHex]].
  */
object MFileMetaHash
  extends IEsMappingProps
{

  object Fields {
    val HASH_TYPE_FN = "t"
    val HEX_VALUE_FN = "x"
    val FLAGS_FN     = "f"
  }


  /** Поддержка play-json для модели. */
  implicit def fileMetaHashJson: OFormat[MFileMetaHash] = {
    val F = Fields
    (
      (__ \ F.HASH_TYPE_FN).format[MHash] and
      (__ \ F.HEX_VALUE_FN).format[String] and
      (__ \ F.FLAGS_FN).formatNullable[Set[MFileMetaHashFlag]]
        .inmap[Set[MFileMetaHashFlag]](
          // Изначально, никаких флагов не было. Поэтому имитируем наличие флага TRULY_ORIGINAL. TODO Надо resaveMany() для окончательной фиксации флагов.
          EmptyUtil.opt2ImplEmpty1F( Set(MFileMetaHashFlags.TrulyOriginal) ),
          { flags => Option.when(flags.nonEmpty)(flags) }
        )
    )(apply, unlift(unapply))
  }

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.HASH_TYPE_FN -> FKeyWord.indexedJs,
      F.HEX_VALUE_FN -> FKeyWord.indexedJs,
      F.FLAGS_FN     -> FNumber(
        typ   = DocFieldTypes.Short,
        index = someTrue,
      )
    )
  }

  /** Сконвертить список [[MFileMetaHash]] в карту хешей. */
  def toHashesHex(hhs: IterableOnce[MFileMetaHash]): HashesHex = {
    hhs
      .iterator
      .map(_.hashHexTuple)
      .toMap
  }

  @inline implicit def univEq: UnivEq[MFileMetaHash] = UnivEq.derive

  lazy val hexValue = GenLens[MFileMetaHash](_.hexValue)
  lazy val flags = GenLens[MFileMetaHash](_.flags)

}


/** Класс ES-модели инфы по одному хешу файла.
  *
  * @param hType Тип хеша (алгоритм хеширования).
  * @param hexValue Вычисленный хеш.a
  * @param flags Разные флаги.
  */
final case class MFileMetaHash(
                                hType           : MHash,
                                hexValue        : String,
                                flags           : Set[MFileMetaHashFlag],
                              )
  extends IId[MHash]
{

  /** Конверсия в ключ-значение. */
  def hashHexTuple: HashHex = (hType, hexValue)

  override def id = hType

}
