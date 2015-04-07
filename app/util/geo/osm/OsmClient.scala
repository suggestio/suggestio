package util.geo.osm

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import util.geo.osm.OsmElemTypes.OsmElemType
import util.ws.HttpGetToFile
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.09.14 15:54
 * Description: Статический клиент для доступа к OSM API. Для парсинга используется парсеры для xml api.
 */
object OsmClient {

  /**
   * Отфетчить элемент из инета, распарсить и вернуть результат.
   * Для фетчинга используется WS api, отправка данных во временный файл и парсинг из файла.
   * @param typ Тип запрашиваемого osm-объекта.
   * @param id Числовой id запрашиваемого osm-объекта.
   * @return Полученный и распарсенный osm-объект.
   */
  def fetchElement(typ: OsmElemType, id: Long)(implicit ws1: WSClient): Future[OsmObject] = {
    val fetcher = new HttpGetToFile {
      override def ws = ws1
      override def urlStr = typ.xmlUrl(id)
      override def followRedirects = false
      override def statusCodeInvalidException(resp: WSResponseHeaders) = {
        OsmClientStatusCodeInvalidException(resp.status)
      }
    }
    fetcher.request()
      .map { case (headers, file) =>
        try {
          OsmUtil.parseElementFromFile(file, typ, id)
        } finally {
          file.delete()
        }
      }
  }

}


case class OsmClientStatusCodeInvalidException(statusCode: Int)
  extends RuntimeException("Unexpected status code: " + statusCode)
