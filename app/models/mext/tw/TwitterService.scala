package models.mext.tw

import models.mext.IExtService
import util.adv.OAuth1ServiceActorFactory
import util.ext.tw.TwitterHelper

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:14
 * Description: Абстрактная реализация twitter-сервиса.
 */
trait TwitterService extends IExtService {

  override def helperCt = ClassTag( classOf[TwitterHelper] )

  /** Ссылка на главную твиттера, и на собственный акк, если юзер залогинен. */
  override def mainPageUrl = "https://twitter.com/"

  override def nameI18N = "Twitter"
  override def dfltTargetUrl = Some(mainPageUrl)

  /** twitter работает через OAuth1. */
  override def extAdvServiceActorFactoryCt = ClassTag( classOf[OAuth1ServiceActorFactory] )

  /** Человекочитабельный юзернейм (id страницы) suggest.io на стороне сервиса. */
  override def myUserName = Some("@suggest_io")

}
