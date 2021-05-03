package io.suggest.jd.tags

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.color.IColorPickerMarker
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq._
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
  case object STRIP extends MJdTagName("s") with IColorPickerMarker

  /** Тег, хранящий текст, отформатированный через quill-editor. */
  case object QD_CONTENT extends MJdTagName("q") with IColorPickerMarker

  /** Тег одной qd-операций.
    * Ранее, операции лежали внутри jdt.props1.qdOps, но пришлось запихать из children.
    */
  case object QD_OP extends MJdTagName("o")

  // TODO HTML, когда поддержка реализуется.

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

  @inline implicit def univEq: UnivEq[MJdTagName] = UnivEq.derive


  implicit class MJdTagNameOpsExt(val jdtn: MJdTagName) extends AnyVal {

    /** Допустимо ли использовать фоновое изображение для указанного тега? */
    def isBgImgAllowed: Boolean = {
      jdtn ==* MJdTagNames.STRIP
    }

    def isEventsAllowed: Boolean = {
      // TODO Добавить поддержку qd_op
      jdtn match {
        case MJdTagNames.STRIP | MJdTagNames.QD_CONTENT => true
        case _ => false
      }
    }

  }

}

