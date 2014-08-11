package models

import java.sql.Connection

import anorm._
import util.SqlModelSave

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.14 18:35
 * Description: База городов для базы ip из ipgeobase.ru.
 */
object IpGeoBaseCity extends SqlModelStatic {

  override val TABLE_NAME = "ipgeobase_city"
  override type T = IpGeoBaseCity
  override val rowParser: RowParser[T] = {
    import SqlParser._
    get[Int]("id") ~ get[String]("city_name") ~ get[String]("region") ~ get[Double]("lat") ~ get[Double]("lon") map {
      case id ~ cityName ~ region ~ lat ~ lon =>
        IpGeoBaseCity(id = id, cityName = cityName, region = region, lat = lat, lon = lon)
    }
  }

}

import IpGeoBaseCity._

case class IpGeoBaseCity(
  id        : Int,
  cityName  : String,
  region    : String,
  lat       : Double,
  lon       : Double
) extends SqlModelSave[IpGeoBaseCity] {

  override def hasId = true

  override def saveInsert(implicit c: Connection): IpGeoBaseCity = {
    SQL(s"INSERT INTO $TABLE_NAME(id, city_name, region, lat, lon) VALUES({id}, {cityName}, {region}, {lat}, {lon})")
      .on('id -> id, 'cityName -> cityName, 'region -> region, 'lat -> lat, 'lon -> lon)
      .executeInsert(rowParser single)
  }

  override def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET city_name = {cityName}, region = {region}, lat = {lat}, lon = {lon} WHERE id = {id}")
      .on('id -> id, 'cityName -> cityName, 'region -> region, 'lat -> lat, 'lon -> lon)
      .executeUpdate()
  }
}


/** Примесь к экземпляру другой модели, которая имеет необязательное поле со значением city_id. */
trait IpGeoBaseCityIdOpt {
  def cityId: Option[Int]
  def cityOpt(implicit c: Connection): Option[IpGeoBaseCity] = {
    cityId flatMap { _cityId =>
      IpGeoBaseCity.getById(_cityId)
    }
  }
}