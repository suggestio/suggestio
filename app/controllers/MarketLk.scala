package controllers

import play.api.mvc.Call
import models._
import util.acl._
import scala.concurrent.Future
import views.html.market.lk
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.03.14 10:33
 * Description: Система личных кабинетов sio-market. Тут экшены и прочее API, которое не поместилось в конкретные ЛК.
 */
object MarketLk extends SioController {

  /** Список личных кабинетов юзера. */
  def lkList = IsAuth.async { implicit request =>
    val personId = request.pwOpt.get.personId
    martsAndShopsFut(personId) map { case (mmarts, mshops) =>
      Ok(lk.lkList(mmarts, mshops))
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
  private def renderDfltPage(implicit ctx: util.Context) = Ok(lk.lkIndexTpl(Ident.emailPwLoginFormM))


  /** При логине юзера по email-pw мы определяем его присутствие в маркете, и редиректим в ЛК магазина или в ЛК ТЦ. */
  def getMarketRdrCallFor(personId: String): Future[Option[Call]] = {
    martsAndShopsFut(personId) map { case (mmarts, mshops) =>
      // Не используем .tail.isEmpty вместо .size, т.к. тут не List, а обычно что-то иное, и .tail скорее всего приведет к созданию новой коллекции.
      // Внутри функции разрешаем null, чтобы избежать повторяющихся Some().
      val rdrOrNull: Call = if (mshops.isEmpty && mmarts.isEmpty) {
        // У юзера нет ни магазинов, ни ТЦ во владении. Некуда его редиректить, вероятно ошибся адресом.
        null
      } else if (mmarts.isEmpty && mshops.size == 1) {
        // У юзера есть один магазин во владении
        routes.MarketShopLk.showShop(mshops.head.id.get)
      } else if (mshops.isEmpty && mmarts.size == 1) {
        // У юзера есть только один ТЦ во владении
        routes.MarketMartLk.martShow(mmarts.head.id.get)
      } else {
        // У юзера есть несколько market-сущностей в персональном пользовании. Нужно отправить его на страницу со списком этого всего.
        routes.MarketLk.lkList()
      }
      Option(rdrOrNull)
    }
  }

  private def martsAndShopsFut(personId: String): Future[(Seq[MMart], Seq[MShop])] = {
    val mshopsFut = MShop.findByPersonId(personId)
    val mmartsFut = MMart.findByPersonId(personId)
    for {
      mshops <- mshopsFut
      mmarts <- mmartsFut
    } yield {
      (mmarts, mshops)
    }
  }

}
