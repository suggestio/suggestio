package models.mext.tw.card

import enumeratum.values.{StringEnum, StringEnumEntry}
import models.mctx.Context
import play.twirl.api.{Html, Template2}
import views.html.ext.tw._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.15 18:26
 * Description: Поддерживаемые типы twitter-карточек.
 */
object CardTypes extends StringEnum[CardType] {

  case object Photo extends CardType("photo") {
    override type Args_t = IPhotoCardArgs
    override def template = _metaTwitterPhotoCardTpl
  }

  override def values = findValues

}

sealed abstract class CardType(override val value: String) extends StringEnumEntry {
  type Args_t <: ICardArgsBase { type W = Args_t }
  def template: Template2[Args_t, Context, Html]
}
