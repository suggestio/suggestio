package models.mext.vk

import io.suggest.ext.svc.MExtServices
import models.mext.{IAdvExtService, IExtService, IJsActorExtService}
import util.ext.vk.VkontakteHelper

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:24
 * Description: Абстрактная реализация внешнего сервиса vk.com.
 */
class VkService
  extends IExtService
  with IJsActorExtService
  with VkLoginProvider
  with IAdvExtService
{

  override def advExt = this

  override def helperCt = ClassTag(classOf[VkontakteHelper])

  /** Поддержка логина через вконтакт. */
  override def ssLoginProvider = Some(this)

  override def dfltTargetUrl = Some( MExtServices.VKontakte.mainPageUrl )

  override def cspSrcDomains: Iterable[String] = {
    "vk.com" ::
    "*.vk.com" ::           // login.vk.com, api.vk.com, возможно ещё какие-то.
    "*.vkontakte.ru" ::     // Не нужно, но на всякий случай.
    "*.vk.me" ::            // Не нужно, но на всякий случай.
    Nil
  }

}
