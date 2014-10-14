package models

import play.api.mvc.{QueryStringBindable, PathBindable}
import play.twirl.api.{Template1, HtmlFormat}
import views.html.blocks.svg._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.09.14 18:20
 * Description: Карта названий шаблонов и самих шаблонов.
 */
object BlocksSvg extends Enumeration {

  protected case class Val(
    name: String,
    template: Template1[BSvgColorMap, HtmlFormat.Appendable]
  ) extends super.Val(name) {
    def render(colors: BSvgColorMap) = template.render(colors)
  }

  type BlockSvg = Val

  // Перечисление шаблонов и зацепок к ним.
  val bg19                = Val("bg19", _bg19Tpl)
  val mask23              = Val("m23",  _mask23)
  val mask24              = Val("m24",  _mask24)

  implicit def value2val(x: Value): BlockSvg = x.asInstanceOf[BlockSvg]

  def maybeWithName(x: String): Option[BlockSvg] = {
    values
      .find(_.name == x)
      .asInstanceOf[Option[BlockSvg]]
  }

  implicit def pathBindable = {
    new PathBindable[BlockSvg] {
      override def bind(key: String, value: String): Either[String, BlockSvg] = {
        maybeWithName(value)
          .fold [Either[String, BlockSvg]] ( Left("No or invalid svg filename") ) (Right(_))
      }

      override def unbind(key: String, value: BlockSvg): String = {
        value.name
      }
    }
  }

}


/** Названия цветов, используемых в списке args. */
object BSvgColorNames extends Enumeration {
  type BSvgColorName = Value

  val Fill   = Value("f")
  val Border = Value("b")
  val Line   = Value("l")
  val Bg     = Value("g")

  def maybeWithName(x: String): Option[BSvgColorName] = {
    values
      .find(_.toString == x)
      .asInstanceOf[Option[BSvgColorName]]
  }

  implicit def qsb(implicit strB: QueryStringBindable[String]) = {
    new QueryStringBindable[BSvgColorName] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, BSvgColorName]] = {
        strB.bind(key, params).flatMap {
          case Right(raw) =>
            maybeWithName(raw) match {
              case Some(bscn) => Some(Right(bscn))
              case None       => Some(Left("Unknown color code."))
            }
          case Left(msg) => Some(Left(msg))
        }
      }

      override def unbind(key: String, value: BSvgColorName): String = {
        strB.unbind(key, value.toString)
      }
    }
  }
}


object BSvgColorsUtil {

  /** Карта цветов, которая передаётся для рендера svg-шаблона. */
  type BSvgColorMap = Map[BSvgColorName, String]

  /** Система сборки/разборки карты цветов из/в query string. */
  implicit def qsb(implicit strB: QueryStringBindable[String]) = {
    new QueryStringBindable[BSvgColorMap] {

      /** Десериализация карты цветов из query string. */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, BSvgColorMap]] = {
        val prefix = key + "."
        val result: BSvgColorMap = params
          .iterator
          // Оставить в карте qs только то, что начинается на ключ.
          .filter { case (k, _) => k.startsWith(prefix) }
          // Выбрать в списке значений первый цвет (первая непустая строка значения)
          .flatMap { case (k, vs) => vs.find(!_.isEmpty).map { k -> _ } }
          // Распарить название цвета.
          .flatMap { case (colorNameRawPrefixed, color) =>
            val colorNameRaw = colorNameRawPrefixed.substring(prefix.length)
            BSvgColorNames.maybeWithName(colorNameRaw) map { cn =>
              cn -> color
            }
          }
          // Обратно в карту цветов.
          .toMap
        // svg-шаблоны без хотя бы одного цвета не рендерятся (потому они и шаблоны, а не статика).
        // Поэтому, с пустой картой нет смысла продолжать обработку запроса.
        if (result.isEmpty) {
          None
        } else {
          Some(Right(result))
        }
      }

      /** Сериализация карты цветов в кусок query string. */
      override def unbind(key: String, value: BSvgColorMap): String = {
        value
          .iterator
          // Пустоцветы отсеять:
          .filter { case (_, color) => color != null && !color.isEmpty }
          // Сортировать qs-парамеры по алфавиту, чтобы ещё улучшить кеширование на клиенте
          .toSeq
          .sortBy(_._1)
          // Сериализовать в qs:
          .iterator
          .map { case (colorName, color) => strB.unbind(key + "." + colorName.toString, color) }
          .mkString("&")
      }
    }
  }

}
