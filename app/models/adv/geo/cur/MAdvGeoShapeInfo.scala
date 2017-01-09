package models.adv.geo.cur

import io.suggest.mbill2.m.gid.Gid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 15:59
  * Description: модель представления выхлопа базы по теме текущего геошейпа одного или более размещений.
  */
object MAdvGeoShapeInfo {

  /** Собрать инстанс из аналогичного корежа.
    * Такие кортежи генерит slick на сервере. */
  def applyOpt(tuple: AdvGeoShapeInfo_t): Option[MAdvGeoShapeInfo] = {
    val (gsStrOpt, idOpt, statusStrOpt) = tuple
    for {
      gsStr <- gsStrOpt
      id    <- idOpt
      statusStr <- statusStrOpt
    } yield {
      apply(gsStr, id, statusStr)
    }
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
                             itemId      : Gid_t,
                             hasApproved : Boolean
                           )
