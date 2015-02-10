package util.adn

import io.suggest.ym.model.common.{AdnMemberShowLevels, AdnRights, AdNetMemberTypes, AdNetMemberInfo}
import models._
import play.api.db.DB
import util.async.AsyncUtil

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

  /** Дефолтовый лимит на размещение у самого себя на главной. */
  val SL_START_PAGE_LIMIT_DFLT: Int = configuration.getInt("user.node.sl.statpage.limit.dflt") getOrElse 50

  /** Стартовый баланс узла. */
  val BILL_START_BALLANCE: Float = {
    configuration.getDouble("user.node.bill.balance.dflt") match {
      case Some(d) => d.toFloat
      case None    => 0F
    }
  }

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
        showLevelsInfo  = AdnMemberShowLevels(
          out = Map(AdShowLevels.LVL_START_PAGE -> SL_START_PAGE_LIMIT_DFLT)
        )
      ),
      personIds = Set(personId),
      meta = AdnMMetadata(name = name)
    )
  }

  def createUserNodeBilling(adnId: String): Future[_] = {
    Future {
      DB.withTransaction { implicit ctx =>
        val mbc = MBillContract(adnId = adnId).save
        val mbb = MBillBalance(adnId = adnId, amount = BILL_START_BALLANCE).save
        val mmp0 = MBillMmpDaily(
          contractId = mbc.id.get
        ).save
      }
    }(AsyncUtil.jdbcExecutionContext)
  }

  /**
   * Создание нового узла для юзера. Узел должен быть готов к финансовой работе.
   * @param name Название узла.
   * @param personId id юзера-владельца.
   * @return Фьючерс с готовым инстансом нового существующего узла.
   */
  def createUserNode(name: String, personId: String): Future[MAdnNode] = {
    val inst = userNodeInstance(name = name, personId = personId)
    val nodeSaveFut = inst.save
    val billSaveFut = nodeSaveFut flatMap { adnId =>
      createUserNodeBilling(adnId)
    }
    for {
      adnId <- nodeSaveFut
      _     <- billSaveFut
    } yield {
      inst.copy(id = Some(adnId))
    }
  }

}
