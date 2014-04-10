package util

import io.suggest.event._
import models._
import SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.{Failure, Success}
import util.event.SiowebNotifier
import scala.concurrent.Future
import play.api.cache.Cache
import play.api.Play.current
import io.suggest.util.SioEsUtil.laFuture2sFuture
import io.suggest.event.SioNotifier.{Subscriber, Classifier}
import io.suggest.event.subscriber.SnFunSubscriber
import io.suggest.ym.ad.ShowLevelsUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.14 17:22
 * Description: Разные фунции и прочая утиль для работы с индексами.
 */

object IndicesUtil extends PlayMacroLogsImpl with SNStaticSubscriber {

  import LOGGER._

  implicit private def sn = SiowebNotifier

  /** Подписчик на события в виде анонимной фунцкии. */
  private def snSubscriber = SnFunSubscriber {
    case e: IAdnId =>
      MAdnNode.getById(e.adnId) onSuccess {
        case Some(prod) => ShowLevelsUtil.handleProducerOnOff(prod)
        case None => warn(e + ": Producer disabled, but it is not found!")
      }
  }

  /** Карта подписок на события. */
  def snMap: Seq[(Classifier, Seq[Subscriber])] = {
    val subs = Seq(snSubscriber)
    Seq(
      AdnNodeOnOffEvent.getClassifier() -> subs,
      AdnNodeSavedEvent.getClassifier() -> subs
    )
  }

  /** Имя дефолтового индекса для индексации MMart. Используется пока нет менеджера индексов,
    * и одного индекса на всех достаточно. */
  // TODO Вероятно, поддержка механизма inx2 будет удалена или как-то изменена под нужды кравлера.
  def MART_INX_NAME_DFLT = "--1siomart"

}
