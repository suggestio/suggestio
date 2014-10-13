package util.showcase

import models.{GeoSearchInfo, AdSearch}
import util.PlayMacroLogsImpl
import util.acl.AbstractRequestWithPwOpt
import util.event.SiowebNotifier.Implicts.sn
import play.api.http.HeaderNames.USER_AGENT

import io.suggest.model.OptStrId
import io.suggest.util.UuidUtil
import models.stat.ScStatAction
import net.sf.uadetector.service.UADetectorServiceFactory
import org.joda.time.DateTime
import util._
import util.acl._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.10.14 14:37
 * Description: Утиль для построения сборщиков статистики по разным экшенам SM-выдачи. + Сами сборщики статистики.
 * Этот велосипед возник из-за необъятности модели MAdStat и необходимости заполнять её немного по-разному
 * в разных случаях ситуациях, при этом с минимальной дубликацией кода и легкой расширяемостью оного.
 */
trait ScStatUtilT extends PlayMacroLogsImpl {

  import LOGGER._

  implicit def request: AbstractRequestWithPwOpt[_]
  def gsiFut: Future[Option[GeoSearchInfo]]
  def madIds: Set[String]

  val uaOpt = {
    request
      .headers
      .get(USER_AGENT)
      .filter(!_.isEmpty)
  }

  val agent = uaOpt.flatMap { ua =>
    // try-catch для самозащиты от возможных багов в православной либе uadetector.
    try {
      val uaParser = UADetectorServiceFactory.getResourceModuleParser
      Some(uaParser.parse(ua))
    } catch {
      case ex: Throwable =>
        warn("saveStats(): Unable to use UADetector for parsing UA: " + ua, ex)
        None
    }
  }

  def withHeadAd: Boolean = false

  def forceFirstMadIds: Seq[String] = Seq.empty

  val clickedAdIds = {
    if (withHeadAd && forceFirstMadIds.nonEmpty) {
      forceFirstMadIds
        .find { madIds.contains }
        .toSeq
    } else {
      Nil
    }
  }

  def saveStats(a: AdSearch) : Future[_] = {
    ???
  }

}
