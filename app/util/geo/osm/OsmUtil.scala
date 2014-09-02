package util.geo.osm

import io.suggest.model.geo.GeoPoint

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.09.14 12:03
 * Description: Вспомогательная утиль для работы с osm.
 */

import OsmElemTypes.OsmElemType

trait OsmObject {
  def id: Long

  def osmElemType: OsmElemType

  def xmlUrl = osmElemType.xmlUrl(id)
}

case class OsmNode(id: Long, gp: GeoPoint, visible: Boolean = true) extends OsmObject {
  override def osmElemType = OsmElemTypes.NODE
}

case class OsmWayNd(ref: Long)
case class OsmWayParsed(id: Long, nodeRefsOrdered: List[OsmWayNd]) extends OsmObject {
  override def osmElemType = OsmElemTypes.WAY

  def toWay(nodesMap: collection.Map[Long, OsmNode]): OsmWay = {
    OsmWay(
      id = id,
      nodesOrdered = nodeRefsOrdered.map { nd => nodesMap(nd.ref) }
    )
  }
}

case class OsmWay(id: Long, nodesOrdered: List[OsmNode]) extends OsmObject {
  override def osmElemType = OsmElemTypes.WAY
}


object RelMemberRoles extends Enumeration {
  type OsmRelMemberRole = Value
  // Нужен upper case
  val INNER, OUTER = Value : OsmRelMemberRole
}

object OsmElemTypes extends Enumeration {
  protected abstract class Val(val name: String) extends super.Val(name) {
    def xmlUrl(id: Long): String
  }

  type OsmElemType = Val
  // Нужен upper case

  val NODE: OsmElemType = new Val("node") {
    override def xmlUrl(id: Long) = s"http://www.openstreetmap.org/api/0.6/node/$id"
  }

  val WAY: OsmElemType = new Val("way") {
    override def xmlUrl(id: Long) = s"http://www.openstreetmap.org/api/0.6/way/$id/full"
  }

  val RELATION: OsmElemType = new Val("relation") {
    override def xmlUrl(id: Long) = s"http://www.openstreetmap.org/api/0.6/relation/$id/full"
  }

  implicit def value2val(x: Value): OsmElemType = x.asInstanceOf[OsmElemType]
}

import RelMemberRoles.OsmRelMemberRole


case class OsmRelMemberParsed(ref: Long, typ: OsmElemType, role: Option[OsmRelMemberRole])
case class OsmRelationParsed(id: Long, memberRefs: List[OsmRelMemberParsed]) {
  def toRelation(nodesMap: collection.Map[Long, OsmNode],
                 waysMap: collection.Map[Long, OsmWay],
                 relsMap: collection.Map[Long, OsmRelation]): OsmRelation = {
    OsmRelation(
      id = id,
      members = memberRefs.map { mr =>
        mr.typ match {
          case OsmElemTypes.NODE      => nodesMap(mr.ref)
          case OsmElemTypes.WAY       => waysMap(mr.ref)
          case OsmElemTypes.RELATION  => relsMap(mr.ref)
        }
      }
    )
  }

  def membersOfType(typ: OsmElemType) = memberRefs.iterator.filter(_.typ == typ)
}


case class OsmRelation(id: Long, members: List[OsmObject]) extends OsmObject {
  override def osmElemType = OsmElemTypes.RELATION
}


