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
    
  }
}

import IpGeoBaseCity._

case class IpGeoBaseCity(
  id        : Option[Int],
  cityName  : String,
  region    : String,
  lat       : Double,
  lon       : Double
) extends SqlModelSave[IpGeoBaseCity] with SqlModelDelete {

  override def hasId = id.isDefined

  override def saveInsert(implicit c: Connection): IpGeoBaseCity = {
    SQL()
  }

  override def saveUpdate(implicit c: Connection): Int = ???
}

