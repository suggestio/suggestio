package io.suggest.jd.tags

import enumeratum._
import io.suggest.enum2.EnumeratumUtil
import io.suggest.primo.IStrId
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
    EnumeratumUtil.enumEntryFormat( MJdTagNames )
  }

}


/** Класс каждого элемента модели типов структуры документа. */
sealed abstract class MDtName extends EnumEntry with IStrId {

  override final def toString = super.toString

}


/** Модель типов элементов структуры документа. */
object MJdTagNames extends Enum[MDtName] {

  /** Документ - это корневой "тег" структуры документа.
    * С него начинается любой "документ" рекламной карточки.
    */
  case object DOCUMENT extends MDtName {
    override def strId = "d"
  }

  /** "Полоса" карточки, т.е. элемент вертикальной разбивки документа наподобии
    * абзаца или страницы.
    */
  case object STRIP extends MDtName {
    override def strId = "s"
  }

  /** Отсылка к ресурсу, который живёт вне этого дерева.
    *
    * Хранение вне дерева и линковка по id ресурса является простым решением проблемы с индексацией,
    * поиском и highlight'ом некоторых частей документа без дубликации этих самых частей.
    */
  case object PLAIN_PAYLOAD extends MDtName {
    override def strId = "p"
  }

  /** Картинка.
    * Фоновая или нет -- не суть важно, это описывается параметрами самой картинки.
    */
  case object PICTURE extends MDtName {
    override def strId = "i"
  }


  /** Имя тега абсолютного позиционирования элемента. */
  case object ABS_POS extends MDtName {
    override def strId = "a"
  }


  /** Все элементы модели в исходном порядке. */
  override val values = findValues

}
