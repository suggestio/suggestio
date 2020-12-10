package io.suggest.media

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.17 9:43
  * Description: Модель типов элементов визуальной галлереи.
  */

object MMediaTypes extends StringEnum[MMediaType] {

  /** Картинка (изображение). */
  case object Image extends MMediaType("i")

  // TODO video, audio, etc...

  override def values = findValues

}


/** Класс одного элемента модели. */
sealed abstract class MMediaType(override val value: String) extends StringEnumEntry {

  override final def toString = value

}


object MMediaType {

  /** Поддержка play-json. */
  implicit def MMEDIA_TYPE_FORMAT: Format[MMediaType] = {
    EnumeratumUtil.valueEnumEntryFormat( MMediaTypes )
  }

  @inline implicit def univEq: UnivEq[MMediaType] = UnivEq.derive

}

