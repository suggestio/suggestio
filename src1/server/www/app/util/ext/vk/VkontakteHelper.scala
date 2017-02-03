package util.ext.vk

import java.net.URL

import com.google.inject.{Inject, Singleton}
import io.suggest.common.geom.d2.INamedSize2di
import io.suggest.util.logs.MacroLogsImpl
import models.im.{OutImgFmt, OutImgFmts}
import models.mext.MExtServices
import models.mext.vk.VkImgSizes
import models.mproj.ICommonDi
import play.api.libs.ws.WSClient
import util.ext.IExtServiceHelper

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.16 11:07
  * Description: Утиль для взаимодействия с вконтактом.
  */
@Singleton
class VkontakteHelper @Inject() (
  override val wsClient     : WSClient,
  override val mCommonDi    : ICommonDi
)
  extends IExtServiceHelper
  with VkMpUpload
  with MacroLogsImpl
{

  override def mExtService = MExtServices.VKONTAKTE

  override def checkImgUploadUrl(url: String): Boolean = {
    val v = try {
      new URL(url)
        .getHost
        .contains(".vk")
    } catch {
      case ex: Throwable =>
        LOGGER.debug("checkImgUploadUrl(): Invalid URL: " + url)
        false
    }
    v || super.checkImgUploadUrl(url)
  }

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

  override def maybeMpUpload = Some(this)

  override def imgFmtDflt: OutImgFmt = OutImgFmts.JPEG

  override def isForHost(host: String): Boolean = {
    "(?i)(www\\.)?vk(ontakte)?\\.(com|ru|me)$".r
      .pattern
      .matcher(host)
      .find()
  }

}
