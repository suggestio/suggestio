package util.ident

import controllers.routes
import io.suggest.ym.model.MAdnNode
import models.usr.{MPerson, MExtIdent}
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

  /** При логине юзера по email-pw мы определяем его присутствие в маркете, и редиректим в ЛК магазина или в ЛК ТЦ. */
  def getMarketRdrCallFor(personId: String): Future[Option[Call]] = {
    // Нам тут не надо выводить элементы, нужно лишь определять кол-во личных кабинетов и данные по ним.
    MAdnNode.findByPersonId(personId, maxResults = 2).map { adnNodes =>
      val rdrOrNull: Call = if (adnNodes.isEmpty) {
        // У юзера нет рекламных узлов во владении. Некуда его редиректить, вероятно ошибся адресом.
        null
      } else if (adnNodes.size == 1) {
        // У юзера есть один рекламный узел
        val adnNode = adnNodes.head
        routes.MarketLkAdn.showNodeAds(adnNode.id.get)
      } else {
        // У юзера есть несколько узлов во владении. Нужно предоставить ему выбор.
        routes.MarketLkAdn.lkList()
      }
      Option(rdrOrNull)
        // Если некуда отправлять, а юзер - админ, то отправить в /sys/.
        .orElse {
          if (MPerson.isSuperuserId(personId)) {
            Some(routes.Application.sysIndex())
          } else {
            None
          }
        }
    }
  }


  def redirectCallUserSomewhere(personId: String): Future[Call] = {
    getMarketRdrCallFor(personId) flatMap {
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

          // Нет узлов, залогинился через emailPW, отправить в lkList, там есть кнопка добавления узла.
          case _ =>
            routes.MarketLkAdn.lkList()
        }
    }
  }

  /** Сгенерить редирект куда-нибудь для указанного юзера. */
  def redirectUserSomewhere(personId: String): Future[Result] = {
    redirectCallUserSomewhere(personId) map { Results.Redirect }
  }

  // TODO Нужен метод "инсталляции" нового юзера.
}
