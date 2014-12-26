package models.adv

import java.net.URL

import io.suggest.model.EnumMaybeWithName
import io.suggest.util.UrlUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:13
 * Description: Модель сервисов для внешнего размещения рекламных карточек.
 */
object MExtServices extends Enumeration with EnumMaybeWithName {

  protected abstract sealed class Val(val strId: String) extends super.Val(strId) {
    def i18nCode: String
    def isForHost(host: String): Boolean
    def normalizeTargetUrl(url: URL): String = {
      UrlUtil.normalize(url.toExternalForm)
    }
  }

  type MExtService = Val
  override type T = MExtService


  /** Сервис вконтакта. */
  val VKONTAKTE: MExtService = new Val("vk") {
    override def i18nCode = "VKontakte"
    override def isForHost(host: String): Boolean = {
      "(?i)(www\\.)?vk(ontakte)?\\.(com|ru)$".r.pattern.matcher(host).find()
    }
  }

  /** Сервис фейсбука. */
  val FACEBOOK: MExtService = new Val("fb") {
    override def i18nCode = "Facebook"
    override def isForHost(host: String): Boolean = {
      "(?i)(www\\.)?facebook\\.(com|net)$".r.pattern.matcher(host).matches()
    }
  }

  /** Сервис твиттера. */
  val TWITTER: MExtService = new Val("tw") {
    override def i18nCode = "Twitter"
    override def isForHost(host: String): Boolean = {
      "(?i)(www\\.)?twitter\\.com".r.pattern.matcher(host).matches()
    }
  }

  /**
   * Поиск подходящего сервиса для указанного хоста.
   * @param host Хостнейм искомого сервиса.
   * @return Сервис, если такой есть.
   */
  def findForHost(host: String): Option[MExtService] = {
    values
      .find(_.isForHost(host))
      .map(value2val)
  }

}
