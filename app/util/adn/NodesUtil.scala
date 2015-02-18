package util.adn

import controllers.routes
import io.suggest.ym.model.common.{NodeConf, AdnMemberShowLevels}
import models._
import models.adv.{MExtServices, MExtTarget}
import models.madn.NodeDfltColors
import play.api.db.DB
import play.api.i18n.Lang
import play.api.mvc.Call
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
  val SL_START_PAGE_LIMIT_DFLT: Int = configuration.getInt("user.node.adn.sl.out.statpage.limit.dflt") getOrElse 50

  /** Стартовый баланс узла. */
  val BILL_START_BALLANCE: Float = {
    configuration.getDouble("user.node.bill.balance.dflt") match {
      case Some(d) => d.toFloat
      case None    => 0F
    }
  }

  /** Через сколько секунд отправлять юзера в ЛК ноды после завершения реги юзера. */
  val NODE_CREATED_SUCCESS_RDR_AFTER: Int = configuration.getInt("user.node.created.success.redirect.after.sec") getOrElse 5

  /** Куда отправлять юзера, когда тот создал новый узел? */
  def userNodeCreatedRedirect(adnId: String): Call = {
    routes.MarketLkAdnEdit.editAdnNode(adnId)
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
      meta = {
        val dc = NodeDfltColors.getOneRandom()
        AdnMMetadata(
          name      = name,
          color     = Some(dc.bgColor),
          fgColor   = Some(dc.fgColor)
        )
      },
      conf = NodeConf(
        showInScNodesList = false
      )
    )
  }

  /**
   * Инициализировать биллинг для пользовательского узла.
   * @param adnId id узла.
   * @return Фьючерс для синхронизации.
   */
  def createUserNodeBilling(adnId: String): Future[_] = {
    Future {
      DB.withTransaction { implicit ctx =>
        MBillContract(adnId = adnId).save
        MBillBalance(adnId = adnId, amount = BILL_START_BALLANCE).save
        // 2015.feb.18: Не надо создавать новый пользовательский узел как платный ресивер.
        //val mmp0 = MBillMmpDaily(contractId = mbc.id.get).save
        // Можно возвращать все эти экземпляры в результате работы. Сейчас это не требуется, поэтому они висят так.
      }
    }(AsyncUtil.jdbcExecutionContext)
  }

  /**
   * Создать дефолтовые таргеты для размещения в соц.сетях.
   * @param adnId id узла.
   * @return Фьючерс для синхронизации.
   */
  def createExtDfltTargets(adnId: String): Future[_] = {
    val tgtsIter = MExtServices.values
      .iterator
      .flatMap { _.dfltTarget(adnId) }
    Future.traverse(tgtsIter)(_.save)
  }

  /**
   * Создание нового узла для юзера. Узел должен быть готов к финансовой работе.
   * @param name Название узла.
   * @param personId id юзера-владельца.
   * @return Фьючерс с готовым инстансом нового существующего узла.
   */
  def createUserNode(name: String, personId: String)(implicit lang: Lang): Future[MAdnNode] = {
    val inst = userNodeInstance(name = name, personId = personId)
    val nodeSaveFut = inst.save
    nodeSaveFut flatMap { adnId =>
      val billSaveFut = createUserNodeBilling(adnId)
      for {
        _ <- createExtDfltTargets(adnId)
        _ <- billSaveFut
      } yield {
        inst.copy(id = Some(adnId))
      }
    }
  }

}
