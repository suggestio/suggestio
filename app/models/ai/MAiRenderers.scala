package models.ai

import io.suggest.model.EnumMaybeWithName
import util.ai.mad.render.{MadAiRenderedT, ScalaStiRenderer}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 20:02
 * Description: Модель доступных рендереров динамических рекламных карточек.
 */

object MAiRenderers extends EnumMaybeWithName {
  /** Экземпляр модели. */
  protected sealed abstract class Val(val name: String) extends super.Val(name) {
    def getRenderer(): MadAiRenderedT
  }

  type MAiRenderer = Val
  override type T = MAiRenderer

  /** Вызов рендера шаблонов scalasti. */
  val ScalaSti: MAiRenderer = new Val("scalasti") {
    override def getRenderer() = ScalaStiRenderer
  }

}
