package io.suggest.bill.price.dsl

import io.suggest.common.html.HtmlConstants
import io.suggest.geo.{CircleGs, DistanceUtil}
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.18 22:20
  * Description: Утиль для рендера и интернационализации price reason'ов.
  */
object PriceReasonI18n {

  def i18nPayload(payload: MPriceReason): Option[String] = {
    payload.reasonType match {
      case MReasonTypes.GeoArea =>
        // Пытаемся отрендерить инфу по гео-кругу:
        payload.geoCircles
          .headOption
          .map( i18nPayloadCircle )

      case MReasonTypes.BlockModulesCount =>
        payload.ints
          .headOption
          .map( i18nPayloadModulesCount )

      case MReasonTypes.Tag =>
        payload.strings
          .headOption
          .map( i18nPayloadTagFace )

      case MReasonTypes.Rcvr =>
        payload.nameIds
          .headOption
          .map(_.name)

      // Остальные типы не содержат нагрузки, пригодной для рендера.
      case _ => None
    }
  }


  def i18nPayloadCircle(circleGs: CircleGs): String = {
    val distanceStr = DistanceUtil.formatDistanceM( circleGs.radiusM )(Messages.f)
    val coordsStr = circleGs.center.toHumanFriendlyString
    Messages(
      MsgCodes.`in.radius.of.0.from.1`,
      distanceStr :: coordsStr :: Nil
    )
  }


  def i18nPayloadModulesCount(blockModulesCount: Int): String =
    Messages( MsgCodes.`N.modules`, blockModulesCount :: Nil )

  def i18nPayloadTagFace(tagFace: String): String =
    HtmlConstants.TAG_PREFIX + tagFace


}
