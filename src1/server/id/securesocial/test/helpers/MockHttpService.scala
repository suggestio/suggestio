package helpers

import org.specs2.mock.Mockito
import play.api.http.{ ContentTypeOf, Writeable, HeaderNames }
import play.api.libs.ws._
import play.api.http.Status._
import scala.concurrent.Future
import play.api.libs.ws.WSRequest
import securesocial.core.services.HttpService

object MockHttpService {
  type Params = Map[String, Seq[String]]
  type ParamsWriter = Writeable[Params]
  type ContentTypeOfParams = ContentTypeOf[Params]
}
class MockHttpService extends Mockito with HttpService {

  val request = mock[WSRequest].as(s"Request($hashCode)")
  val response = mock[WSResponse].as(s"Response($hashCode)")

  val urls: collection.mutable.Buffer[String] = new collection.mutable.ArrayBuffer[String]()

  response.status returns OK
  response.header(HeaderNames.CONTENT_TYPE) returns Some("text/html;charset=UTF-8")
  response.body returns ""

  request.get() returns Future.successful(response)
  //request.post(anyString)(any[Writeable[String]], any[ContentTypeOf[String]]) returns Future.successful(response)

  def url(url: String): WSRequest = {
    urls += url
    request
  }

  def underlying[T]: T = this.asInstanceOf[T]
}
