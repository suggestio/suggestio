package util.adn

import controllers.routes
import io.suggest.ym.model.ad.AdsSearchArgsDflt
import io.suggest.ym.model.common.{NodeConf, AdnMemberShowLevels}
import models._
import models.adv.MExtServices
import models.madn.NodeDfltColors
import org.joda.time.DateTime
import play.api.db.DB
import play.api.i18n.Messages
import play.api.mvc.Call
import util.PlayMacroLogsImpl
import util.async.AsyncUtil

import scala.concurrent.Future
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.Play.{current, configuration}

import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 9:53
 * Description: Утиль для работы с нодами. Появилось, когда понадобилась функция создания пользовательского узла
 * в нескольких контроллерах.
 * 2015.mar.18: Для новосозданного узла нужно создавать начальные рекламные карточки.
 */
object NodesUtil extends PlayMacroLogsImpl {

  import LOGGER._

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

  // Для новосозданного узла надо создавать новые карточки, испортируя их из указанного узла в указанном кол-ве.
  /** id узла, который содержит дефолтовые карточки. Задается явно в конфиге. */
  val ADN_IDS_INIT_ADS_SOURCE = configuration.getStringSeq("user.node.created.mads.import.from.adn_ids") getOrElse Nil

  /** Кол-во карточек для импорта из дефолтового узла. */
  val INIT_ADS_COUNT = configuration.getInt("user.node.created.mads.import.count") getOrElse 1


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
  def createExtDfltTargets(adnId: String)(implicit lang: Messages): Future[_] = {
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
  def createUserNode(name: String, personId: String)(implicit lang: Messages): Future[MAdnNode] = {
    val inst = userNodeInstance(name = name, personId = personId)
    val nodeSaveFut = inst.save
    nodeSaveFut flatMap { adnId =>
      val billSaveFut = createUserNodeBilling(adnId)
      val madsCreateFut = installDfltMads(adnId)
      for {
        _ <- createExtDfltTargets(adnId)
        _ <- billSaveFut
        _ <- madsCreateFut
      } yield {
        inst.copy(id = Some(adnId))
      }
    }
  }


  /** Установить дефолтовые карточки. */
  def installDfltMads(adnId: String, count: Int = INIT_ADS_COUNT)(implicit lang: Messages): Future[Seq[String]] = {
    lazy val logPrefix = s"installDfltMads($adnId):"
    (Future successful ADN_IDS_INIT_ADS_SOURCE)
      // Если нет продьюсеров, значит функция отключена. Это будет перехвачено в recover()
      .filter { _.nonEmpty }
      // Собрать id карточек, относящиеся к заданным узлам-источникам.
      .flatMap { prodIds =>
        val dsa0 = new AdsSearchArgsDflt {
          override def producerIds = prodIds
          override val maxResults = Math.max(50, count * 2)
          override def offset = 0
        }
        MAd.dynSearchIds(dsa0)
      }
      // Случайно выбрать из списка id карточек только указанное кол-во карточек.
      .flatMap { madIds =>
        val count = madIds.size
        val rnd = new Random()
        val madIds2 = (0 until Math.min(count, count))
          .iterator
          .map { _ =>  madIds( rnd.nextInt(count) ) }
        MAd.multiGet(madIds2)
      }
      // Обновить карточки
      .flatMap { mads0 =>
        trace(s"$logPrefix Will install ${mads0.size} ads: [${mads0.iterator.flatMap(_.id).mkString(", ")}]")
        Future.traverse(mads0) { mad0 =>
          // Создать новую карточку на базе текущей.
          val mad1 = mad0.copy(
            id          = None,
            versionOpt  = None,
            dateCreated = DateTime.now(),
            dateEdited  = None,
            producerId  = adnId,
            alienRsc    = true,
            receivers   = Map(
              adnId -> AdReceiverInfo(adnId, Set(SinkShowLevels.GEO_START_PAGE_SL))
            ),
            // Нужно локализовать текстовые поля с использование lang.
            offers = mad0.offers.map { offer =>
              offer.copy(
                text1 = offer.text1.map { aosf =>
                  aosf.copy(
                    value = Messages(aosf.value)
                  )
                }
              )
            }
            // TODO Нужно проверить содержимое поля text4search.
          )
          mad1.save
        }
      }
      // Если не было adnId узлов-источников, то
      .recover { case ex: NoSuchElementException =>
        LOGGER.warn("Node default ads installer is disabled!")
        Nil
      }
  }

}
