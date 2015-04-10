package models.mext.tw

import models.Context
import play.twirl.api.{Html, Template2}
import views.html.ext.tw._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.15 18:26
 * Description: Поддерживаемые типы twitter-карточек.
 */
object CardTypes extends Enumeration {

  /** Экземпляр модели. */
  abstract sealed protected class Val(name: String) extends super.Val(name) {
    type Args_t <: ICardArgsBase { type W = Args_t }
    def template: Template2[Args_t, Context, Html]
  }

  type T = Val

  val Photo = new Val("photo") {
    override type Args_t = IPhotoCardArgs
    override def template = _metaTwitterPhotoCardTpl
  }

}
