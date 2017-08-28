package io.suggest.jd.tags

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 12:21
  * Description: Doc Tag Name -- статически-типизированное имя (название) тега.
  */
object MDtName {

  /** Поддержка play-json. */
  implicit val MDOC_TAG_NAME_FORMAT: Format[MDtName] = {
    EnumeratumUtil.valueEnumEntryFormat( MJdTagNames )
  }

}


/** Класс каждого элемента модели типов структуры документа. */
sealed abstract class MDtName(override val value: String) extends StringEnumEntry {

  override final def toString = value

}


/** Модель типов элементов структуры документа. */
object MJdTagNames extends StringEnum[MDtName] {

  /** Документ - это корневой "тег" структуры документа.
    * С него начинается любой "документ" рекламной карточки.
    */
  case object DOCUMENT extends MDtName("d")

  /** "Полоса" карточки, т.е. элемент вертикальной разбивки документа наподобии
    * абзаца или страницы.
    */
  case object STRIP extends MDtName("s")

  /** Отсылка к ресурсу, который живёт вне этого дерева.
    *
    * Хранение вне дерева и линковка по id ресурса является простым решением проблемы с индексацией,
    * поиском и highlight'ом некоторых частей документа без дубликации этих самых частей.
    */
  case object PLAIN_PAYLOAD extends MDtName("p")

  /** Картинка.
    * Фоновая или нет -- не суть важно, это описывается параметрами самой картинки.
    */
  case object PICTURE extends MDtName("i")


  /** Имя тега абсолютного позиционирования элемента. */
  case object ABS_POS extends MDtName("a")


  /** line BReak, в частности br-тег в html. */
  case object LINE_BREAK extends MDtName("b")


  /** Название тега текстового элемента. */
  case object TEXT extends MDtName("t")

  // ------------------------------------------------------------------------------

  /** Все элементы модели. */
  override val values = findValues

}
