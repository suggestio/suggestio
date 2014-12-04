package util.geo.osm.xml

import io.suggest.model.geo.GeoPoint
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.{SAXParseException, Attributes}
import util.PlayLazyMacroLogsImpl
import util.ai.sax.StackFsmSax
import util.geo.osm._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.09.14 11:57
 * Description: Парсер для element-выхлопов osm xml api по разным объектам.
 * http://wiki.openstreetmap.org/wiki/API_v0.6#Elements_2
 */

/** Статические константы инклюдятся в трейт, т.к. за пределами редкоработающего инстанса парсера они не нужны. */
trait ElementParserConstants {

  val FORMAT_VSN      = "0.6"

  val TAG_OSM         = "osm"
  val TAG_NODE        = "node"
  val TAG_WAY         = "way"
  val TAG_RELATION    = "relation"
  val TAG_TAG         = "tag"
  val TAG_ND          = "nd"
  val TAG_MEMBER      = "member"

  val ATTR_VERSION    = "version"
  val ATTR_ID         = "id"
  val ATTR_LAT        = "lat"
  val ATTR_LON        = "lon"
  val ATTR_REF        = "ref"
  val ATTR_TYPE       = "type"
  val ATTR_ROLE       = "role"

}


/** Абстрактный парсер выхлопов OSM GET elements xml api. */
trait ElementsParserT
  extends DefaultHandler
  with StackFsmSax
  with PlayLazyMacroLogsImpl
  with ElementParserConstants
{

  protected var nodesAccRev: List[OsmNode] = Nil
  protected var waysAccRev: List[OsmWayParsed] = Nil
  protected var relationsAccRev: List[OsmRelationParsed] = Nil

    /** Начало работы с документом. Нужно выставить дефолтовый обработчик в стек. */
  override def startDocument() {
    become(new TopLevelHandler)
  }

  /** Получить карту точек. */
  def getNodesMap: Map[Long, OsmNode] = {
    nodesAccRev
      .iterator
      .map { node => node.id -> node }
      .toMap
  }
  def getNodes = nodesAccRev.reverse
  def getNodesRev = nodesAccRev

  /** Добавить ноду в аккамулятор. */
  def addNode(node: OsmNode): Unit = {
    nodesAccRev ::= node
  }

  def getWays = waysAccRev
  def getWaysRev = waysAccRev.reverse

  def addWay(way: OsmWayParsed): Unit = {
    waysAccRev ::= way
  }

  def addRelation(rel: OsmRelationParsed): Unit = {
    relationsAccRev ::= rel
  }
  def getRelationsRev = relationsAccRev
  def getRelations = relationsAccRev.reverse


  trait ElementTagHandler extends TagHandler {
    val getId = thisTagAttrs.getValue(ATTR_ID).toLong
  }

  /** Самый начальный обработчик докумена. С него начинается парсинг. Тут ожидается тэг osm.  */
  class TopLevelHandler extends TopLevelHandlerT {
    override def startTag(tagName: String, attributes: Attributes) {
      if (tagName == TAG_OSM) {
        become(new OsmTagHandler(attributes))
      } else {
        val ex = new SAXParseException(s"Unexpected tag: '$tagName'. 'yml_catalog' expected.", locator)
        fatalError(ex)
        throw ex
      }
    }
  }


  /** Обработчик osm-тега, т.е. top-level тега, включающего в себя всю инфу ответа, версию xml-протокола и
    * прочие метаданные. */
  class OsmTagHandler(val thisTagAttrs: Attributes) extends TagHandler {
    override def thisTagName = TAG_OSM
    
    def apiVersion = Option( thisTagAttrs.getValue(ATTR_VERSION) )

    // Проверяем версию формата сразу в конструкторе.
    {
      val vsnOpt = apiVersion
      if (!vsnOpt.exists { _ equalsIgnoreCase FORMAT_VSN })
        fatalError(new SAXParseException(s"Invalid osm.xml version: $vsnOpt ; expected $FORMAT_VSN", locator))
    }

    override def startTag(tagName: String, attributes: Attributes): Unit = {
      super.startTag(tagName, attributes)
      val nextHandler: TagHandler = tagName match {
        case TAG_NODE       => new NodeTagHandler(attributes)
        case TAG_WAY        => new WayTagHandler(attributes)
        case TAG_RELATION   => new RelationTagHandler(attributes)
      }
      become(nextHandler)
    }
  }


  /** Обработчик тега node, описывающего точку на карте. */
  class NodeTagHandler(val thisTagAttrs: Attributes) extends ElementTagHandler {
    override def thisTagName = TAG_NODE

    // Вызываем getNode в конструкторе, т.к. аттрибуты будут уже не доступны во время endTag или иных действий.
    val getNode = {
      OsmNode(
        id = getId,
        gp = GeoPoint(
          lat = thisTagAttrs.getValue(ATTR_LAT).toDouble,
          lon = thisTagAttrs.getValue(ATTR_LON).toDouble
        )
      )
    }

    override def startTag(tagName: String, attributes: Attributes): Unit = {
      super.startTag(tagName, attributes)
      become( new DummyHandler(tagName, attributes) )
    }

    override def endTag(tagName: String): Unit = {
      addNode(getNode)
      super.endTag(tagName)
    }
  }


  /** Обработчик way-тегов, описывающих пути на карте. */
  class WayTagHandler(val thisTagAttrs: Attributes) extends ElementTagHandler {
    override def thisTagName = TAG_WAY

    /** Аккамулятор для списка точек пути (теги nd). */
    var ndAcc: List[OsmWayNd] = Nil

    override def startTag(tagName: String, attributes: Attributes): Unit = {
      super.startTag(tagName, attributes)
      val nextHandler = tagName match {
        case TAG_ND   => new NdTagHandler(attributes)
        case TAG_TAG  => new DummyHandler(tagName, attributes)
      }
      become(nextHandler)
    }

    override def endTag(tagName: String): Unit = {
      super.endTag(tagName)
      val way = OsmWayParsed(
        id = getId,
        nodeRefsOrdered = ndAcc.reverse
      )
      addWay(way)
    }

    /** Обработчик nd-тегов. */
    class NdTagHandler(val thisTagAttrs: Attributes) extends TagHandler {
      override def thisTagName = TAG_ND

      val getRef = thisTagAttrs.getValue(ATTR_REF).toLong

      override def startTag(tagName: String, attributes: Attributes): Unit = {
        super.startTag(tagName, attributes)
        become( new DummyHandler(tagName, attributes) )
      }

      override def endTag(tagName: String): Unit = {
        ndAcc ::= OsmWayNd(getRef)
        super.endTag(tagName)
      }
    }
  }
  

  /** Парсер тега relation, описывающий отношения на карте. */
  class RelationTagHandler(val thisTagAttrs: Attributes) extends ElementTagHandler {
    override def thisTagName = TAG_RELATION

    var membersAcc: List[OsmRelMemberParsed] = Nil

    override def startTag(tagName: String, attributes: Attributes): Unit = {
      super.startTag(tagName, attributes)
      val nextHandler = tagName match {
        case TAG_MEMBER => new MemberTagHandler(attributes)
        case _          => new DummyHandler(tagName, attributes)
      }
      become(nextHandler)
    }

    override def endTag(tagName: String): Unit = {
      val rel = OsmRelationParsed(
        id = getId,
        memberRefs = membersAcc.reverse
      )
      addRelation(rel)
      super.endTag(tagName)
    }

    class MemberTagHandler(val thisTagAttrs: Attributes) extends TagHandler {
      override def thisTagName = TAG_MEMBER

      /**
       * Собираем результат в конструкторе, пока доступны аттрибуты тега.
       * @see [[http://wiki.openstreetmap.org/wiki/Relation:boundary#Relation_members]]
       */
      val role = Option( thisTagAttrs.getValue(ATTR_ROLE) )
        .flatMap { RelMemberRoles.maybeWithName }
        .getOrElse(RelMemberRoles.default)

      membersAcc ::= OsmRelMemberParsed(
        ref = thisTagAttrs.getValue(ATTR_REF).toLong,
        typ = OsmElemTypes.withName( thisTagAttrs.getValue(ATTR_TYPE).toLowerCase ),
        role = role
      )

      override def startTag(tagName: String, attributes: Attributes): Unit = {
        super.startTag(tagName, attributes)
        become( new DummyHandler(tagName, attributes) )
      }

    }
  }

}


/** Дефолтовая реализация парсера геоэлементов osm-карты, описанных по xml api. */
class ElementsParser extends ElementsParserT

