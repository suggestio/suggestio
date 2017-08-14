package io.suggest.text

import enumeratum._
import io.suggest.enum2.EnumeratumUtil
import io.suggest.primo.IStrId
import play.api.libs.json.Format


/** Статическая поддержк для классов модели [[MTextAlign]]. */
object MTextAlign {

  /** Поддержка play-json для всех элементов модели [[MTextAligns]]. */
  implicit val MTEXT_ALIGN_FORMAT: Format[MTextAlign] = {
    EnumeratumUtil.enumEntryFormat( MTextAligns )
  }

}


/** Класс одного элемента модели типов выравниваний текста. */
sealed abstract class MTextAlign extends EnumEntry with IStrId {

  /** Название CSS-класса. */
  def cssName: String

  override final def toString = super.toString

}


/** Модель списка допустимых значений для выравнивания текста. */
object MTextAligns extends Enum[MTextAlign] {

  // Left, Right, Center, Justify мигрированы с прошлой модели, их id должны быть жестко зафиксированы.

  /** Выравнивание по левому краю. */
  case object Left extends MTextAlign {
    override def strId    = "l"
    override def cssName  = "left"
  }

  /** Выравнивание по правому краю. */
  case object Right extends MTextAlign {
    override def strId    = "r"
    override def cssName  = "right"
  }

  /** Выравнивание по центру. */
  case object Center extends MTextAlign {
    override def strId    = "c"
    override def cssName  = "center"
  }

  /** Выравнивание по ширине. */
  case object Justify extends MTextAlign {
    override def strId    = "j"
    override def cssName  = "justify"
  }


  override val values = findValues

  def withCssNameOption(cssName: String): Option[MTextAlign] = {
    values
      .find { _.cssName equalsIgnoreCase cssName }
      .asInstanceOf[Option[MTextAlign]]
  }

}
