package controllers

import play.api.mvc.{Result, Call}
import models._
import util.acl._
import scala.concurrent.Future
import views.html.market._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.PlayMacroLogsImpl
import controllers.Ident.EmailPwLoginForm_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.03.14 10:33
 * Description: Система личных кабинетов sio-market. Тут экшены и прочее API, которое не поместилось в конкретные ЛК.
 */
object MarketLk extends SioController with EmailPwSubmit with PlayMacroLogsImpl {

  /** Список личных кабинетов юзера. */
  def lkList = IsAuth.async { implicit request =>
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

  /** Юзер заходит в /market (или на market.suggest.io). Он видит страницу с описанием и кнопку для логина.
    * Если юзер уже залогинен и у него есть магазины/тц, то его надо переправить в ЛК. */
  def lkIndex = MaybeAuth.async { implicit request =>
    request.pwOpt match {
      case Some(pw) =>
        getMarketRdrCallFor(pw.personId) map {
          case Some(call) => Redirect(call)
          // Юзер залогинен, но попал в маркет, где у него нет прав. Отобразить обычную форму.
          case None => renderDfltPage
        }

      case None => renderDfltPage
    }
  }

  /** Рендер дефолтовой страницы. */
  private def renderDfltPage(implicit ctx: util.Context) = Ok(indexTpl(Some(Ident.emailPwLoginFormM)))


  /** При логине юзера по email-pw мы определяем его присутствие в маркете, и редиректим в ЛК магазина или в ЛК ТЦ. */
  def getMarketRdrCallFor(personId: String): Future[Option[Call]] = {
    // Нам тут не надо выводить элементы, нужно лишь определять кол-во личных кабинетов и данные по ним.
    MAdnNode.findByPersonId(personId, maxResults = 2).map { adnNodes =>
      val rdrOrNull: Call = if (adnNodes.isEmpty) {
        // У юзера нет ни магазинов, ни ТЦ во владении. Некуда его редиректить, вероятно ошибся адресом.
        null
      } else if (adnNodes.size == 1) {
        // У юзера есть магазин или ТЦ
        val adnNode = adnNodes.head
        routes.MarketLkAdn.showAdnNode(adnNode.id.get)
      } else {
        // У юзера есть более одно объекта во владении. Нужно предоставить ему выбор.
        routes.MarketLk.lkList()
      }
      Option(rdrOrNull)
    }
  }

  override def emailSubmitError(lf: EmailPwLoginForm_t)(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    Forbidden(indexTpl(Some(lf)))
  }
}
