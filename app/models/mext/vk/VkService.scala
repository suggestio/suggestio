package models.mext.vk

import io.suggest.adv.ext.model.im.VkWallImgSizesScalaEnumT
import models.mext.IJsActorExtService
import util.ext.vk.VkontakteHelper

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:24
 * Description: Абстрактная реализация внешнего сервиса vk.com.
 */
trait VkService extends IJsActorExtService with VkLoginProvider {

  /** URL главной страницы сервиса. */
  override def mainPageUrl: String = "https://vk.com/"

  override def helperCt = ClassTag(classOf[VkontakteHelper])

  override def nameI18N = "VKontakte"

  /** Поддержка логина через вконтакт. */
  override def loginProvider = Some(this)

  override def dfltTargetUrl = Some(mainPageUrl)

}


/** Реализация модели размеров картинок vk. */
object VkImgSizes extends VkWallImgSizesScalaEnumT
