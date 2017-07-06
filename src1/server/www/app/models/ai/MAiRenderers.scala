package models.ai

import io.suggest.common.menum.EnumMaybeWithName
import util.ai.mad.render.{MadAiRenderedT, ScalaStiRenderer}

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 20:02
 * Description: Модель доступных рендереров динамических рекламных карточек.
 */

object MAiRenderers extends EnumMaybeWithName {

  /** Экземпляр модели. */
  protected sealed abstract class Val(val name: String) extends super.Val(name) {
    def getRendererClass: ClassTag[_ <: MadAiRenderedT]
  }

  type MAiRenderer = Val
  override type T = MAiRenderer

  /** Вызов рендера шаблонов scalasti. */
  val ScalaSti: MAiRenderer = new Val("scalasti") {
    override def getRendererClass = ClassTag( classOf[ScalaStiRenderer] )
  }

}
