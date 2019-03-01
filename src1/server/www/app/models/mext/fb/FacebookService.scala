package models.mext.fb

import io.suggest.ext.svc.MExtServices
import io.suggest.proto.http.HttpConst
import models.mext.{IAdvExtService, IExtService, IJsActorExtService}
import util.ext.fb.FacebookHelper

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:21
 * Description: Абстрактная реализация facebook-сервиса.
 */
class FacebookService
  extends IExtService
  with IJsActorExtService
  with FbLoginProvider
  with IAdvExtService
{

  /** Поддержка логина через facebook. */
  override def ssLoginProvider = Some(this)

  override def advExt = this

  override def helperCt = ClassTag(classOf[FacebookHelper])

  override def dfltTargetUrl = Some( MExtServices.FaceBook.mainPageUrl + "me" )

  override def cspSrcDomains: Iterable[String] = {
    "facebook.com"   ::     // Не используется, но на всякий случай.
    "*.facebook.com" ::     // Почти все запросы к мордокниге улетают в эти домены.
    "*.fbcdn.net"  ::       // Не нужно, но на всякий случай.
    "*.facebook.net" ::     // .net нужен на самом шаге инициализации fb.js API в adv-ext.
    Nil
  }

  /** CSP: Список поддерживаемых протоколов для обычных запросов. */
  override def cspSrcProtos: List[String] = {
    // В dev-режиме:
    // The page’s settings observed the loading of a resource at
    // http://staticxx.facebook.com/connect/xd_arbiter/r/JtmcTFxyLye.js?version=42
    HttpConst.Proto.HTTP :: super.cspSrcProtos
  }

}
