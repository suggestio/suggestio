package io.suggest.text

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format


/** Модель списка допустимых значений для выравнивания текста. */
object MTextAligns extends StringEnum[MTextAlign] {

  // Left, Right, Center, Justify мигрированы с прошлой модели, их id должны быть жестко зафиксированы.

  /** Выравнивание по левому краю. */
  case object Left extends MTextAlign("l") {
    override def quillName = None
    override def cssName  = "left"
  }

  /** Выравнивание по правому краю. */
  case object Right extends MTextAlign("r") {
    override def cssName  = "right"
  }

  /** Выравнивание по центру. */
  case object Center extends MTextAlign("c") {
    override def cssName  = "center"
  }

  /** Выравнивание по ширине. */
  case object Justify extends MTextAlign("j") {
    override def cssName  = "justify"
  }


  override val values = findValues

  def withCssNameOption(cssName: String): Option[MTextAlign] = {
    values
      .find { _.cssName equalsIgnoreCase cssName }
  }


  /** Поиск по возможному quill-идентификатору (аттрибут align) */
  def withQuillNameOpt(quillName: Option[String]): Option[MTextAlign] = {
    values
      .find { _.quillName == quillName }
  }

  def withQuillName(quillName: String): Option[MTextAlign] = {
    values
      .find { _.quillName contains quillName }
  }

}


/** Класс одного элемента модели типов выравниваний текста. */
sealed abstract class MTextAlign(override val value: String) extends StringEnumEntry {

  /** Название CSS-класса. */
  def cssName: String

  /** quill-редактор считает дефолтом left-выравнивание, и там значение None.
    * Для остальных режимов используется Some(cssName).
    */
  def quillName: Option[String] = Some(cssName)

  override final def toString = value

}


/** Статическая поддержка для классов модели [[MTextAlign]]. */
object MTextAlign {

  /** Поддержка play-json для всех элементов модели [[MTextAligns]]. */
  implicit val MTEXT_ALIGN_FORMAT: Format[MTextAlign] = {
    EnumeratumUtil.valueEnumEntryFormat( MTextAligns )
  }

  implicit def univEq: UnivEq[MTextAlign] = UnivEq.derive

}


