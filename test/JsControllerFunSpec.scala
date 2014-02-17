
import controllers.routes
import org.specs2.mutable._

import _root_.util.DomainQi.qiIdLen
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global // TODO Почему play EC не подходит сюда, принимается только штатный EC.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.06.13 17:13
 * Description: Тесты для проверки валидации сайта. Имитируется взятие кода suggest.io и установка его на тестовый сайт.
 */
class JsControllerFunSpec extends Specification {

  "Js controller " should {

    val testSite = "test0.sio.cbca.ru"
    val testSiteUrl = "http://" + testSite
    val testSetCodeUrl  = testSiteUrl + "/common/set_code"

    "handle valid installation via qi" in new WithServer {

      val selfUrlPrefix = "http://localhost:" + port
      // TODO fake-запросы надо переписать через WS.url.get с использованием вышеуказанного профикса, иначе сессии не пашут.

      val addDomainResult = controllers.Js.addDomain()(FakeRequest().withFormUrlEncodedBody("url" -> testSiteUrl))
      // Проверяем возвращаемый статус. Если ошибка, то напечатать в консоли тело ошибки для облегчения исправления ошибки.
      status(addDomainResult) match {
        case code if code != 200 =>
          println(contentAsString(addDomainResult))
          code must beEqualTo(200)

        case _ =>
      }
      val addDomainJsonJsString = Json.parse(contentAsBytes(addDomainResult))
      addDomainJsonJsString must beAnInstanceOf[JsObject]
      val addDomainJson = addDomainJsonJsString.asInstanceOf[JsObject]

      // Извлекаем ключ домена и проверяем
      val dkey = (addDomainJson \ "domain").asInstanceOf[JsString].value
      dkey must beEqualTo("test0.sio.cbca.ru")

      // Извлекаем ссылку на js с qi_id и слегка проверяем по регэкспу
      val jsUrl = (addDomainJson \ "js_url").asInstanceOf[JsString].value
      val jsUrlRegex = routes.Js.v2(dkey, "RRRR").url.replace("RRRR", "[\\w]{" + qiIdLen + "}").r
      jsUrlRegex.pattern.matcher(jsUrl).matches() must beTrue

      // Генерим приблизительный скрипт, который "увидит" детектор SioJs.
      val script = """<script type="text/javascript">
          (function() {
	          var _sw = document.createElement("script");
	          _sw.type = "text/javascript";
	          _sw.async = true;_sw.src = "https://suggest.io%s";var _sh = document.getElementsByTagName("head")[0]; _sh.appendChild(_sw);})();
        </script>""" format jsUrl

      // Отправить "полученный" скрипт на тестовый сайт (через WebServices API).
      val saveCodeBody = Map(
        "code"                -> Seq(script),
        "domain_id"           -> Seq("68"),
        "is_show_charset"     -> Seq("on"),
        "validation_content"  -> Seq(""),
        "validation_filename" -> Seq("")
      )
      val saveCodeFut = WS.url(testSetCodeUrl).post(saveCodeBody)
      saveCodeFut.map(_.status) must beEqualTo(200)

      // Код установлен на сайт. Ход установки можно наблюдать через SioNotifier (или через WebSocket на клиенте).


      // Теперь надо сымитировать запрос к suggest.io к /js/v2/....
      val jsReqResultOpt = route(FakeRequest(GET, jsUrl).withSession(
        session(addDomainResult).data.toList : _*))
      jsReqResultOpt must beSome

      val jsReqResult = jsReqResultOpt.get
      status(jsReqResult) must beEqualTo(200)
      contentAsString(jsReqResult) must contain("install")

      // Тут пошла асинхронная проверка сайта на наличие скрипта. Результаты накапливаются в очереди новостей и придут клиенту по web socket.
      // TODO Пройти процедуру валидации. Тут нужен вебсокет видимо... А значит и работать надо через htmlunit и browser.
      // TODO Очистить тестовый сайт.
    }


    // TODO закоменчено и не работает, потому что шаблоны кривые.
    /*"browser: install new site via Qi via main-site-page" in {
      running(TestServer(3333), HTMLUNIT) { browser =>

        // Зайти на тестовый сервер, убедится, что там не установлен скрипт sio
        browser.goTo(testSiteUrl)
        val codeAreaSelector = "textarea[name='code']"
        browser.$(codeAreaSelector).getAttribute("style").length must beGreaterThan(0)

        // Убедится, что там пустой скрипт сейчас и засабмиттить.
        browser.$(codeAreaSelector).clear()
        browser.$(codeAreaSelector).getText must beEmpty
        browser.$("input[type='submit']").click()
        browser.$(codeAreaSelector).getText must beEmpty

        // переходим на suggest.io, добавляем сайт.
        browser.goTo("http://localhost:3333/")                  // Зайти на главную suggest.io
        browser.$("#userSiteInput").text(testSiteUrl)           // Вдолбить туда сайт
        browser.$(".start-button").click()                      // Подтвердить.

        // Дождаться появления выдачи кода на странице.
        browser.waitUntil {
          !browser.$("#jsCodeTextarea").getText.isEmpty
        }
        val jsCode = browser.$("#jsCodeTextarea").getText
        jsCode must startWith("<script")

        // TODO нужно пофиксить главную, чтоб всё отображалось как надо.

        // TODO вставить код на исходный сайт, перейти на сайт, завершить проверку.
      }
    }*/
  }

}
