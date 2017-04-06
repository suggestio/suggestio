package io.suggest.geo

import boopickle.Default._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 18:47
  * Description: Кроссплатформенная модель описания географического круга.
  */
object MGeoCircle {

  implicit val mGeoCirlePickler: Pickler[MGeoCircle] = {
    implicit val mgpP = MGeoPoint.pickler
    generatePickler[MGeoCircle]
  }


  /*
  import com.wix.accord.Validator
  import com.wix.accord.dsl._

  implicit val VALIDATOR: Validator[MGeoCircle] = {
    validator[MGeoCircle] { mgc =>
      mgc.center is valid
      mgc.radiusM should be >= 1d
    }
  }
  */

}


/** Состояние круга. */
case class MGeoCircle(
                       center   : MGeoPoint,
                       radiusM  : Double    = 5000
                     ) {

  def radiusKm = radiusM / 1000d

  def withCenter(center2: MGeoPoint) = copy(center = center2)

  def withRadiusM(radius2: Double) = copy(radiusM = radius2)

}