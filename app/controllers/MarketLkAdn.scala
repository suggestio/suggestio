package controllers

import util.PlayMacroLogsImpl
import controllers.adn.AdnShowLk
import util.acl.IsAdnNodeAdmin
import models.MAdnNodeCache
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 11:18
 * Description: Унифицированные части личного кабинета.
 */
object MarketLkAdn extends SioController with PlayMacroLogsImpl with AdnShowLk {

  /**
   * Отрендерить страницу ЛК абстрактного узла рекламной сети.
   * @param adnId id узла.
   * @param newAdIdOpt Костыль: если была добавлена рекламная карточка, то она должна отобразится сразу,
   *                   независимо от refresh в индексе. Тут её id.
   */
  def showAdnNode(adnId: String, newAdIdOpt: Option[String]) = IsAdnNodeAdmin(adnId).async { implicit request =>
    import request.adnNode
    val fallbackLogoFut = adnNode.adn.supId match{
      case Some(supId) =>
        MAdnNodeCache.getByIdCached(supId) map {
          _.flatMap(_.logoImgOpt)
        }

      case None => Future successful None
    }
    renderShowAdnNode(adnNode, newAdIdOpt, fallbackLogoFut)
  }

}
