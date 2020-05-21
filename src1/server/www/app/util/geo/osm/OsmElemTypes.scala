package util.geo.osm

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.05.2020 15:43
  */

object OsmElemTypes extends StringEnum[OsmElemType] {

  case object NODE extends OsmElemType("node") {
    override def xmlUrl(id: Long) = s"http://www.openstreetmap.org/api/0.6/node/$id"
  }

  case object WAY extends OsmElemType("way") {
    override def xmlUrl(id: Long) = s"http://www.openstreetmap.org/api/0.6/way/$id/full"
  }

  case object RELATION extends OsmElemType("relation") {
    override def xmlUrl(id: Long) = s"http://www.openstreetmap.org/api/0.6/relation/$id/full"
  }


  override def values = findValues

}


sealed abstract class OsmElemType( override val value: String ) extends StringEnumEntry {
  def xmlUrl(id: Long): String
}
object OsmElemType {
  @inline implicit def univEq: UnivEq[OsmElemType] = UnivEq.derive
}
