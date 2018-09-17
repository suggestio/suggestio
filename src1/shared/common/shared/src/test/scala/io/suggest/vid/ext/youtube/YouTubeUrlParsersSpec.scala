package io.suggest.vid.ext.youtube

import io.suggest.vid.ext.{MVideoExtInfo, MVideoServices}
import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.10.17 16:13
  * Description: Тесты для парсеров [[YouTubeUrlParsers]].
  */
object YouTubeUrlParsersSpec extends SimpleTestSuite {

  private object Parsers extends YouTubeUrlParsers


  private def mkTest(url: String, videoId: String): Unit = {
    val pr = Parsers.parse( Parsers.youtubeUrl2videoIdP, url )
    assert( pr.successful, pr.toString )
    assertEquals(pr.get, MVideoExtInfo(MVideoServices.YouTube, videoId))
  }

  private def mkFail(url: String): Unit = {
    val pr = Parsers.parse( Parsers.youtubeUrl2videoIdP, url )
    assert( !pr.successful, pr.toString )
  }


  test("Short URL (youtu.be/XXXXXXX)") {
    mkTest(
      url = "http://youtu.be/dQw4w9WgXcQ",
      videoId = "dQw4w9WgXcQ"
    )
  }

  test("Embed URL (youtube.com/embed/XXXX)") {
    mkTest(
      url = "https://www.youtube.com/embed/dQw4w9WgXcQ",
      videoId = "dQw4w9WgXcQ"
    )
  }

  test("Normal full page URL (youtube.com/watch?v=XXXX)") {
    mkTest(
      url = "http://www.youtube.com/watch?v=dQw4w9WgXcQ",
      videoId = "dQw4w9WgXcQ"
    )
  }

  test("Shorted + QS youtube.com URL (youtube.com/?v=XXXX)") {
    mkTest(
      url = "http://www.youtube.com/?v=dQw4w9WgXcQ",
      videoId = "dQw4w9WgXcQ"
    )
  }

  test("Shorted + v-path URL") {
    mkTest(
      url = "http://www.youtube.com/v/dQw4w9WgXcQ",
      videoId = "dQw4w9WgXcQ"
    )
  }

  test("Shorted + e-path URL") {
    mkTest(
      url = "https://youtube.com/e/dQw4w9WgXcQ",
      videoId = "dQw4w9WgXcQ"
    )
  }

  test("Long username + playlist + video URL") {
    mkTest(
      url = "https://www.youtube.com/user/username#p/u/11/dQw4w9WgXcQ",
      videoId = "dQw4w9WgXcQ"
    )
  }

  test("Support inside-playlist #URL") {
    mkTest(
      url = "http://youtube.com/sandalsResorts#p/c/54B8C800269D7C1B/0/FJUvudQsKCM",
      videoId = "FJUvudQsKCM"
    )
  }

  test("Normal URL with garbage in QS") {
    mkTest(
      url = "http://www.youtube.com/watch?feature=player_embedded&v=dQw4w9WgXcQ",
      videoId = "dQw4w9WgXcQ"
    )
  }

  test("Shorted URL with garbage in QS") {
    mkTest(
      url = "http://www.youtube.com/?feature=player_embedded&v=dQw4w9WgXcQ",
      videoId = "dQw4w9WgXcQ"
    )
  }

  test("Minimal human-readable URL without proto/www/etc") {
    mkTest(
      url     = "youtu.be/dQw4w9WgXcQ",
      videoId = "dQw4w9WgXcQ"
    )
  }


  // Негативные тесты, т.е. возвращающие ошибки:

  test("Error on vimeo URL") {
    mkFail(
      url = "https://vimeo.com/72792859#"
    )
  }

  test("Error on fake-yt-domain") {
    mkFail(
      url = "https://youtube.hu/watch?v=dQw4w9WgXcQ"
    )
  }

}
