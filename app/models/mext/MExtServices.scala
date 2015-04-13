package models.mext

import _root_.util.PlayLazyMacroLogsImpl
import io.suggest.adv.ext.model.MServices._
import io.suggest.adv.ext.model._
import models.adv.MExtTarget
import models.mext.fb.FacebookService
import models.mext.tw.TwitterService
import models.mext.vk.VkService
import play.api.i18n.Messages
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:13
 * Description: Мегамодель сервисов для внешнего размещения рекламных карточек.
 */

// TODO Нужно модель IdProviders замержить в эту модель.

object MExtServices extends MServicesT with PlayLazyMacroLogsImpl {

  override type T = Val

  /** Экземпляр модели. */
  protected sealed abstract class Val(strId: String) extends super.Val(strId) with IExtService {
    override lazy val APP_ID_OPT = super.APP_ID_OPT

    override def dfltTarget(adnId: String)(implicit lang: Messages): Option[MExtTarget] = {
      dfltTargetUrl.map { url =>
        MExtTarget(
          url     = url,
          adnId   = adnId,
          service = this,
          name    = Some(Messages(iAtServiceI18N))
        )
      }
    }

    override val imgFmt = super.imgFmt
    override val szMult = super.szMult
  }


  /** Сервис вконтакта. */
  val VKONTAKTE: T = new Val(VKONTAKTE_ID) with VkService

  /** Сервис фейсбука. */
  val FACEBOOK: T = new Val(FACEBOOK_ID) with FacebookService

  /** Сервис твиттера. */
  val TWITTER: T = new Val(TWITTER_ID) with TwitterService


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

}


/** Абстрактные метаданные по посту на внешнем сервисе. */
trait IExtPostInfo {
  def id: String
  def url: String
}

