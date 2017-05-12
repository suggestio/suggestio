package models.mext.fb

import io.suggest.adv.ext.model.im.FbWallImgSizesScalaEnumT
import models.mext.IJsActorExtService
import util.ext.fb.FacebookHelper

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:21
 * Description: Абстрактная реализация facebook-сервиса.
 */
trait FacebookService
  extends IJsActorExtService
  with FbLoginProvider
{

  override def helperCt = ClassTag(classOf[FacebookHelper])

  /** URL главной страницы сервиса. */
  override def mainPageUrl: String = "https://facebook.com/"

  override def nameI18N = "Facebook"

  /** Поддержка логина через facebook. */
  override def loginProvider = Some(this)

  override def dfltTargetUrl = Some(mainPageUrl + "me")

  override def cspSrcDomains: Iterable[String] = {
    "facebook.com"   ::     // Не используется, но на всякий случай.
    "*.facebook.com" ::     // Почти все запросы к мордокниге улетают в эти домены.
      "*.fbcdn.net"  ::      // Не нужно, но на всякий случай.
      "*.facebook.net" ::   // .net нужен на самом шаге инициализации fb.js API в adv-ext.
      Nil
  }

  /** CSP: Список поддерживаемых протоколов для обычных запросов. */
  override def cspSrcProtos: List[String] = {
    // В dev-режиме:
    // The page’s settings observed the loading of a resource at
    // http://staticxx.facebook.com/connect/xd_arbiter/r/JtmcTFxyLye.js?version=42
    "http" :: super.cspSrcProtos
  }
}

/** Реализация модели размеров картинок фейсбука. */
object FbImgSizes extends FbWallImgSizesScalaEnumT
