package controllers

import util.PlayMacroLogsImpl
import util.acl.IsSuperuser

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.14 18:19
 * Description: Вместо похожих MarketMartLk и MarketShopLk используется этот контроллер.
 * Он реализует личный кабинет абстрактного узла рекламной сети.
 */
object MarketLkAdnNode extends SioController with PlayMacroLogsImpl with LogoSupport {

  /** Отображение страницы личного кабинета узла рекламной сети. */
  def showAdnNode(adnId: String) = IsSuperuser { implicit request =>
    ???
  }

  /** Список подчиненных рекламодателей (раньше был список арендаторов). */
  def producersShow(adnId: String) = IsSuperuser { implicit request =>
    ???
  }

  /** Рендер формы редактирования узла рекламной сети. */
  def nodeEditForm(adnId: String) = IsSuperuser { implicit request =>
    ???
  }

}
