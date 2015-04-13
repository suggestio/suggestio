package models.mext.tw.card

import models.Context
import models.mext.MExtServices
import play.twirl.api.Html

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.15 18:14
 * Description: Интерфейсы контейнеров и контейнеры для аргументов рендера различных twitter card.
 * @see [[https://dev.twitter.com/cards/overview]]
 */

trait ICardArgsBase {
  type W
}

trait ICardArgs extends ICardArgsBase {
  override type W <: ICardArgs

  /** Тип карточки. Заполняется промежуточными реализациями. */
  def cardType: CardType // { type Args_t = W }

  /** canonical url страницы. */
  def url: Option[String]

  /** заголовок, отображаемый например в img.alt. */
  def title: Option[String]

  /** Юзернейм приложения на твиттере. По идее он всегда одинаковый. */
  def site: Option[String] = MExtServices.TWITTER.myUserName

  /** Рендер шаблона. */
  def render()(implicit ctx: Context): Html
  // TODO Нужно как-то зафиксировать тип cardType.
  //def render()(implicit ctx: Context): Html = cardType.template.render(this, ctx)
}


/** Интерфейс generic-враппера. */
trait ICardArgsWrapper extends ICardArgsBase {

  /** Заврапанные данные. */
  def _cardArgsUnderlying: W
}


/** Враппер для [[ICardArgs]]. */
trait CardArgsWrapper extends ICardArgs with ICardArgsWrapper {
  override def cardType = _cardArgsUnderlying.cardType
  override def url      = _cardArgsUnderlying.url
  override def title    = _cardArgsUnderlying.title
  override def site     = _cardArgsUnderlying.site
}


trait CardArgsDflt extends ICardArgs {
  override def url    : Option[String] = None
  override def title  : Option[String] = None
}
