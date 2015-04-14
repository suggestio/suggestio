package models.mext.vk

import java.net.URL

import io.suggest.adv.ext.model.im.{INamedSize2di, VkWallImgSizesScalaEnumT}
import models.mext.{IMpUploadSupport, IExtService}
import util.adv.ExtServiceActor

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:24
 * Description: Абстрактная реализация внешнего сервиса vk.com.
 */
trait VkService extends IExtService {

  override def nameI18N = "VKontakte"
  override def isForHost(host: String): Boolean = {
    "(?i)(www\\.)?vk(ontakte)?\\.(com|ru|me)$".r.pattern.matcher(host).find()
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

  override def dfltTargetUrl = Some("https://vk.com/")

  /**
   * Максимальные размеры картинки при постинге во вконтакт в соц.сеть в css-пикселях.
   * Система будет пытаться вписать картинку в этот размер.
   * У вконтакта экспирементальная макс.ширина - 601px почему-то.
   * @see [[https://vk.com/vkrazmer]]
   * @see [[https://pp.vk.me/c617930/v617930261/4b62/S2KQ45_JHM0.jpg]] хрень?
   * @return Экземпляр 2D-размеров.
   */
  override def advPostMaxSz(tgUrl: String) = VkImgSizes.VkWallDflt

  /** Найти стандартный (в рамках сервиса) размер картинки. */
  override def postImgSzWithName(n: String): Option[INamedSize2di] = {
    VkImgSizes.maybeWithName(n)
  }

  /** VK работает через openapi.js. */
  override def extAdvServiceActor = ExtServiceActor

  override def maybeMpUpload = Some(VkMpUpload)
}


/** Реализация модели размеров картинок vk. */
object VkImgSizes extends VkWallImgSizesScalaEnumT
