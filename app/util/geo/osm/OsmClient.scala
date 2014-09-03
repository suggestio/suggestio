package util.geo.osm

import java.io._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Iteratee
import play.api.libs.ws._
import util.geo.osm.OsmElemTypes.OsmElemType
import play.api.Play.current
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
  def fetchElement(typ: OsmElemType, id: Long): Future[OsmObject] = {
    val urlStr = typ.xmlUrl(id)
    val respFut = WS.url(urlStr)
      .withFollowRedirects(false)
      .getStream()
    respFut.flatMap { case (headers, body) =>
      assert(headers.status == 200, s"Unexpected http status: ${headers.status}")
      val f = File.createTempFile(s"$typ.$id.", ".osm.xml")
      val os = new FileOutputStream(f)
      val iteratee = Iteratee.foreach[Array[Byte]] { bytes =>
        os.write(bytes)
      }
      // отправлять байты enumerator'а в iteratee, который будет их записывать в файл.
      (body |>>> iteratee)
        // Надо дождаться закрытия файла перед вызовом последующего map, который его откроет для чтения.
        .andThen {
          case result =>  os.close()
        }
        // Открыть файл и распарсить содержимое
        .map { _ =>
          OsmUtil.parseElementFromFile(f, typ, id)
        }
    }
  }

}
