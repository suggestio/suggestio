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

}

/** Реализация модели размеров картинок фейсбука. */
object FbImgSizes extends FbWallImgSizesScalaEnumT
