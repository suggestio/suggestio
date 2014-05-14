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
  protected abstract case class Val(amt: AdNetMemberType) extends super.Val(amt.name) {
    /** Ссылка на создание инвайта для под-узла, если есть возможность. */
    def inviteSubNodeCall(adnId: String): Option[Call]

    /** Ссылка на редактирование подчинённого узла, если есть возможность. */
    def slaveNodeEditCall(adnId: String): Option[Call]

    /** Id для синтеза ссылки на demo web site, если есть возможность. */
    def demoWebsiteAdnId(adnNode: MAdnNode): Option[String] = adnNode.id
  }

  type AdnShowType = Val

  implicit def value2val(x: Value): AdnShowType = x.asInstanceOf[AdnShowType]
  implicit def anmt2showType(x: AdNetMemberType): AdnShowType = withName(x.name)


  val SHOP = new Val(AdNetMemberTypes.SHOP) {
    override def inviteSubNodeCall(adnId: String) = None
    override def slaveNodeEditCall(adnId: String) = Some(routes.MarketMartLk.editShopForm(adnId))
    /** Для магазина надо отображать демо всего ТЦ, который обычно является супервизором. */
    override def demoWebsiteAdnId(adnNode: MAdnNode) = {
      adnNode.adn.supId
        .orElse(super.demoWebsiteAdnId(adnNode))
    }
  }

  val MART = new Val(AdNetMemberTypes.MART) {
    override def inviteSubNodeCall(adnId: String) = Some(routes.MarketMartLk.inviteShopForm(adnId))
    override def slaveNodeEditCall(adnId: String) = None
  }


  val RESTAURANT = new Val(AdNetMemberTypes.RESTAURANT) {
    override def inviteSubNodeCall(adnId: String) = None
    override def slaveNodeEditCall(adnId: String) = Some(routes.MarketMartLk.editShopForm(adnId))
  }

  val RESTAURANT_SUP = new Val(AdNetMemberTypes.RESTAURANT_SUP) {
    override def inviteSubNodeCall(adnId: String) = None // TODO Надо?
    override def slaveNodeEditCall(adnId: String) = None
  }

}
