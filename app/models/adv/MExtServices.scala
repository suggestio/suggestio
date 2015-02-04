package models.adv

import java.net.URL

import io.suggest.model.EnumMaybeWithName
import io.suggest.util.UrlUtil
import models.adv.js.ctx.MJsCtx
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:13
 * Description: Модель сервисов для внешнего размещения рекламных карточек.
 * TODO ext.adv api v2: Модель осталась для совместимости, должна быть удалена или же стать неким каталогом скриптов,
 * либо ещё что-то...
 */
object MExtServices extends Enumeration with EnumMaybeWithName {

  val NAME_FN   = "name"
  val APP_ID_FN = "appId"

  /** Десериализация из JSON. Всё можно прочитать по имени. */
  implicit def reads: Reads[T] = {
    (__ \ NAME_FN)
      .read[String]
      .map { withName }
  }

  /** Сериализация в JSON. */
  implicit def writes: Writes[T] = (
    (__ \ NAME_FN).write[String] and
    (__ \ APP_ID_FN).writeNullable[String]
  ){ s => (s.strId, s.APP_ID_OPT) }


  /** Экземпляр модели. */
  protected abstract sealed class Val(val strId: String) extends super.Val(strId) {

    /** id приложения на стороне сервиса. */
    val APP_ID_OPT = configuration.getString(s"ext.adv.$strId.api.id")

    def i18nCode: String
    def isForHost(host: String): Boolean
    def normalizeTargetUrl(url: URL): String = {
      UrlUtil.normalize(url.toExternalForm)
    }

    /**
     * Бывает нужно закинуть в контекст какие-то данные для доступа к сервису или иные параметры.
     * @param mctx Исходный JSON контекст.
     * @return Обновлённый JSON контекст.
     */
    def prepareContext(mctx: MJsCtx): MJsCtx = {
      mctx.copy(
        service = Some(this)
      )
    }

    /**
     * Клиент прислал upload-ссылку. Нужно её проверить на валидность.
     * @param url Ссылка.
     * @return true если upload-ссылка корректная. Иначе false.
     */
    def checkImgUploadUrl(url: String): Boolean = false
  }

  override type T = Val


  /** Сервис вконтакта. */
  val VKONTAKTE: T = new Val("vk") {
    override def i18nCode = "VKontakte"
    override def isForHost(host: String): Boolean = {
      "(?i)(www\\.)?vk(ontakte)?\\.(com|ru)$".r.pattern.matcher(host).find()
    }

    override def checkImgUploadUrl(url: String): Boolean = {
      val v = try {
        new URL(url)
          .getHost
          .contains(".vk")
      } catch {
        case ex: Throwable => false
      }
      v || super.checkImgUploadUrl(url)
    }
  }


  /** Сервис фейсбука. */
  val FACEBOOK: T = new Val("fb") {
    override def i18nCode = "Facebook"
    override def isForHost(host: String): Boolean = {
      "(?i)(www\\.)?facebook\\.(com|net)$".r.pattern.matcher(host).matches()
    }
  }


  /** Сервис твиттера. */
  val TWITTER: T = new Val("tw") {
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
