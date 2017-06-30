package util.geo.osm

import javax.inject.Inject

import io.suggest.ahc.util.HttpGetToFile
import play.api.libs.ws.WSResponse
import util.geo.osm.OsmElemTypes.OsmElemType

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.09.14 15:54
 * Description: Статический клиент для доступа к OSM API. Для парсинга используется парсеры для xml api.
 */
class OsmClient @Inject() (
                            httpGetToFile           : HttpGetToFile,
                            implicit private val ec : ExecutionContext
                          ) {

  /**
   * Отфетчить элемент из инета, распарсить и вернуть результат.
   * Для фетчинга используется WS api, отправка данных во временный файл и парсинг из файла.
   * @param typ Тип запрашиваемого osm-объекта.
   * @param id Числовой id запрашиваемого osm-объекта.
   * @return Полученный и распарсенный osm-объект.
   */
  def fetchElement(typ: OsmElemType, id: Long): Future[OsmObject] = {
    val fetcher = new httpGetToFile.AbstractDownloader {
      override def urlStr = typ.xmlUrl(id)
      override def followRedirects = false
      override def statusCodeInvalidException(resp: WSResponse): Exception = {
        OsmClientStatusCodeInvalidException(resp.status)
      }
    }
    fetcher.request()
      .map { dlResp => //case (headers, file) =>
        import dlResp.file
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
