package controllers.adn

import controllers.routes
import play.api.mvc.Call
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.04.14 16:11
 * Description: Трейты для контроллеров, чтобы те могли легко отображать страницы главную
 * страницу личного кабинета указанного объекта.
 */


/** Расширение функционала AdNetMemberTypes для функций доступа к дополнительным данным в рамках.
  * Пришло на смену ShowAdnNodeCtx, жившему на уровне реализаций контроллеров. */
object AdnShowTypes extends Enumeration {
  protected case class Val(amt: AdNetMemberType) extends super.Val(amt.name) {
    /** Call для доступа к поисковой выдаче для админа ЛК. */
    def nodeAdmSiteCall(adnNode: MAdnNode): Option[Call] = {
      Some( routes.MarketShowcase.demoWebSite(adnNode.id.get) )
    }
  }

  type AdnShowType = Val

  implicit def value2val(x: Value): AdnShowType = x.asInstanceOf[AdnShowType]
  implicit def anmt2showType(x: AdNetMemberType): AdnShowType = withName(x.name)


  val SHOP: AdnShowType = new Val(AdNetMemberTypes.SHOP) {
    override def nodeAdmSiteCall(adnNode: MAdnNode): Option[Call] = {
      val call = routes.MarketShowcase.myAdsSite(adnNode.id.get)
      Some(call)
    }
  }

  val MART: AdnShowType = Val(AdNetMemberTypes.MART)


  val RESTAURANT: AdnShowType = Val(AdNetMemberTypes.RESTAURANT)

  val RESTAURANT_SUP: AdnShowType = new Val(AdNetMemberTypes.RESTAURANT_SUP) {
    override def nodeAdmSiteCall(adnNode: MAdnNode) = None
  }

}
