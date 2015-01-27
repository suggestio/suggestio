package controllers

import controllers.ident._
import play.api.mvc.{Result, Call}
import models._
import util.acl._
import scala.concurrent.Future
import views.html.market._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.03.14 10:33
 * Description: Система личных кабинетов sio-market. Тут экшены и прочее API, которое не поместилось в конкретные ЛК.
 */
object MarketLk extends SioController with EmailPwSubmit with PlayMacroLogsImpl {

  /** Список личных кабинетов юзера. */
  def lkList = IsAuthC(obeyReturnPath = false).async { implicit request =>
    val personId = request.pwOpt.get.personId
    val adnmsFut = MAdnNode.findByPersonId(personId)
    val allMartsMapFut = MAdnNode.getAll()
      .map { mmarts => mmarts.map {mmart => mmart.id.get -> mmart}.toMap }
    for {
      adnms <- adnmsFut
      allMartsMap <- allMartsMapFut
    } yield {
      val adnmsGrouped = adnms.groupBy(_.adn.memberType)
        .mapValues(_.sortBy(_.meta.name.toLowerCase))
      Ok(lk.lkList(adnmsGrouped, allMartsMap))
    }
  }

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
        routes.MarketLk.lkList()
      }
      Option(rdrOrNull)
        // Если некуда отправлять, а юзер - админ, то отправить в /sys/.
        .orElse {
          if (MPerson isSuperuserId personId) {
            Some(routes.Application.sysIndex())
          } else {
            None
          }
        }
    }
  }

  override def emailSubmitError(lf: EmailPwLoginForm_t, r: Option[String])(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    MarketIndexAccess.getNodes.map { nodes =>
      Forbidden(indexTpl(
        lf    = Some(lf),
        nodes = nodes
      ))
    }
  }
}
