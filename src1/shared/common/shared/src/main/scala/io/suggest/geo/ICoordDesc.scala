package io.suggest.geo

import io.suggest.common.geom.coord.GeoCoord_t
import io.suggest.math.MathConst

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.01.17 11:13
  * Description: Модели с описаниями свойств географических координат.
  *
  * sealed, т.к. пока не планируется реализовывать этот трейт за пределами файла.
  */
sealed trait ICoordDesc {

  /** Имя поля на стороне ElasticSearch. */
  def ES_FN: String

  /** Имя query-string поля. */
  def QS_FN: String = ES_FN

  /** Абсолютная граница значений координаты: -bound <= x <= +bound. */
  def BOUND: GeoCoord_t

  def BOUND_UPPER: GeoCoord_t = BOUND
  def BOUND_LOWER: GeoCoord_t = -BOUND

  /** messages-код сообщения об ошибке. */
  def E_INVALID = "e.coord." + ES_FN

  /** Проверка на валидность значения координаты. */
  def isValid(value: GeoCoord_t): Boolean = {
    value >= BOUND_LOWER &&
    value <= BOUND_UPPER
  }

  /** Принудительное запихивание значения координаты в разрешенный диапазон. */
  def ensureInBounds(value: GeoCoord_t): GeoCoord_t = {
    BOUND_UPPER.min(
      BOUND_LOWER.max( value )
    )
  }


  import scalaz.Validation
  import scalaz.ValidationNel

  def validator(value: GeoCoord_t): ValidationNel[String, GeoCoord_t] = {
    val v = MathConst.FracScale.scaledOptimal(value, MGeoPoint.FracScale.DEFAULT)
    Validation.liftNel(v)(!isValid(_), E_INVALID)
  }

}


/**
  * Модель свойств широты (параллелей).
  * @see [[https://en.wikipedia.org/wiki/Latitude]]
  */
case object Lat extends ICoordDesc {

  override def QS_FN = "a"

  override def ES_FN = "lat"

  override def BOUND = 90d

}


/**
  * Модель свойств долготы (меридианы).
  * @see [[https://en.wikipedia.org/wiki/Longitude]]
  */
case object Lon extends ICoordDesc {

  override def QS_FN = "o"

  override def ES_FN = "lon"

  override def BOUND = 180d

}

