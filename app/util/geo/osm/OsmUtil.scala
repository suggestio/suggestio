package util.geo.osm

import java.io.{FileInputStream, File, InputStream}
import OsmElemTypes.OsmElemType
import io.suggest.model.geo._
import org.xml.sax.SAXParseException
import util.PlayLazyMacroLogsImpl
import scala.annotation.tailrec
import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.09.14 12:03
 * Description: Вспомогательная утиль для работы с osm.
 */

object OsmUtil extends PlayLazyMacroLogsImpl {

  import LOGGER._

  /** Добавлять ли subarea-relation'ы в путь relation'а? */
  val RELATION_ADD_SUBAREAS: Boolean = false

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
      val restRelsCount = relps.size
      if (restRelsCount == relpsFailed.size) {
        warn(s"compileRelationsParsed(): Cannot compile $restRelsCount relations: " + relpsFailed.iterator.map(_.id).mkString(", "))
        newRels
      } else {
        compileRelationsParsed(nodesMap, waysMap, relpsFailed, newRels)
      }
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
        val ex2 = handler.locator match {
          case null => new SAXParseException("Parsing failed before document locator has been set", "", "", 0, 0, ex)
          case l    => new SAXParseException(s"Parsing failed at (${l.getLineNumber}, ${l.getColumnNumber})", l, ex)
        }
        throw ex2
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
        val relsRaw = if (RELATION_ADD_SUBAREAS) {
          handler.getRelations
        } else {
          handler.getRelations.filter(_.id == id)
        }
        val rels = compileRelationsParsed(nodesMap, waysMap, relsRaw)
        rels(id)
      }
    }
  }


  /** Пути могут быть иметь обратный порядок узлов внутри себя. Надо стыковать текущую последнюю точку со следующей
    * первой точкой путем реверса точек при необходимости. */
  def connectWays(ways: Iterator[OsmWay]): List[List[OsmNode]] = {
    ways.foldLeft ( List(ways.next().nodesOrdered) ) { (waysAcc, way) =>
      val nodesAcc = waysAcc.head
      lazy val wayLast = way.nodesOrdered.last
      lazy val nodesAccLast = nodesAcc.last
      if (way.nodesOrdered.head == nodesAcc.head) {
        // нужно добавить все элементы слева в акк в обратном порядке
        val w2 = way.nodesOrdered.tail.foldLeft(nodesAcc) { (_acc, node) => node :: _acc }
        w2 :: waysAcc.tail
      } else if (wayLast == nodesAcc.head) {
        // Нужно добавить все элементы слева в акк в прямом порядке
        val w2 = way.nodesOrdered.foldRight(nodesAcc.tail) { (node, _acc) => node :: _acc }
        w2 :: waysAcc.tail
      } else if (wayLast == nodesAccLast) {
        // Нужно добавить все элементы в хвост акку в обратном порядке
        val w2 = nodesAcc ++ way.nodesOrdered.reverse.tail
        w2 :: waysAcc.tail
      } else if (way.nodesOrdered.head == nodesAccLast) {
        // нужно добавить все элемены в хвост акку в прямом порядке
        val w2 = nodesAcc ++ way.nodesOrdered.tail
        w2 :: waysAcc.tail
      } else if (nodesAccLast == nodesAcc.head) {
        // Текущий контур уже замкнут, а точки продолжаются. Значит это отдельный островок. Стартуем новый контур в аккамуляторе.
        trace("connectWays(): Starting new way in acc...")
        way.nodesOrdered :: waysAcc
      } else {
        throw new IllegalArgumentException(s"directWays(): Cannot connect way to nodes acc: no common points found:\n - way = $way\n - acc = $nodesAcc")
      }
    }
  }

}


/** Утиль для разбора ссылок на osm-барахло. */
trait OsmParsersT extends JavaTokenParsers {

  // browser-ссылки -- это ссылки вида:
  // http://www.openstreetmap.org/relation/368287#map=11/59.8721/30.4651
  // http://www.openstreetmap.org/way/31399147#map=18/59.93284/30.25950

  def osmSitePrefixP = "(?i)https?".r ~> "://" ~> opt("(?i)www.".r) ~> "(?i)openstreetmap.org/".r

