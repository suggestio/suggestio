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
    def lkNodeEditCall(adnId: String): Call
    def sysShow(adnId: String): Call
    def inviteSubNodeCall(adnId: String): Call
    def slaveNodeEditCall(adnId: String): Option[Call]
    def sysEditCall(adnId: String): Option[Call]
  }

  type AdnShowType = Val

  implicit def value2val(x: Value) = x.asInstanceOf[AdnShowType]
  implicit def anmt2showType(x: AdNetMemberType): AdnShowType = withName(x.name)

  val SHOP = new Val(AdNetMemberTypes.SHOP) {
    override def lkNodeEditCall(adnId: String) = routes.MarketShopLk.editShopForm(adnId)
    override def sysShow(adnId: String): Call = routes.SysMarket.shopShow(adnId)
    override def inviteSubNodeCall(adnId: String): Call = throw new UnsupportedOperationException("Shops cannot invite sub-shops.")
    override def slaveNodeEditCall(adnId: String): Option[Call] = Some(routes.MarketMartLk.editShopForm(adnId))
    override def sysEditCall(adnId: String): Option[Call] = Some(routes.SysMarket.shopEditForm(adnId))
  }

  val MART = new Val(AdNetMemberTypes.MART) {
    override def lkNodeEditCall(adnId: String) = routes.MarketMartLk.martEditForm(adnId)
    override def sysShow(adnId: String): Call = routes.SysMarket.martShow(adnId)
    override def inviteSubNodeCall(adnId: String): Call = routes.MarketMartLk.inviteShopForm(adnId)
    override def slaveNodeEditCall(adnId: String): Option[Call] = None
    override def sysEditCall(adnId: String): Option[Call] = Some(routes.SysMarket.martEditForm(adnId))
  }

}
