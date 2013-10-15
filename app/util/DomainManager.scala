package util

import io.suggest.event.SNStaticSubscriber
import io.suggest.event.SioNotifier.{Classifier, Subscriber}
import io.suggest.event.subscriber.SnFunSubscriber
import util.event.{ValidSioJsFoundOnSite, SioJsFoundOnSite}
import scala.util.{Failure, Success}
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.10.13 12:09
 * Description: Установка новых доменов и управление ими происходит отсюда.
 */
object DomainManager extends SNStaticSubscriber with Logs {

  import LOGGER._

  def snMap: Seq[(Classifier, Seq[Subscriber])] = {
    val c = SioJsFoundOnSite.getClassifier(isValidOpt = Some(true))
    val s = SnFunSubscriber {
      case ValidSioJsFoundOnSite(_dkey) => maybeInstallDkey(_dkey)
    }
    Seq(c -> Seq(s))
  }

  /**
   * Запрос на установку домена в систему. Возможно, он уже установлен.
   * @param dkey Ключ домена.
   */
  def maybeInstallDkey(dkey: String) = {
    trace(s"maybeInstallDkey($dkey): starting")
    // TODO
    installDkey(dkey)
  }

  /**
   * Выполнить все действия для добавления домена в кравлер.
   * @param dkey Ключ домена.
   */
  def installDkey(dkey: String) = {
    lazy val logPrefix = s"installDkey($dkey): "
    val seedUrls = List("http://" + dkey + "/")
    trace(logPrefix + "seedUrls = " + seedUrls)
    // TODO Тут нужно определить прямую ссылку без редиректов: сделав HEAD/GET-запросы по этой ссылке и затем отработав
    //      редиректы с проверкой dkey.
    val fut = SiobixClient.maybeBootstrapDkey(dkey, seedUrls)
    fut onComplete {
      case Success(r)  => info(logPrefix + "success; result = " + r)
      case Failure(ex) => error(logPrefix + "crawler ask failed", ex)
    }
    fut
  }

}
