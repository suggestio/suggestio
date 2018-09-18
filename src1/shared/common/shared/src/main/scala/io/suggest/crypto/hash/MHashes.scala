package io.suggest.crypto.hash

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 10:35
  * Description: Модель криптографических хэшей.
  */
object MHashes extends StringEnum[MHash] {

  private def SHA_ = "SHA-"

  /** SHA-1 хэш.
    * @see [[https://ru.wikipedia.org/wiki/SHA-1]] */
  case object Sha1 extends MHash("s1") {
    override def byteSize = 20
    override def fullStdName = SHA_ + "1"
  }

  /** SHA-256 хэш.
    * @see [[https://ru.wikipedia.org/wiki/SHA-256]] */
  case object Sha256 extends MHash("s256") {
    override def byteSize = 32
    override def fullStdName = SHA_ + bitSize
  }


  override def values = findValues

}


/** Класс описания одного хэша. */
sealed abstract class MHash(override val value: String) extends StringEnumEntry {

  override final def toString = value

  /** Кол-во байт в выхлопе. */
  def byteSize: Int

  /** Длина hex-строки. hex-кодирование подразумевает 2 символа на 1 байт. */
  def hexStrLen = byteSize * 2

  /** Размер хэша в битах: в 1 байте - 8 бит. */
  def bitSize = byteSize * 8

  /** Полное стандартное имя. */
  def fullStdName: String

}


object MHash {

  /** Поддержка play-json. */
  implicit val MHASH_FORMAT: Format[MHash] = {
    EnumeratumUtil.valueEnumEntryFormat( MHashes )
  }

  /** Поддержка UnivEq. */
  @inline implicit def univEq: UnivEq[MHash] = UnivEq.derive

}
