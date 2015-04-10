package models.mext.tw

import _root_.util.adv.OAuth1ServiceActor
import models.mext.IExtService

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:14
 * Description: Абстрактнаня реализация twitter-сервиса.
 */
trait TwitterService extends IExtService {

  override def nameI18N = "Twitter"

  override def isForHost(host: String): Boolean = {
    "(?i)(www\\.)?twitter\\.com".r.pattern.matcher(host).matches()
  }
  override def dfltTargetUrl = None

  /** Найти стандартный (в рамках сервиса) размер картинки. */
  override def postImgSzWithName(n: String) = ???  // TODO

  override def advPostMaxSz(tgUrl: String) = ???    // TODO

  /** Префикс ключей конфигурации. Конфиг расшарен с secure-social. */
  def confPrefix = "securesocial.twitter"

  /** twitter работает через OAuth1. */
  override def extAdvServiceActor = OAuth1ServiceActor

  lazy val oa1Support = new OAuth1Support(confPrefix)

  /** Поддержка OAuth1 на текущем сервисе. None означает, что нет поддержки. */
  override def oauth1Support = Some(oa1Support)

  /** Человекочитабельный юзернейм (id страницы) suggest.io на стороне сервиса. */
  override def myUserName = Some("@suggest_io")

}
