package io.suggest.maps.m

import io.suggest.adv.geo.AdvGeoConstants.GjFtPropsC.{CIRCLE_RADIUS_M_FN, HAS_APPROVED_FN, ITEM_ID_FN}

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 12:28
  * Description: JSON-фасад для JSON-модели пропертисов одной GeoJSON фичи, описывающей данные
  * по уже существующим георазмещениям.
  */

@js.native
trait MGeoAdvExistGjFtProps extends js.Object {

  // Long тут не пашет нормальном, поэтому Double.
  @JSName( ITEM_ID_FN )
  val itemId: Double = js.native

  @JSName( HAS_APPROVED_FN )
  val hasApproved: Boolean = js.native

  @JSName( CIRCLE_RADIUS_M_FN )
  var radiusM: UndefOr[Double] = js.native

}


object MGeoAdvExistGjFtProps {

  implicit def fromAny(raw: js.Any): MGeoAdvExistGjFtProps = {
    raw.asInstanceOf[MGeoAdvExistGjFtProps]
  }

}
