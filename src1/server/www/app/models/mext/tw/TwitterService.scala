package models.mext.tw

import io.suggest.ext.svc.MExtServices
import models.mext.{IAdvExtService, IExtService}
import util.adv.ext.OAuth1ServiceActorFactory
import util.ext.tw.TwitterHelper

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:14
 * Description: Абстрактная реализация twitter-сервиса.
 */
class TwitterService
  extends IExtService
  with IAdvExtService
{

  override def advExt = this

  override def dfltTargetUrl = Some( MExtServices.TWITTER.mainPageUrl )

  override def helperCt = ClassTag( classOf[TwitterHelper] )

  /** twitter работает через OAuth1. */
  override def extAdvServiceActorFactoryCt = ClassTag( classOf[OAuth1ServiceActorFactory] )

  override def cspSrcDomains: Iterable[String] = {
    "*.twitter.com" ::
      Nil
  }

}
