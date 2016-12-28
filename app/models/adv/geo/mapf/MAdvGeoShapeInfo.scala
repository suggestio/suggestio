package models.adv.geo.mapf

import io.suggest.adv.geo.MAdvGeoShapeProps

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 15:59
  * Description: модель представления выхлопа базы по теме текущего геошейпа одного или более размещений.
  */
object MAdvGeoShapeInfo {

  /** Собрать инстанс из аналогичного корежа.
    * Такие кортежи генерит slick на сервере. */
  def apply(tuple: AdvGeoShapeInfo_t): MAdvGeoShapeInfo = {
    val (gsStr, id, statusStr) = tuple
    apply(gsStr, MAdvGeoShapeProps(id, statusStr))
  }

}


/**
  * Класс модели представления данных по одному шейпу размещений.
  *

  * @param geoShapeStr Геометрия передаётся толстоватой GeoJSON-строкой.
  *                 т.к. GeoShape-модели пока не являются кроссплатформенными, а завязаны на сервер.
  */
case class MAdvGeoShapeInfo(
                             geoShapeStr : String,
                             props       : MAdvGeoShapeProps
                           )
