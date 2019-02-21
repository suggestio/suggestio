package models.ai

import enumeratum.values.{StringEnum, StringEnumEntry}
import util.ai.mad.render.{MadAiRenderedT, ScalaStiRenderer}

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 20:02
 * Description: Модель доступных рендереров динамических рекламных карточек.
 */

object MAiRenderers extends StringEnum[MAiRenderer] {

  /** Вызов рендера шаблонов scalasti. */
  case object ScalaSti extends MAiRenderer("scalasti") {
    override def getRendererClass = ClassTag( classOf[ScalaStiRenderer] )
  }

  override def values = findValues

}


sealed abstract class MAiRenderer(override val value: String) extends StringEnumEntry {
  def getRendererClass: ClassTag[_ <: MadAiRenderedT]
}
