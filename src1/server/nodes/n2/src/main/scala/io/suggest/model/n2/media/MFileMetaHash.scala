package io.suggest.model.n2.media

import io.suggest.common.empty.EmptyUtil
import io.suggest.crypto.hash.{HashHex, HashesHex, MHash}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil._
import io.suggest.primo.id.IId
import japgolly.univeq.UnivEq
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
  with IGenEsMappingProps
{

  object Fields {
    val HASH_TYPE_FN = "t"
    val HEX_VALUE_FN = "x"
    val FLAGS_FN     = "f"
  }

  /** Допустимые значения флагов. */
  object Flags {

    /** Флаг истинного оригинала (который был залит на сервер). */
    val TRULY_ORIGINAL              = 1.toShort

    /** Флаг lossless-дериватива из оригинала (причёсанный и почищенный оригинал). */
    val LOSSLESS_DERIVATIVE         = TRULY_ORIGINAL * 2

  }


  /** Поддержка play-json для модели. */
  implicit val MFILE_META_HASH_FORMAT: OFormat[MFileMetaHash] = {
    val F = Fields
    (
      (__ \ F.HASH_TYPE_FN).format[MHash] and
      (__ \ F.HEX_VALUE_FN).format[String] and
      (__ \ F.FLAGS_FN).formatNullable[Set[Short]]
        .inmap[Set[Short]](
          // Изначально, никаких флагов не было. Поэтому имитируем наличие флага TRULY_ORIGINAL. TODO Надо resaveMany() для окончательной фиксации флагов.
          EmptyUtil.opt2ImplEmpty1F( Set(Flags.TRULY_ORIGINAL) ),
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

  /** Список ES-полей модели. */
  override def generateMappingProps: List[DocField] = {
    val F = Fields
    List(
      FieldKeyword(F.HASH_TYPE_FN, index = true, include_in_all = false),
      FieldKeyword(F.HEX_VALUE_FN, index = true, include_in_all = true),
      FieldNumber(F.FLAGS_FN, fieldType = DocFieldTypes.short, index = true, include_in_all = false)
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

}


/** Класс ES-модели инфы по одному хешу файла.
  *
  * @param hType Тип хеша (алгоритм хеширования).
  * @param hexValue Вычисленный хеш.a
  * @param flags Разные флаги.
  */
case class MFileMetaHash(
                          hType           : MHash,
                          hexValue        : String,
                          flags           : Set[Short]
                        )
  extends IId[MHash]
{

  /** Конверсия в ключ-значение. */
  def hashHexTuple: HashHex = (hType, hexValue)

  override def id = hType

}
