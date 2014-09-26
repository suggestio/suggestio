package models

import play.api.mvc.PathBindable
import play.twirl.api.{Template2, HtmlFormat}
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
    template: Template2[String, Map[String, String], HtmlFormat.Appendable]
  ) extends super.Val(name) {
    def render(fillColor: String, args: Map[String, String]) = template.render(fillColor, args)
  }

  type BlockSvg = Val

  // Перечисление шаблонов и зацепок к ним.
  val bg19 = Val("bg19", _bg19Tpl)


  implicit def value2val(x: Value): BlockSvg = x.asInstanceOf[BlockSvg]

  def maybeWithName(x: String): Option[BlockSvg] = {
    values
      .find(_.name == x)
      .asInstanceOf[Option[BlockSvg]]
  }

  implicit def pathBindable(implicit strB: PathBindable[String]) = {
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
