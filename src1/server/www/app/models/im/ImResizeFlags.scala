package models.im

import enumeratum.values.{CharEnum, CharEnumEntry}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.01.18 14:39
  * Description: Модель допустимых флагов [[AbsResizeOp]].
  */

sealed abstract class ImResizeFlag(override val value: Char) extends CharEnumEntry {
  def imChar: Char
  final def urlSafeChar: Char = value
}


/** Модель флагов ресайза. */
object ImResizeFlags extends CharEnum[ImResizeFlag] {

  object IgnoreAspectRatio extends ImResizeFlag('a') {
    override def imChar = '!'
  }

  object OnlyShrinkLarger extends ImResizeFlag('b') {
    override def imChar = '>'
  }

  object OnlyEnlargeSmaller extends ImResizeFlag('c') {
    override def imChar = '<'
  }

  /** resize the image based on the smallest fitting dimension. */
  object FillArea extends ImResizeFlag('d') {
    override def imChar = '^'
  }

  // Другие режимы ресайза тут пока опущены, т.к. не подходят для AbsResize, а другой пока нет.

  override val values = findValues

  def maybeWithName(s: String): Option[ImResizeFlag] = {
    withValueOpt( s.charAt(0) )
  }

}

