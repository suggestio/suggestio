package util.ident

import controllers.{routes, MarketLk}
import models.usr.MExtIdent
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 15:45
 * Description: Утиль для ident-контроллера и других вещей, связанных с идентификацией пользователей.
 */
object IdentUtil {

  def redirectCallUserSomewhere(personId: String): Future[Call] = {
    MarketLk.getMarketRdrCallFor(personId) flatMap {
      // Уже ясно куда редиректить юзера
      case Some(rdr) =>
        Future successful rdr

      // У юзера нет узлов
      case None =>
        // TODO Отправить на форму регистрации, если логин через внешнего id прова.
        val idpIdsCntFut = MExtIdent.countByPersonId(personId)
        idpIdsCntFut map {
          // Есть идентификации через соц.сети. Вероятно, юзер не закончил регистрацию.
          case n if n > 0L =>
            routes.Ident.idpConfirm()

          // Нет узлов, залогинился через emailPW, отправить в lkList
          case _ =>
            routes.MarketLk.lkList()
        }
    }
  }

  /** Сгенерить редирект куда-нибудь для указанного юзера. */
  def redirectUserSomewhere(personId: String): Future[Result] = {
    redirectCallUserSomewhere(personId) map { Results.Redirect }
  }

  // TODO Нужен метод "инсталляции" нового юзера.
}
