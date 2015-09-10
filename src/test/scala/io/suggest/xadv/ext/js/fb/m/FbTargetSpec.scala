package io.suggest.xadv.ext.js.fb.m

import minitest._
import FbNodeTypes._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.03.15 18:12
 * Description: Тесты для модели FbTarget.
 */
object FbTargetSpec extends SimpleTestSuite {

  import FbTarget._

  /** Привести путь к абсолютное ссылке на фейсбук, если требуется. */
  private def path2url(path: String): String = {
    if (path startsWith "http") {
      path
    } else {
      val path1 = if (path startsWith "/") {
        path
      } else {
        "/" + path
      }
      "https://facebook.com" + path1
    }
  }

  /** Запуск теста. */
  private def t(path: String, id: String, nodeType: Option[FbNodeType] = null): Unit = {
    val url = path2url(path)
    assertEquals(
      fromUrl(url).map(_.nodeId),
      Some(id)
    )
    if (nodeType != null) {
      assertEquals(
        fromUrl(url).flatMap(_.nodeType),
        nodeType
      )
    }
  }


  // Профили
  test("Profile URL: /profile.php?id=100007720320498") {
    t("https://www.facebook.com/profile.php?id=100007720320498",    "100007720320498")
  }

  test("Profile URL: /profile.php?a=b&id=123123&c=123123") {
    t("https://www.facebook.com/profile.php?a=b&id=100007720320498&c=123123123#asd",    "100007720320498")
  }


  // Группы
  test("Group URL: /groups/570250649777627 with common url garbage") {
    t("https://www.facebook.com/groups/570250649777627/?pnref=lhc",  "570250649777627")
  }

  test("Group URL: /groups/570250649777627") {
    t("https://www.facebook.com/groups/570250649777627",  "570250649777627")
  }


  // События
  test("Event URL: /events/1611299965755692 with common URL garbage") {
    t("https://www.facebook.com/events/1611299965755692/?ref=90&ref_dashboard_filter=upcoming&unit_ref=popular_nearby#", "1611299965755692")
  }

  test("Event URL: /events/1611299965755692") {
    t("https://www.facebook.com/events/1611299965755692", "1611299965755692")
    t("https://www.facebook.com/events/1611299965755692/", "1611299965755692")
  }


  // Страницы
  test("Page URL: /pages/nrsecrets/227818087231518") {
    t("https://www.facebook.com/pages/nrsecrets/227818087231518", "227818087231518")
  }

  // Эта русскоязычная ссылка реальная, взята из /pages
  test("Page %encoded URL: https://www.facebook.com/pages/%D0%9C%D1%8E%D0%B7%D0%B8%D0%BA%D0%BB-%D0%97%D0%BE%D0%BC%D0%B1%D0%B8-%D0%97%D0%BE%D0%BC%D0%B1%D0%B8-%D0%97%D0%BE%D0%BC%D0%B1%D0%B8/1622992914594739") {
    t(
      "https://www.facebook.com/pages/%D0%9C%D1%8E%D0%B7%D0%B8%D0%BA%D0%BB-%D0%97%D0%BE%D0%BC%D0%B1%D0%B8-%D0%97%D0%BE%D0%BC%D0%B1%D0%B8-%D0%97%D0%BE%D0%BC%D0%B1%D0%B8/1622992914594739",
      "1622992914594739",
      Some(Page)
    )
  }

  test("Page %encoded URL: https://www.facebook.com/pages/Бар-Анка/1574446066106309") {
    t("https://www.facebook.com/pages/%D0%91%D0%B0%D1%80-%D0%90%D0%BD%D0%BA%D0%B0/1574446066106309", "1574446066106309")
  }

  test("Page 2015 URL scheme raw (no %-encoding): Бар-Анка") {
    t("https://www.facebook.com/Бар-Анка-1574446066106309/timeline/", "1574446066106309", Some(Page))
  }
  test("Page 2015 URL scheme raw (no %-encoding): LOL") {
    t("https://www.facebook.com/LOL-1574446066106309/timeline/", "1574446066106309", Some(Page))
  }

  test("Page 2015 URL scheme with %-encoding") {
    t("https://www.facebook.com/%D0%91%D0%B0%D1%80-%D0%90%D0%BD%D0%BA%D0%B0-1574446066106309/timeline/",
      "1574446066106309", Some(Page))
  }


  // URL-неймы, т.е. ссылка в корне.
  test("Named URL (simple): /hellocbca?ref=hl") {
    t("https://www.facebook.com/hellocbca?ref=hl", "hellocbca")
    t("https://www.facebook.com/hellocbca", "hellocbca")
  }

  test("Named URL (complex): https://www.facebook.com/sio.search?ref=hl") {
    t("https://www.facebook.com/sio.search?ref=hl", "sio.search")
    t("https://www.facebook.com/sio.search", "sio.search")
  }

}