  def osmTypeP: Parser[OsmElemType] = {
    ("(?i)way".r | "(?i)node".r | "(?i)relation".r) ^^ { OsmElemTypes.withName }
  }

  def objIdP: Parser[Long] = "\\d+".r ^^ { _.toLong }

  def typeIdPartP: Parser[OsmElemType ~ Long] = osmTypeP ~ ("/" ~> objIdP)

  /** Парсер строки типа [[http://www.openstreetmap.org/way/31399147]]. */
  def osmBrowserUrlP: Parser[OsmElemType ~ Long] = {
    osmSitePrefixP ~> typeIdPartP
  }

  /** Парсер API-0.6-ссылки вида [[http://www.openstreetmap.org/api/0.6/way/31399147/full]]. */
  def osmApi06UrlP: Parser[OsmElemType ~ Long] = {
    osmSitePrefixP ~> "(?i)api/0\\.6/".r ~> typeIdPartP
  }

  def osmBrowserUrl2TypeIdP: Parser[(OsmElemType, Long)] = {
    (osmBrowserUrlP | osmApi06UrlP) ^^ {
      case typ ~ id  =>  (typ, id)
    }
  }

}
class OsmParsers extends OsmParsersT


trait OsmObject {
  def id: Long

  def osmElemType: OsmElemType

  def xmlUrl = osmElemType.xmlUrl(id)

  def toGeoShape: GeoShape

  def firstNode: OsmNode
  def lastNode: OsmNode
}

case class OsmNode(id: Long, gp: GeoPoint, visible: Boolean = true) extends OsmObject {
  override def osmElemType = OsmElemTypes.NODE
  override def toGeoShape = PointGs(gp)
  override def firstNode: OsmNode = this
  override def lastNode: OsmNode = this
}

case class OsmWayNd(ref: Long)
case class OsmWayParsed(id: Long, nodeRefsOrdered: List[OsmWayNd]) {

  def toWay(nodesMap: collection.Map[Long, OsmNode]): OsmWay = {
    OsmWay(
      id = id,
      nodesOrdered = nodeRefsOrdered.map { nd => nodesMap(nd.ref) }
    )
  }

}

case class OsmWay(id: Long, nodesOrdered: List[OsmNode]) extends OsmObject {
  override def osmElemType = OsmElemTypes.WAY

  override def firstNode = nodesOrdered.head
  override def lastNode = nodesOrdered.last

  override def toGeoShape: GeoShape = {
    // для замкнутых путей используем полигон, для разомкнутых - просто линию.
    val line = LineStringGs(nodesOrdered.map(_.gp))
    if (nodesOrdered.head == nodesOrdered.last && nodesOrdered.tail.nonEmpty) {
      // Путь замкнут -- заворачиваем в полигон.
      PolygonGs(line)
    } else {
      line
    }
  }
}


object RelMemberRoles extends Enumeration {
  protected case class Val(name: String, isRelationBorder: Boolean) extends super.Val(name)

  type RelMemberRole = Val

  // Нужен upper case
  val INNER: RelMemberRole         = Val("inner", true)
  val OUTER: RelMemberRole         = Val("outer", true)
  val ADMIN_CENTRE: RelMemberRole  = Val("admin_centre", false)
  val LABEL: RelMemberRole         = Val("label", false)
  val SUBAREA: RelMemberRole       = Val("subarea", false)

  implicit def value2val(x: Value): RelMemberRole = x.asInstanceOf[RelMemberRole]

  def default = OUTER

  def maybeWithName(x: String): Option[RelMemberRole] = {
    try {
      val r = x match {
        case ""         => default
        case "enclave"  => INNER
        case "exclave"  => OUTER
        case other      => withName(other)
      }
      Some(r)
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
      members = memberRefs.flatMap { mr =>
        val m = mr.typ match {
          case OsmElemTypes.NODE      => nodesMap(mr.ref)
          case OsmElemTypes.WAY       => waysMap(mr.ref)
          case OsmElemTypes.RELATION if OsmUtil.RELATION_ADD_SUBAREAS =>
            relsMap(mr.ref)
          case OsmElemTypes.RELATION => null
        }
        Option(m) map {
          OsmRelMember(mr.typ, _, mr.role)
        }
      }
    )
  }

