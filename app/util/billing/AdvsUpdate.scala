package util.billing

import java.sql.Connection

import com.google.inject.Inject
import models._
import models.adv.{MAdv, MAdvOk}
import models.mproj.{ICommonDi, IMCommonDi}
import org.joda.time.{DateTime, Period}
import util.PlayMacroLogsImpl
import util.adv.AdvUtil

import scala.concurrent.Future

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
  with IMCommonDi
{

  import LOGGER._
  import mCommonDi._

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
        val madFut = MNode.getById(adId)
        val advsOk1 = advsOk
          .map { updateAdvOk(_, now) }
        db.withTransaction { implicit c =>
          advsOk1.foreach(_.save)
        }
        // Запустить пересчёт уровней отображения для затронутой рекламной карточки.
        val madUpdFut = madFut flatMap {
          case Some(mad0) =>
            // Запускаем полный пересчет карты ресиверов.
            advUtil.calculateReceiversFor(mad0) flatMap { rcvrs1 =>
              // Новая карта ресиверов готова. Заливаем её в карточку и сохраняем последнюю.
              MNode.tryUpdate(mad0) { mad1 =>
                mad1.copy(
                  edges = mad1.edges.copy(
                    out = mad1.edges.out ++ rcvrs1
                  )
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

/** guice factory для быстрой сборки экземпляров [[AdvertiseOfflineAdvs]]. */
trait AdvertiseOfflineAdvsFactory {
  def create(): AdvertiseOfflineAdvs
}

/** Обновлялка adv sls, добавляющая уровни отображаения к существующей рекламе,
  * которая должна бы выйти в свет. */
class AdvertiseOfflineAdvs @Inject() (
  override val mCommonDi          : ICommonDi,
  override val advUtil            : AdvUtil
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

trait DepublishExpiredAdvsFactory {
  def create(): DepublishExpiredAdvs
}
/** Обновлялка adv sls, которая снимает уровни отображения с имеющейся рекламы,
  * которая должна уйти из выдачи по истечению срока размещения. */
class DepublishExpiredAdvs @Inject() (
  override val mCommonDi          : ICommonDi,
  override val advUtil            : AdvUtil
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

