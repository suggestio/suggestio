package io.suggest.crypto.hash

import io.suggest.scalaz.ScalazUtil
import io.suggest.ueq.UnivEqUtil._
import play.api.libs.json._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 15:18
  * Description: Псевдо-модель вокруг карты хешей и их hex-значений.
  */
object HashesHex {

  /**
    * Поддержка play-json для таблицы хэшей, передаваемой через сеть.
    * Максимально компактный формат, но он НЕ подходит для индексирования в ES.
    */
  implicit def MHASHES_HEX_FORMAT_TRASPORT: OFormat[HashesHex] = {
    val r = Reads.mapReads[MHash, String] { k =>
      MHashes
        .withValueOpt(k)
        .fold[JsResult[MHash]](JsError("e.hash.type"))(JsSuccess(_))
    }

    val w = Writes.mapWrites[String]
      .contramap[Map[MHash, String]] { hashesHex =>
        hashesHex.map { case (k,v) => (k.value, v) }
      }

    OFormat(r, w)
  }



  import scalaz._
  import scalaz.syntax.apply._
  import scalaz.std.string._
  import scalaz.std.iterable._

  def hexRe = "[a-f0-9]+".r

  def hashHexPairV(hashHex: HashHex): ValidationNel[String, String] = {
    val hash = hashHex._1
    Validation.liftNel(hashHex._2)(
      {hex =>
        val isOk = hex.length ==* hash.hexStrLen &&
          hexRe.pattern.matcher(hex).matches()
        !isOk
      },
      s"e.hash.$hash.hex.format"
    )
  }


  /** ScalaZ-валидация карты хэшей с помощью множества ожидаемых хэшей.
    *
    * @param hashesHex Карта хэшей, присланная клиентом.
    * @param mustBeHashes Ожидаемые типы хэшей.
    * @return Результат валидации.
    */
  def hashesHexV(hashesHex: HashesHex, mustBeHashes: Set[MHash]): ValidationNel[String, HashesHex] = {
    val ePrefix = "e.hashes."
    (
      Validation.liftNel( hashesHex.size )( { _ !=* mustBeHashes.size }, ePrefix + "size" ) |@|
      Validation.liftNel( hashesHex.keySet ) ( { _ !=* mustBeHashes }, ePrefix + "set" ) |@|
      ScalazUtil.validateAll(hashesHex.to(Iterable))(hashHexPairV)
    ) { (_,_,_) => hashesHex }
  }

}
