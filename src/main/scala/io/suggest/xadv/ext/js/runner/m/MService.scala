package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model._
import io.suggest.adv.ext.model.MServices._

import scala.scalajs.js
import scala.scalajs.js.WrappedDictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 13:27
 * Description: Модель поддерживаемых сервисов.
 */
object MServices extends MServicesLightT {
  sealed protected class Val(val strId: String) extends ValT /*{
    def customFromDynamic(raw: js.Dynamic): Option[IToJson] = None
  }*/

  override type T = Val

  override val FACEBOOK: T = new Val(FACEBOOK_ID)
  override val VKONTAKTE: T = new Val(VKONTAKTE_ID)
  override val TWITTER: T = new Val(TWITTER_ID)
}


object MServiceInfo extends FromStringT {

  override type T = MServiceInfo

  def fromJson(raw: js.Any): MServiceInfo = {
    val d = raw.asInstanceOf[js.Dictionary[String]] : WrappedDictionary[String]
    MServiceInfo(
      service = MServices.withName( d(NAME_FN) ),
      appId   = d.get(APP_ID_FN)
    )
  }

}


case class MServiceInfo(service: MService, appId: Option[String]) extends IToJsonDict {
  override def toJson = {
    val d = js.Dictionary[js.Any](
      NAME_FN -> service.strId
    )
    if (appId.isDefined)
      d.update(APP_ID_FN, appId.get)
    d
  }
}

