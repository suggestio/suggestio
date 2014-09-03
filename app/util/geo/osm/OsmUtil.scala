package util.geo.osm

import java.io.{FileInputStream, File, InputStream}
import OsmElemTypes.OsmElemType
import io.suggest.model.geo.GeoPoint
import org.xml.sax.SAXParseException
import scala.annotation.tailrec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.09.14 12:03
 * Description: Вспомогательная утиль для работы с osm.
 */

object OsmUtil {

  /** Решение проблемы компиляции списка relation'ов, когда одни relation'ы включают в себя другие.
    * @param nodesMap Карта точек.
    * @param waysMap Карта путей.
    * @param relps Исходный набор распарсенных relation'ов.
    * @return Карта relation'ов.
    */
  @tailrec
  def compileRelationsParsed(nodesMap: collection.Map[Long, OsmNode], waysMap: collection.Map[Long, OsmWay],
                             relps: List[OsmRelationParsed], acc: Map[Long, OsmRelation] = Map.empty): Map[Long, OsmRelation] = {
    if (relps.isEmpty) {
      acc
    } else {
      // Попытаться скомпилить оставшиеся relation'ы с учётом текущего аккамулятора.
      val (relpsFailed, newRels) = relps.foldLeft(List.empty[OsmRelationParsed] -> acc) {
        case ((accFail, relsOkMap), relp) =>
          try {
            val rel = relp.toRelation(nodesMap, waysMap, relsOkMap)
            accFail -> (relsOkMap + (rel.id -> rel))
          } catch {
            case ex: NoSuchElementException =>
              (relp :: accFail) -> relsOkMap
          }
      }
      compileRelationsParsed(nodesMap, waysMap, relpsFailed, newRels)
    }
  }

  def parseElementFromFile(f: File, typ: OsmElemType, id: Long): OsmObject = {
    val is = new FileInputStream(f)
    try {
      parseElementFromStream(is, typ, id)
    } finally {
      is.close()
    }
  }

  /**
   * Синхронный поточный парсер osm.xml osm-элемента.
   * @param is Входной поток.
   * @param typ Тип искомого элемента.
   * @param id id искомого элемента.
   * @return Найденный osm object.
   */
  def parseElementFromStream(is: InputStream, typ: OsmElemType, id: Long): OsmObject = {
    val parser = xml.ElementsParser.getSaxFactory.newSAXParser()
    val handler = new xml.ElementsParser
    try {
      parser.parse(is, handler)
    } catch {
      case sex: SAXParseException =>
        throw sex
      case ex: Exception =>
        val l = handler.locator
        throw new SAXParseException(s"Parsing failed at (${l.getLineNumber}, ${l.getColumnNumber})", handler.locator, ex)
    }
    // Распарсенные элементы нужно объеденить в нормальные структуры.
    if (typ == OsmElemTypes.NODE) {
      handler.getNodes.head
    } else {
      val nodesMap = handler.getNodesMap
      if (typ == OsmElemTypes.WAY) {
        handler.getWays.head.toWay(nodesMap)
      } else {
        assert(typ == OsmElemTypes.RELATION, "Unexpected osm element type: " + typ)
        val waysMap = handler.getWaysRev
          .iterator
          .map { wayp => wayp.id -> wayp.toWay(nodesMap) }
          .toMap
        val rels = compileRelationsParsed(nodesMap, waysMap, handler.getRelations)
        rels(id)
      }
    }
  }

}


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
  protected case class Val(name: String, isRelationBorder: Boolean) extends super.Val(name)

  type RelMemberRole = Val

  // Нужен upper case
  val INNER: RelMemberRole         = Val("inner", true)
  val OUTER: RelMemberRole         = Val("outer", true)
  val ADMIN_CENTRE: RelMemberRole  = Val("admin_centre", false)
  val LABEL: RelMemberRole         = Val("label", false)

  implicit def value2val(x: Value): RelMemberRole = x.asInstanceOf[RelMemberRole]

  def maybeWithName(x: String): Option[RelMemberRole] = {
    try {
      Some(withName(x))
    } catch {
      case ex: NoSuchElementException => None
    }
  }

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

import RelMemberRoles.RelMemberRole


case class OsmRelMemberParsed(ref: Long, typ: OsmElemType, role: RelMemberRole)
case class OsmRelationParsed(id: Long, memberRefs: List[OsmRelMemberParsed]) {
  def toRelation(nodesMap: collection.Map[Long, OsmNode],
                 waysMap: collection.Map[Long, OsmWay],
                 relsMap: collection.Map[Long, OsmRelation]): OsmRelation = {
    OsmRelation(
      id = id,
      members = memberRefs.map { mr =>
        val m = mr.typ match {
          case OsmElemTypes.NODE      => nodesMap(mr.ref)
          case OsmElemTypes.WAY       => waysMap(mr.ref)
          case OsmElemTypes.RELATION  => relsMap(mr.ref)
        }
        OsmRelMember(mr.typ, m, mr.role)
      }
    )
  }

  def membersOfType(typ: OsmElemType) = memberRefs.iterator.filter(_.typ == typ)
}

case class OsmRelMember(typ: OsmElemType, obj: OsmObject, role: RelMemberRole)

case class OsmRelation(id: Long, members: List[OsmRelMember]) extends OsmObject {
  override def osmElemType = OsmElemTypes.RELATION
  
  def borderMembers = members.iterator.filter(_.role.isRelationBorder)
}


