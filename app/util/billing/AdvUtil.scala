package util.billing

import io.suggest.model.OptStrId
import io.suggest.ym.model.common.EMReceivers.Receivers_t
import io.suggest.ym.model.common.{IProducerId, EMReceiversI}
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.PlayMacroLogsImpl
import util.SiowebEsUtil.client
import AdReceiverInfo.formatReceiversMapPretty

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.14 18:19
 * Description: Утиль для работы с размещениями рекламных карточек.
 */
object AdvUtil extends PlayMacroLogsImpl {

  import LOGGER._

  /**
   * Пересчет текущих размещений рекламной карточки на основе данных других моделей.
   * Данные по саморазмещению мигрируют из исходных данных размещения.
   * @param mad Исходная рекламная карточка или её интерфейс.
   * @param producerOpt Экземпляр продьюсера
   */
  def calculateReceiversFor(mad: EMReceiversI with OptStrId with IProducerId, producerOpt: Option[MAdnNode] = None): Future[Receivers_t] = {
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
    // Считаем ресиверов через mmp-billing, т.е. платные размещения по времени.
    val receivers0 = MmpDailyBilling.calcualteReceiversMapForAd(mad.id.get)
    val resultFut = priOpt.fold(Future successful receivers0) { pri =>
      producerFut map { producer =>
        // Оставляем только уровни отображения, которые доступны ресиверу.
        val psls2 = pri.sls
          // Выкинуть sink-уровни продьюсера, которые не соответствуют доступным синкам и
          // уровням отображения, которые записаны в политике исходящих размещений
          .filter { ssl  =>  producer.adn.isReceiver  &&  producer.adn.hasSink(ssl.adnSink)  &&  producer.adn.canOutAtLevel(ssl.sl) }
        if (psls2.isEmpty) {
          // Удаляем саморесивер, т.к. уровни пусты.
          receivers0
        } else {
          // Добавляем собственный ресивер с обновлёнными уровнями отображениям.
          val prkv = pri.receiverId -> pri.copy(sls = psls2)
          receivers0 + prkv
        }
      }
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
