package util.billing

import com.google.inject.Inject
import io.suggest.di.{IExecutionContext, IEsClient}
import io.suggest.event.SioNotifierStaticClientI
import models._
import models.adv.{MAdvOk, MAdv}
import org.elasticsearch.client.Client
import org.joda.time.{Period, DateTime}
import play.api.db.Database
import util.adv.AdvUtil
import util.PlayMacroLogsImpl
import util.di.{IDb, ISioNotifier}
import scala.concurrent.{ExecutionContext, Future}
import java.sql.Connection

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.10.15 22:01
 * Description: Файл содержит код периодической проверки и обработки очереди MAdv*.
 */

/**
 * Код вывода в выдачу и последующего сокрытия рекламных карточек крайне похож,
 * поэтому он вынесен в трейт.
 */
sealed abstract class AdvsUpdate
  extends PlayMacroLogsImpl
  with IExecutionContext
  with IEsClient
  with ISioNotifier
  with IDb
{

  import LOGGER._

  /** Экземпляр [[util.adv.AdvUtil]], вбрасываемый через DI. */
  def advUtil: AdvUtil

  def findAdvsOk(implicit c: Connection): List[MAdvOk]

  def nothingToDo() {}

  def updateAdvOk(advOk: MAdvOk, now: DateTime): MAdvOk

  def run() {
    val advs = db.withConnection { implicit c =>
      findAdvsOk
    }
    if (advs.nonEmpty) {
      trace(s"Where are ${advs.size} items. ids = ${advs.map(_.id.get).mkString(", ")}")
      val advsMap = advs.groupBy(_.adId)
      val now = DateTime.now
      advsMap foreach { case (adId, advsOk) =>
        val advsOk1 = advsOk
          .map { updateAdvOk(_, now) }
        db.withTransaction { implicit c =>
          advsOk1.foreach(_.save)
        }
        // Запустить пересчёт уровней отображения для затронутой рекламной карточки.
        val madUpdFut = MAd.getById(adId) flatMap {
          case Some(mad0) =>
            // Запускаем полный пересчет карты ресиверов.
            advUtil.calculateReceiversFor(mad0) flatMap { rcvrs1 =>
              // Новая карта ресиверов готова. Заливаем её в карточку и сохраняем последнюю.
              MAd.tryUpdate(mad0) { mad1 =>
                mad1.copy(
                  receivers = rcvrs1
                )
              }
            }

          // Карточка внезапно не найдена. Наверное она была удалена, а инфа в базе почему-то осталась. В любом случае, нужно удалить все adv.
          case None =>
            val totalDeleted = db.withConnection { implicit c =>
              MAdv.deleteByAdId(adId)
            }
            warn(s"Ad[$adId] not exists, but at least ${advsOk.size} related advs in processing. I've removed $totalDeleted orphan advs from all adv models!")
            Future successful adId
        }
        madUpdFut onFailure { case ex =>
          error(s"Failed to update ad[$adId] rcvrs. Rollbacking advOks update txn...", ex)
          try {
            db.withConnection { implicit c =>
              advs.foreach(_.save)
            }
            debug(s"Successfully recovered adv state back for ad[$adId]. Will retry update on next time.")
          } catch {
            case ex: Throwable =>
              error(s"Failed to rollback advOks update txn! Adv state inconsistent. Advs:\n  $advs", ex)
          }
        }
      }
    } else {
      nothingToDo()
    }
  }
}


/** Обновлялка adv sls, добавляющая уровни отображаения к существующей рекламе,
  * которая должна бы выйти в свет. */
class AdvertiseOfflineAdvs @Inject() (
  override val db                 : Database,
  override val advUtil            : AdvUtil,
  override implicit val ec        : ExecutionContext,
  override implicit val esClient  : Client,
  override implicit val sn        : SioNotifierStaticClientI
)
  extends AdvsUpdate
{

  override def findAdvsOk(implicit c: Connection): List[MAdvOk] = {
    MAdvOk.findAllOfflineOnTime
  }

  override def updateAdvOk(advOk: MAdvOk, now: DateTime): MAdvOk = {
    val dateDiff = new Period(advOk.dateStart, now)
    advOk.copy(
      dateStart = now,
      dateEnd = advOk.dateEnd plus dateDiff,
      isOnline = true
    )
  }
}


/** Обновлялка adv sls, которая снимает уровни отображения с имеющейся рекламы,
  * которая должна уйти из выдачи по истечению срока размещения. */
class DepublishExpiredAdvs @Inject() (
  override val db                 : Database,
  override val advUtil            : AdvUtil,
  override implicit val ec        : ExecutionContext,
  override implicit val esClient  : Client,
  override implicit val sn        : SioNotifierStaticClientI
)
  extends AdvsUpdate
{

  override def findAdvsOk(implicit c: Connection): List[MAdvOk] = {
    MAdvOk.findDateEndExpired()
  }

  override def updateAdvOk(advOk: MAdvOk, now: DateTime): MAdvOk = {
    advOk.copy(
      isOnline = false,
      dateEnd = now
    )
  }
}

