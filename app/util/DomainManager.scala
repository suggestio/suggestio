package util

import io.suggest.event.SNStaticSubscriber
import io.suggest.event.SioNotifier.{Classifier, Subscriber}
import io.suggest.event.subscriber.SnFunSubscriber
import util.event.{ValidSioJsFoundOnSite, SioJsFoundOnSite}
import scala.util.{Failure, Success}
import play.api.libs.concurrent.Execution.Implicits._
import models.MDomain
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.10.13 12:09
 * Description: Установка новых доменов и управление ими происходит отсюда.
 */
object DomainManager extends SNStaticSubscriber with Logs {

  import LOGGER._

  def snMap: List[(Classifier, Seq[Subscriber])] = {
    val c = SioJsFoundOnSite.getClassifier(isValidOpt = Some(true))
    val s = SnFunSubscriber {
      case ValidSioJsFoundOnSite(_dkey, _pwOpt) =>
        val addedBy = _pwOpt.map(_.personId) getOrElse "anonymous"
        maybeInstallDkey(_dkey, addedBy)
    }
    List(c -> Seq(s))
  }

  /**
   * Запрос на установку домена в систему. Возможно, он уже установлен.
   * @param dkey Ключ домена.
   */
  def maybeInstallDkey(dkey:String, addedBy:String) = {
    trace(s"maybeInstallDkey($dkey): starting")
    // TODO
    installDkey(dkey=dkey, addedBy=addedBy)
  }

  /**
   * Выполнить все действия для добавления домена в кравлер.
   * @param dkey Ключ домена.
   */
  def installDkey(dkey: String, addedBy: String) = {
    lazy val logPrefix = s"installDkey($dkey): "
    val seedUrls = List("http://" + dkey + "/")
    trace(logPrefix + "seedUrls = " + seedUrls)
    // TODO Тут нужно определить прямую ссылку без редиректов: сделав HEAD/GET-запросы по этой ссылке и затем отработав
    //      редиректы с проверкой dkey.
    val fut = SiobixClient.maybeBootstrapDkey(dkey, seedUrls)
    fut onComplete {
      case Success(r)  =>
        info(logPrefix + "success; result = " + r)
        new MDomain(dkey=dkey, addedBy=addedBy).save

      case Failure(ex) => error(logPrefix + "crawler ask failed", ex)
    }
    fut
  }

}
