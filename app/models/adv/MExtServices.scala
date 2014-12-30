package models.adv

import java.net.URL

import io.suggest.model.EnumMaybeWithName
import io.suggest.util.UrlUtil
import play.api.libs.json.{JsString, JsObject}
import play.api.Play._

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

    /**
     * Бывает нужно закинуть в контекст какие-то данные для доступа к сервису или иные параметры.
     * @param jso Исходный JSON контекст.
     * @return Обновлённый JSON контекст.
     */
    def prepareContext(jso: JsObject): JsObject = jso

    /**
     * Клиент прислал upload-ссылку. Нужно её проверить на валидность.
     * @param url Ссылка.
     * @return true если upload-ссылка корректная. Иначе false.
     */
    def checkImgUploadUrl(url: String): Boolean = false
  }

  type MExtService = Val
  override type T = MExtService


  /** Сервис вконтакта. */
  val VKONTAKTE: MExtService = new Val("vk") {
    val APP_ID_OPT = configuration.getString("ext.adv.vk.api.id")

    override def i18nCode = "VKontakte"
    override def isForHost(host: String): Boolean = {
      "(?i)(www\\.)?vk(ontakte)?\\.(com|ru)$".r.pattern.matcher(host).find()
    }
    override def prepareContext(jso: JsObject): JsObject = {
      val jso1 = super.prepareContext(jso)
      APP_ID_OPT match {
        case Some(apiId) =>
          val v1 = Stream("appId" -> JsString(apiId)) ++ jso1.value.toStream
          JsObject(v1)
        case None =>
          jso1
      }
    }

    override def checkImgUploadUrl(url: String): Boolean = {
      try {
        new URL(url)
          .getHost
          .contains(".vk.com")
      } catch {
        case ex: Throwable => false
      }
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
