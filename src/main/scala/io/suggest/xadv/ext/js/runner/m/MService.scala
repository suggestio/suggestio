package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model._
import io.suggest.adv.ext.model.MServices._

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 13:27
 * Description: Модель поддерживаемых сервисов.
 */
object MServices extends MServicesLightT


object MServiceInfo extends FromStringT {

  override type T = MServiceInfo

  def fromDyn(raw: js.Dynamic): MServiceInfo = {
    val d = raw.asInstanceOf[js.Dictionary[String]]
    MServiceInfo(
      service = MServices.withName( d.get(NAME_FN).get ),
      appId   = d.get(APP_ID_FN)
    )
  }

}


case class MServiceInfo(service: MService, appId: Option[String]) {
  def toJson: js.Dynamic = {
    val lit = js.Dynamic.literal()
    lit.updateDynamic(NAME_FN)(service.strId)
    if (appId.isDefined) {
      lit.updateDynamic(APP_ID_FN)(appId.get)
    }
    lit
  }
}

