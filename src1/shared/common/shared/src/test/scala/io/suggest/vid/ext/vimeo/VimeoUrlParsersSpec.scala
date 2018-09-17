package io.suggest.vid.ext.vimeo

import io.suggest.vid.ext.{MVideoExtInfo, MVideoServices}
import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.10.17 17:47
  * Description: Тесты для парсеров vimeo-ссылок.
  */
object VimeoUrlParsersSpec extends SimpleTestSuite {

  private object Parsers extends VimeoUrlParsers

  private def mkTest(url: String, videoId: Int): Unit = {
    val pr = Parsers.parse( Parsers.vimeoUrl2VideoP, url )
    assert(pr.successful, pr.toString)
    assertEquals(pr.get, MVideoExtInfo(MVideoServices.Vimeo, videoId.toString))
  }

  private def mkFail(url: String): Unit = {
    val pr = Parsers.parse( Parsers.vimeoUrl2VideoP, url )
    assert(!pr.successful, pr.toString)
  }


  test("Simple https video URL: https://vimeo.com/XXXXX") {
    mkTest(
      url     = "https://vimeo.com/11111111",
      videoId = 11111111
    )
  }

  test("Simple http video URL: http://vimeo.com/XXXXXXX") {
    mkTest(
      url     = "http://vimeo.com/11111111",
      videoId = 11111111
    )
  }

  test("Simple https video URL with www: https://www.vimeo.com/XXXXXX") {
    mkTest(
      url     = "https://www.vimeo.com/11111111",
      videoId = 11111111
    )
  }

  test("Simple http+www video URL: http://www.vimeo.com/XXXXXX") {
    mkTest(
      url     = "http://www.vimeo.com/11111111",
      videoId = 11111111
    )
  }

  // TODO Разобраться, что за каналы у vimeo и как их воспринимать.
  /*
  test("Fail on channel URL") {
    mkFail(
      url = "https://vimeo.com/channels/11111111"
    )
  }
  */

  test("groups/name video URL") {
    mkTest(
      url     = "https://vimeo.com/groups/name/videos/11111111",
      videoId = 11111111
    )
  }


  test("Simple URL with qs-garbage") {
    mkTest(
      url = "https://vimeo.com/11111111?param=test",
      videoId = 11111111
    )
  }

  test("Very-minimal vimeo URL (w/o https and www)") {
    mkTest(
      url     = "vimeo.com/11111111",
      videoId = 11111111
    )
  }


  test("Fail on youtube URL") {
    mkFail(
      url = "https://youtube.com/watch?v=134rfwe45fw4f"
    )
  }

}
