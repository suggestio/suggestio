package io.suggest.model.n2.media

import io.suggest.crypto.hash.{HashHex, HashesHex, MHash}
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil._
import io.suggest.primo.id.IId
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 19:18
  * Description: ES-модель для представления хеша файла.
  * Является ES-аналогом для модели [[io.suggest.crypto.hash.HashHex]].
  */
object MFileMetaHash extends IGenEsMappingProps {

  object Fields {
    val HASH_TYPE_FN = "t"
    val HEX_VALUE_FN = "x"
  }


  /** Поддержка play-json для модели. */
  implicit val MFILE_META_HASH_FORMAT: OFormat[MFileMetaHash] = {
    val F = Fields
    (
      (__ \ F.HASH_TYPE_FN).format[MHash] and
      (__ \ F.HEX_VALUE_FN).format[String]
    )(apply, unlift(unapply))
  }


  /** Список ES-полей модели. */
  override def generateMappingProps: List[DocField] = {
    val F = Fields
    List(
      FieldKeyword(F.HASH_TYPE_FN, index = true, include_in_all = false),
      FieldKeyword(F.HEX_VALUE_FN, index = true, include_in_all = true)
    )
  }


  /** Сконвертить список [[MFileMetaHash]] в карту хешей. */
  def toHashesHex(hhs: TraversableOnce[MFileMetaHash]): HashesHex = {
    hhs.toIterator
      .map(_.hashHexTuple)
      .toMap
  }

}


/** Класс ES-модели инфы по одному хешу файла.
  *
  * @param hType Тип хеша (алгоритм хеширования).
  * @param hexValue Вычисленный хеш.
  */
case class MFileMetaHash(
                          hType     : MHash,
                          hexValue  : String
                        )
  extends IId[MHash]
{

  /** Конверсия в ключ-значение. */
  def hashHexTuple: HashHex = (hType, hexValue)

  override final def id = hType

}
