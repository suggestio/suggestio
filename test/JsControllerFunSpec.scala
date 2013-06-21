
import controllers.routes
import org.specs2.mutable._

import _root_.util.DomainQi.qiIdLen
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.06.13 17:13
 * Description:
 */
class JsControllerFunSpec extends Specification {

  "Js controller " should {

    val testSiteUrl = "http://test0.sio.cbca.ru"

    "handle requests of installing new site" in {
      running(FakeApplication()) {
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

        // TODO Отправить скрипт на тестовый сайт через ws api.
        // TODO Пройти процедуру валидации.
        // TODO Очистить тестовый сайт.
      }
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
