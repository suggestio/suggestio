package util.adn

import io.suggest.ym.model.common.{AdnMemberShowLevels, AdnRights, AdNetMemberTypes, AdNetMemberInfo}
import models._

import scala.concurrent.Future
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 9:53
 * Description: Утиль для работы с нодами. Появилось, когда понадобилась функция создания пользовательского узла
 * в нескольких контроллерах.
 */
object NodesUtil {

  /** */
  val SL_START_PAGE_LIMIT_DFLT = configuration.getInt("adn.node.user.sl.statpage.limit.dflt") getOrElse 50

  /**
   * Создать новый инстанс узла для юзера без сохранения узла в хранилище.
   * @param name название узла.
   * @param personId id юзера-владельца.
   * @return Экземпляр узла без id.
   */
  def userNodeInstance(name: String, personId: String): MAdnNode = {
    MAdnNode(
      adn = AdNetMemberInfo(
        memberType      = AdNetMemberTypes.SHOP,
        rights          = Set(AdnRights.PRODUCER, AdnRights.RECEIVER),
        isUser          = true,
        shownTypeIdOpt  = Some(AdnShownTypes.SHOP.name),
        isEnabled       = true,
        testNode        = false,
        sinks           = Set(AdnSinks.SINK_GEO),
        showLevelsInfo  = AdnMemberShowLevels(
          out = Map(AdShowLevels.LVL_START_PAGE -> SL_START_PAGE_LIMIT_DFLT)
        )
      ),
      personIds = Set(personId),
      meta = AdnMMetadata(name = name)
    )
  }

  /**
   * Создание узла для юзера
   * @param name Название узла.
   * @param personId id юзера-владельца.
   * @return Фьючерс с готовым инстансом нового существующего узла.
   */
  def createUserNode(name: String, personId: String): Future[MAdnNode] = {
    val inst = userNodeInstance(name = name, personId = personId)
    inst.save
      .map { adnId => inst.copy(id = Some(adnId)) }
  }

}
