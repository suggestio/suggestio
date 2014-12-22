package util.adv

import io.suggest.util.JMXBase
import io.suggest.ym.model.common.EMReceivers.Receivers_t
import models._
import org.joda.time.DateTime
import play.api.db.DB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.{AsyncUtil, PlayMacroLogsImpl}
import util.SiowebEsUtil.client
import util.billing.MmpDailyBilling
import util.event.SiowebNotifier.Implicts.sn
import play.api.Play.{current, configuration}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.14 18:19
 * Description: Утиль для работы с размещениями рекламных карточек.
 */
object AdvUtil extends PlayMacroLogsImpl {

  import LOGGER._

  /** Текущие активные аддоны, участвующие в генерации списка ресиверов. */
  val EXTRA_RCVRS_CALCS: List[AdvExtraRcvrsCalculator] = {
    List(
      AdvFreeGeoParentRcvrs,
      AdvTownCoverageRcvrs
    )
    .filter(_.isEnabled)
  }

  /** Причина hard-отказа в размещении со стороны suggest.io, а не узла.
    * Потом надо это заменить на нечто иное: чтобы суперюзер s.io вводил причину. */
  def SIOM_REFUSE_REASON = configuration.getString("sys.m.ad.hard.refuse.reason") getOrElse "Refused by suggest.io."

  
  info(s"Enabled extra-rcvrs generators: " + EXTRA_RCVRS_CALCS.iterator.map(_.getClass.getSimpleName).mkString(", "))

  /**
   * Пересчет текущих размещений рекламной карточки на основе данных других моделей.
   * Данные по саморазмещению мигрируют из исходных данных размещения.
   * @param mad Исходная рекламная карточка или её интерфейс.
   * @param producerOpt Экземпляр продьюсера
   */
  def calculateReceiversFor(mad: MAdT, producerOpt: Option[MAdnNode] = None): Future[Receivers_t] = {
    val priOpt = mad.receivers.get(mad.producerId)
    val needProducer = priOpt.isDefined
    // Нам нужен продьюсер для фильтрации копируемых sls продьюсера. Ищем его в фоне.
    val producerFut: Future[MAdnNode] = if (needProducer) {
      producerOpt
        // самоконтроль: резать переданного продьюсера, если он не является продьюсером данной карточки.
        .filter { _.id contains mad.producerId }
        .fold[Future[MAdnNode]]
          { MAdnNodeCache.getById(mad.producerId).map(_.get) }
          { Future.successful }
    } else {
      Future successful producerOpt.orNull
    }
    // Считаем непосредственных ресиверов через mmp-billing, т.е. платные размещения по времени.
    val receiversMmp = MmpDailyBilling.calcualteReceiversMapForAd(mad.id.get)

    // Чистим саморазмещение и добавляем в карту прямых ресиверов.
    val prodResultFut: Future[Receivers_t] = priOpt.fold(Future successful receiversMmp) { pri =>
      producerFut map { producer =>
        // Оставляем только уровни отображения, которые доступны ресиверу.
        val psls2 = pri.sls
          // Выкинуть sink-уровни продьюсера, которые не соответствуют доступным синкам и
          // уровням отображения, которые записаны в политике исходящих размещений
          .filter { ssl  =>  producer.adn.isReceiver  &&  producer.adn.hasSink(ssl.adnSink)  &&  producer.adn.canOutAtLevel(ssl.sl) }
        if (psls2.isEmpty) {
          // Удаляем саморесивер, т.к. уровни пусты.
          receiversMmp
        } else {
          // Добавляем собственный ресивер с обновлёнными уровнями отображениям.
          val prkv = pri.receiverId -> pri.copy(sls = psls2)
          receiversMmp + prkv
        }
      }
    }

    // На чищенную карту ресиверов запускаем поиск экстра-ресиверов на основе списка непосредственных.
    val resultFut: Future[Receivers_t] = prodResultFut
      .flatMap { prodResults =>
        // Запускаем сборку данных по доп.ресиверами. Получающиеся карты ресиверов объединяем асинхронно.
        val extrasFuts = EXTRA_RCVRS_CALCS.map { src =>
          src.calcForDirectRcvrs(prodResults, mad.producerId)
        }
        Future.reduce(prodResultFut :: extrasFuts) {
          (rm1, rm2) =>
            val result = AdReceiverInfo.mergeRcvrMaps(rm1, rm2)
            //trace(s"Merging rcvr maps:\n  $rm1\n  + $rm2\n  = $result")
            result
        }
      }

    resultFut
  }


