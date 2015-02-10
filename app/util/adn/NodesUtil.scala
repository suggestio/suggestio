package util.adn

import io.suggest.ym.model.common.{AdnRights, AdNetMemberTypes, AdNetMemberInfo}
import models.{AdnShownTypes, MAdnNode}

import scala.concurrent.Future
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 9:53
 * Description: Утиль для работы с нодами. Появилось, когда понадобилась функция создания пользовательского узла
 * в нескольких контроллерах.
 */
object NodesUtil {


  def createUserNode(name: String): Future[MAdnNode] = {
    // Прежде чем создавать узел, нужно найти/создать компанию, относящуюся к узлу и юзеру.
    val adn0 = MAdnNode(
      companyId = None,
      adn = AdNetMemberInfo(
        memberType      = AdNetMemberTypes.SHOP,
        rights          = Set(AdnRights.PRODUCER, AdnRights.RECEIVER),
        isUser          = true,
        shownTypeIdOpt  = Some(AdnShownTypes.SHOP.name)
      )
    )

    ???
  }

}
