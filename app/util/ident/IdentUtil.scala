package util.ident

import controllers.{routes, MarketLk}
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 15:45
 * Description: Утиль для ident-контроллера и других вещей, связанных с идентификацией пользователей.
 */
object IdentUtil {

  def redirectCallUserSomewhere(personId: String): Future[Call] = {
    MarketLk.getMarketRdrCallFor(personId) map {
      _ getOrElse routes.Market.index() // Был раньше Admin.index(), но кравлер пока выпилен ведь.
    }
  }

  /** Сгенерить редирект куда-нибудь для указанного юзера. */
  def redirectUserSomewhere(personId: String): Future[Result] = {
    redirectCallUserSomewhere(personId) map { Results.Redirect }
  }

  // TODO Нужен метод "инсталляции" нового юзера.
}