  def membersOfType(typ: OsmElemType) = memberRefs.iterator.filter(_.typ == typ)
}

case class OsmRelMember(typ: OsmElemType, obj: OsmObject, role: RelMemberRole)


case class OsmRelation(id: Long, members: List[OsmRelMember]) extends OsmObject {

  override def osmElemType = OsmElemTypes.RELATION

  def filterByRole(role: RelMemberRole) = members.iterator.filter(_.role == role)
  
  def borderMembers = members.iterator.filter(_.role.isRelationBorder)

  def subareas = filterByRole(RelMemberRoles.SUBAREA)
  def inners = filterByRole(RelMemberRoles.INNER)
  def outers = filterByRole(RelMemberRoles.OUTER)

  def ways(memberIter: Iterator[OsmRelMember]) = {
    memberIter.flatMap { mmbr =>
      if (mmbr.typ == OsmElemTypes.WAY)
        Seq(mmbr.obj.asInstanceOf[OsmWay])
      else
        Seq()
    }
  }

  def outerWays = ways(outers)
  def innerWays = ways(inners)

  /** Сгрупировать inner-пути в дырки, состоящие из путей. */
  def innerHoles = {
    innerWays
      .foldLeft [List[List[OsmWay]]] (List(Nil)) { (acc, e) =>
        val hole = e :: acc.head
        val acc1 = hole :: acc.tail
        if ( acc.head.last.firstNode == e.lastNode ) {
          // конец текущего пути совпадает с началом подпути
          Nil :: acc1
        } else {
          // Нет замкнутой кривой. Добавляем элемент к текущему набору
          acc1
        }
      }
      .iterator
      .filter(_.nonEmpty)
  }

  def firstOuter: OsmNode = outers.next().obj.firstNode
  def lastOuter: OsmNode = {
    outers
      .reduceLeft[OsmRelMember] { (old, next) => next }
      .obj
      .lastNode
  }

  override def firstNode: OsmNode = firstOuter
  override def lastNode: OsmNode = lastOuter

  def hasOuters = outers.hasNext
  def hasInners = inners.hasNext
  def hasSubareas = subareas.hasNext

  override def toGeoShape: GeoShape = {
    // Тут рендерятся линии, мультиполигоны и полигоны. Сначала рендерим полигон, описанный в inners/outers
    val acc0: List[GeoShape] = if (hasOuters) {
      val outerLineWays = OsmUtil.connectWays(outerWays)
      if (outerLineWays.tail.isEmpty) {
        // Одна линия. Если это полигон, то можно вырезанть в нём дырки.
        val hd = outerLineWays.head
        val line = LineStringGs(hd.map(_.gp))
        val isOuterClosed = hd.head == hd.last && hd.tail.nonEmpty
        val e = if (isOuterClosed) {
          val holes = innerHoles
            .flatMap { wayGroup =>
            OsmUtil.connectWays(wayGroup.iterator)
              .map { w => LineStringGs(w.map(_.gp))}
          }
            .toList
          PolygonGs(line, holes)
        } else {
          // Линия не замкнутая, например дорога. Inner holes тут нет.
          line
        }
        List(e)
      } else {
        // Мультиполигон.
        val polys = outerLineWays
          .map { olw =>
            val ls = LineStringGs(olw.map(_.gp))
            // TODO Нужно определять, какие дырки относятся к текущему полигону.
            PolygonGs(ls)
          }
        List( MultiPolygonGs(polys) )
      }
    } else {
      Nil
    }
    // Добавляем все subarea в кучу? Обычно, это не нужно.
    val acc2 = if (OsmUtil.RELATION_ADD_SUBAREAS) {
      subareas.foldLeft(acc0) { (acc, subarea) =>
        subarea.obj.toGeoShape :: acc
      }
    } else {
      acc0
    }
    // Заворачиваем в финальный гео-объект контейнер.
    if (acc2.tail.isEmpty) {
      acc2.head
    } else {
      GeometryCollectionGs(acc2)
    }
  }

}


