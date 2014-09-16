package util.adv

import io.suggest.ym.model.common.EMReceivers.Receivers_t
import models.AdReceiverInfo.formatReceiversMapPretty
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.PlayMacroLogsImpl
import util.SiowebEsUtil.client
import util.billing.MmpDailyBilling

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
        .filter { _.id.exists(_ == mad.producerId) }
        .fold[Future[MAdnNode]]
          { MAdnNodeCache.getById(mad.producerId).map(_.get) }
          { Future successful }
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
        Future.reduce(prodResultFut :: extrasFuts) { AdReceiverInfo.mergeRcvrMaps(_, _) }
      }

    // Если trace, то нужно сообщить разницу в карте ресиверов до и после.
    if (LOGGER.underlying.isTraceEnabled) {
      resultFut onSuccess { case result =>
        trace(s"calculateReceiversFor(${mad.id.get}): withProducer=$needProducer\n  oldRcvrs = ${formatReceiversMapPretty(mad.receivers)}\n  newRcvrs=${formatReceiversMapPretty(result)}")
      }
    }
    resultFut
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

