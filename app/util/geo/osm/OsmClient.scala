package util.geo.osm

import java.io.ByteArrayInputStream
import javax.xml.parsers.SAXParserFactory

import com.ning.http.client.{AsyncHttpClientConfig, AsyncHttpClient}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import util.geo.osm.OsmElemTypes.OsmElemType
import play.api.Play.current
import dispatch._

import scala.annotation.tailrec
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.09.14 15:54
 * Description: Статический клиент для доступа к OSM API. Для парсинга используется парсеры для xml api.
 */
object OsmClient {

  def getHttpClient = {
    val builder = new AsyncHttpClientConfig.Builder()
      .setCompressionEnabled(true)
      .setRequestTimeoutInMs(10000)
      .setConnectionTimeoutInMs(3000)
      .setAllowSslConnectionPool(true)
      .setAllowPoolingConnection(true)
      .setFollowRedirects(false)
      .setIdleConnectionTimeoutInMs(10000)
      .setMaximumConnectionsPerHost(1)
    Http(new AsyncHttpClient(builder.build))
  }

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

  def fetchElement(typ: OsmElemType, id: Long, client: Http): Future[OsmObject] = {
    val urlStr = typ.xmlUrl(id)
    // TODO Нужно какой-то асинхронный xml-parser замутить. Можно aalto прикрутить и переписывать sax-парсер на stax.
    // TODO Для снижения нагрузки на RAM лучше сохранять это дело во временный файл, и из файла уже парсить.
    client(url(urlStr) OK as.Bytes) map { bytes =>
      val parser = xml.ElementsParser.getSaxFactory.newSAXParser()
      val handler = new xml.ElementsParser
      val bais = new ByteArrayInputStream(bytes)
      parser.parse(bais, handler)
      // Распарсенные элементы нужно объеденить в нормальные структуры.
      if (typ == OsmElemTypes.NODE) {
        handler.getNodes.head
      } else {
        val nodesMap = handler.getNodesMap
        if (typ == OsmElemTypes.WAY) {
          handler.getWays.head.toWay(nodesMap)
        } else if (typ == OsmElemTypes.RELATION) {
          val waysMap = handler.getWaysRev
            .iterator
            .map { wayp => wayp.id -> wayp.toWay(nodesMap) }
            .toMap
          val rels = compileRelationsParsed(nodesMap, waysMap, handler.getRelations)
          rels(id)
        } else {
          ???
        }
      }
    }
  }

}
