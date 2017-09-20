package io.suggest.jd.tags

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 12:21
  * Description: Doc Tag Name -- статически-типизированное имя (название) тега.
  *
  * В начальной реализации jd-тегов, эти теги были по факту только на уровне JSON.
  *
  * После унификации всех тегов воедино, эта модель стала настоящим именем тега, используемым везде.
  */

object MJdTagNames extends StringEnum[MJdTagName] {

  /** Документ - это корневой "тег" структуры документа.
    * С него начинается любой "документ" рекламной карточки.
    */
  case object DOCUMENT extends MJdTagName("d")

  /** "Полоса" карточки, т.е. элемент вертикальной разбивки документа наподобии
    * абзаца или страницы.
    */
  case object STRIP extends MJdTagName("s")

  /** Тег, хранящий текст, отформатированный через quill-editor. */
  case object QUILL_DELTA extends MJdTagName("q")


  // ------------------------------------------------------------------------------

  /** Все элементы модели. */
  override val values = findValues

}


/** Класс каждого элемента модели типов структуры документа. */
sealed abstract class MJdTagName(override val value: String) extends StringEnumEntry {

  override final def toString = value

}


object MJdTagName {

  /** Поддержка play-json. */
  implicit val MDOC_TAG_NAME_FORMAT: Format[MJdTagName] = {
    EnumeratumUtil.valueEnumEntryFormat( MJdTagNames )
  }

  implicit def univEq: UnivEq[MJdTagName] = UnivEq.derive

}