  /**
   * Убрать указанную рекламную карточку из выдачи указанного ресивера или всех ресиверов.
   * @param adId id рекламной карточки.
   * @param rcvrIdOpt id ресивера. Если пусто, то все ресиверы вообще.
   * @return Boolean, который обычно не имеет смысла.
   */
  def removeAdRcvr(adId: String, rcvrIdOpt: Option[String]): Future[Boolean] = {
    lazy val logPrefix = s"removeAdRcvr(ad[$adId]${rcvrIdOpt.fold("")(", rcvr[" + _ + "]")}): "
    val madOptFut = MAd.getById(adId)
    // Радуемся в лог.
    rcvrIdOpt match {
      case Some(rcvrId) => info(logPrefix + "Starting removing for single rcvr...")
      case None => warn(logPrefix + "Starting removing ALL rcvrs...")
    }
    // Надо убрать указанного ресиверов из списка ресиверов
    val isOkFut = madOptFut flatMap {
      case Some(mad) =>
        MAd.tryUpdate(mad) { mad1 =>
          mad1.copy(
            receivers = if (rcvrIdOpt.isEmpty) {
              Map.empty
            } else {
              mad1.receivers.filterKeys(_ != rcvrIdOpt.get)
            }
          )
        }
          .map { _ => true}
      case None =>
        warn(logPrefix + "MAd not found: " + adId)
        Future successful false
    }
    // Надо убрать карточку из текущего размещения на узле, если есть: из advOk и из advReq.
    val dbUpdFut = Future {
      DB.withTransaction { implicit c =>
        // Резать как online, так и в очереди на публикацию.
        val sepo = SelectPolicies.UPDATE
        val advsOk = if (rcvrIdOpt.isDefined) {
          MAdvOk.findNotExpiredByAdIdAndRcvr(adId, rcvrId = rcvrIdOpt.get, policy = sepo)
        } else {
          MAdvOk.findNotExpiredByAdId(adId, policy = sepo)
        }
        advsOk
          .foreach { advOk =>
          info(s"${logPrefix}offlining advOk[${advOk.id.get}]")
          advOk.copy(dateEnd = DateTime.now, isOnline = false)
            .saveUpdate
        }
        // Запросы размещения переколбашивать в refused с возвратом бабла.
        val advsReq = if (rcvrIdOpt.isDefined) {
          MAdvReq.findByAdIdAndRcvr(adId, rcvrId = rcvrIdOpt.get, policy = sepo)
        } else {
          MAdvReq.findByAdId(adId, policy = sepo)
        }
        // Расставляем сообщения о депубликации для всех запросов размещения.
        val msg = SIOM_REFUSE_REASON
        advsReq.foreach { madvReq =>
          trace(s"${logPrefix}refusing advReq[${madvReq.id.get}]...")
          // TODO Нужно как-то управлять причиной выпиливания. Этот action работает через POST, поэтому можно замутить форму какую-то.
          MmpDailyBilling.refuseAdvReq(madvReq, msg)
        }
      }
    }(AsyncUtil.jdbcExecutionContext)
    dbUpdFut flatMap { _ =>
      isOkFut
    }
  }

}


/** Интерфейс для модулей рассчета extra-ресиверов карточки. */
trait AdvExtraRcvrsCalculator {

  /** Метод для проверки активности модуля. */
  def isEnabled: Boolean

  /**
   * Рассчет доп.ресиверов на основе карты прямых (непосредственных) ресиверов карточки.
   * @param allDirectRcvrs Карта непосредственных ресиверов.
   * @param producerId id продьюсера.
   * @return Фьючерс с картой extra-ресиверов.
   */
  def calcForDirectRcvrs(allDirectRcvrs: Receivers_t, producerId: String): Future[Receivers_t]
}



// JMX утиль
/** MBean-интерфейс для доступа к сабжу. */
trait AdvUtilJmxMBean {

  /** Пройтись по карточкам, пересчитать всех ресиверов для каждой карточки и сохранить в хранилище. */
  def resetAllReceivers(): String

  /**
   * Пересчитать ресиверов для карточки и сохранить в карточку.
   * @param adId id карточки.
   */
  def resetReceiversForAd(adId: String): String

  /**
   * Депубликация указанной рекламной карточки отовсюду.
   * @param adId id рекламной карточки.
   */
  def depublishAd(adId: String): String

  /**
   * Депубликация рекламной карточки на указанном узле.
   * @param adId id рекламной карточки.
   * @param rcvrId id ресивера.
   */
  def depublishAdAt(adId: String, rcvrId: String): String
}

/** Реализация MBean'а для прямого взаимодействия с AdvUtil. */
final class AdvUtilJmx extends AdvUtilJmxMBean with JMXBase {

  override def jmxName = "util:type=adv,name=" + getClass.getSimpleName.replace("Jmx", "")

  override def resetAllReceivers(): String = {
    val cntFut = MAd.updateAll() { mad0 =>
      AdvUtil.calculateReceiversFor(mad0) map { rcvrs1 =>
        mad0.copy(
          receivers = rcvrs1
        )
      }
    }
    val s = cntFut map { cnt =>
      "Total updated: " + cnt
    }
    awaitString(s)
  }

  override def resetReceiversForAd(adId: String): String = {
    val s = MAd.getById(adId) flatMap {
      case None =>
        Future successful s"Not found ad: $adId"
      case Some(mad) =>
        AdvUtil.calculateReceiversFor(mad) flatMap { rcvrs1 =>
          MAd.tryUpdate(mad) { mad0 =>
            mad0.copy(
              receivers = rcvrs1
            )
          } map { s =>
            "Successfully reset receivers: " + s + "\n\n" + rcvrs1
          }
        }
    }
    awaitString(s)
  }

  override def depublishAd(adId: String): String = {
    val s = AdvUtil.removeAdRcvr(adId, rcvrIdOpt = None)
      .map { isOk => "Result: " + isOk }
    awaitString(s)
  }

  override def depublishAdAt(adId: String, rcvrId: String): String = {
    val s = AdvUtil.removeAdRcvr(adId, rcvrIdOpt = Some(rcvrId))
      .map { isOk => "Result: " + isOk }
    awaitString(s)
  }

}

