package util.compat

import io.suggest.util.JMXBase
import io.suggest.ym.model.MAd
import io.suggest.ym.model.ad.AOBlock

import scala.concurrent.Future
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.02.15 12:21
 * Description: Наконец было принято решение по выпиливанию text2-поля, которое дублировало text1, но при этом жило-было
 * чуть менее чем везде.
 */
object BlockOffersRemoveText2 {

  /**
   * Для всех рекламных карточек надо обновить содержимое поля offers таким образом,
   * чтобы данные не потерялись и чтобы поле text2 было пустым.
   * @return Фьючерс для синхронизации. Внутри -- кол-во отработанных карточек.
   */
  def processAllAds(): Future[Int] = {
    MAd.updateAll() { mad0 =>
      val mad1 = mad0.copy(
        offers = mad0.offers
          .foldLeft(0 -> List.empty[AOBlock]) { case (xacc @ (nextN, acc), offer) =>
            (offer.text1.isDefined, offer.text2.isDefined) match {
              // Два оффера в одном. Раскрываем.
              case (true, true) =>
                val acc1 =
                  offer.copy(n = nextN, text2 = None) ::
                  offer.copy(n = nextN + 1, text1 = offer.text2, text2 = None) ::
                  acc
                (nextN + 2) -> acc1
              // Есть только text1. В этом оффере нужно только обновить счетчик.
              case (true, false) =>
                (nextN + 1) -> (offer.copy(n = nextN) :: acc)
              // Есть только text2. Переместить данные в text1.
              case (false, true) =>
                (nextN + 1) -> (offer.copy(n = nextN, text1 = offer.text2, text2 = None) :: acc)
              // (Should never happen) Это пустой оффер. Его надо просто выкинуть.
              case (false, false) =>
                xacc
            }
          }
          ._2
          .reverse
      )
      // Самоконтроль: проверить, что text2 везде пустой, а text1 везде заполнен.
      val isOk = mad1.offers
        .forall { offer => offer.text1.nonEmpty && offer.text2.isEmpty }
      if (isOk) {
        Future successful mad1
      } else {
        Future failed new IllegalStateException(s"Something gonna wrong.\n  mad0.offers = ${mad0.offers}\n mad1.offers= ${mad1.offers}")
      }
    }
  }

}


// Поддержка JMX для взаимодействия с модулем.
trait BlockOffersRemoveText2JmxMBean {
  def processAllIds(): String
}
class BlockOffersRemoveText2Jmx extends JMXBase with BlockOffersRemoveText2JmxMBean {
  override def jmxName: String = "io.suggest.model:type=compat,name=" + getClass.getSimpleName.replace("Jmx", "")

  override def processAllIds(): String = {
    val fut = BlockOffersRemoveText2.processAllAds()
      .map { count => "Ok. Total processed: " + count }
    awaitString(fut)
  }
}
