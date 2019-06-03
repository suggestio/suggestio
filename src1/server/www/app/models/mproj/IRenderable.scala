package models.mproj

import models.mctx.Context
import play.twirl.api.Html

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.06.19 11:52
  * Интерфейс для возможности задания моделей, умеющих рендер в html.
  */
trait IRenderable {

  /** Запуск рендера в контексте рендера шаблонов. */
  def render()(implicit ctx: Context): Html

}
