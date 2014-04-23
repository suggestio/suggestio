package controllers.adn

import scala.concurrent.Future
import controllers.{routes, SioController}
import play.api.mvc.{Call, AnyContent, Result}
import util.acl.AbstractRequestWithPwOpt
import models._
import views.html.market.lk.adn._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.04.14 16:11
 * Description: Трейты для контроллеров, чтобы те могли легко отображать страницы главную
 * страницу личного кабинета указанного объекта.
 */
trait AdnShowLk extends SioController {

  def renderShowAdnNode(node: MAdnNode, newAdIdOpt: Option[String], fallbackLogoFut: Future[Option[MImgInfo]] = Future successful None)
                       (implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[Result] = {
    val adnId = node.id.get
    val adsFut = MAd.findForProducerRt(adnId)
    // TODO Если магазин удалён из ТЦ, то это как должно выражаться?
    // Бывает, что добавлена новая карточка. Нужно её как-то отобразить.
    val extAdOptFut = newAdIdOpt match {
      case Some(newAdId) => MAd.getById(newAdId)
        .map { _.filter { mad =>
        mad.producerId == adnId ||  mad.receivers.valuesIterator.exists(_.receiverId == adnId)
      } }
      case None => Future successful None
    }
    for {
      mads      <- adsFut
      extAdOpt  <- extAdOptFut
      fallbackLogo <- fallbackLogoFut
    } yield {
      // Если есть карточка в extAdOpt, то надо добавить её в начало списка, который отсортирован по дате создания.
      val mads2 = if (extAdOpt.isDefined  &&  mads.headOption.flatMap(_.id) != newAdIdOpt) {
        extAdOpt.get :: mads
      } else {
        mads
      }
      Ok(adnNodeShowTpl(node, mads2, fallbackLogo = fallbackLogo))
    }
  }

}


/** Расширение функционала AdNetMemberTypes для функций доступа к дополнительным данным в рамках.
  * Пришло на смену ShowAdnNodeCtx, жившему на уровне реализаций контроллеров. */
object AdnShowTypes extends Enumeration {
  protected abstract case class Val(amt: AdNetMemberType) extends super.Val(amt.name) {
    def lkNodeEditCall(adnId: String): Call
    def sysShow(adnId: String): Call
    def inviteSubNodeCall(adnId: String): Call
  }

  type AdnShowType = Val

  implicit def value2val(x: Value) = x.asInstanceOf[AdnShowType]
  implicit def anmt2showType(x: AdNetMemberType): AdnShowType = withName(x.name)

  val SHOP = new Val(AdNetMemberTypes.SHOP) {
    override def lkNodeEditCall(adnId: String) = routes.MarketShopLk.editShopForm(adnId)
    override def sysShow(adnId: String): Call = routes.SysMarket.shopShow(adnId)
    override def inviteSubNodeCall(adnId: String): Call = throw new UnsupportedOperationException("Shops cannot invite sub-shops.")
  }

  val MART = new Val(AdNetMemberTypes.MART) {
    override def lkNodeEditCall(adnId: String) = routes.MarketMartLk.martEditForm(adnId)
    override def sysShow(adnId: String): Call = routes.SysMarket.martShow(adnId)
    override def inviteSubNodeCall(adnId: String): Call = routes.MarketMartLk.inviteShopForm(adnId)
  }

}
