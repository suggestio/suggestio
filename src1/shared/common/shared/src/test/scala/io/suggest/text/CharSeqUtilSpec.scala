package io.suggest.text

import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.17 13:25
  * Description: Tests for [[CharSeqUtil]].
  */
object CharSeqUtilSpec extends SimpleTestSuite {

  // len = 32
  private val str0 = "data:image/jpg;base64,/9j/asdasd"

  test("length") {
    assertEquals(
      CharSeqUtil.view(str0, 0, 4).length,
      str0.subSequence(0, 4).length()
    )
  }

  test("view/2.charAt") {
    assertEquals(
      CharSeqUtil.view(str0, 4, 8).charAt(1),
      str0.substring(4, 8).charAt(1)
    )
  }

  test("view/3.toString should produce valid views for short string") {
    assertEquals(
      CharSeqUtil.view(str0, 4, 10).toString,
      str0.substring(4, 10)
    )
  }

  test("view/3.toString at BOL") {
    assertEquals(
      CharSeqUtil.view(str0, 0, 10).toString,
      str0.substring(0, 10)
    )
  }

  test("view/3.toString at EOL") {
    assertEquals(
      CharSeqUtil.view(str0, 30, 32).toString,
      str0.substring(30, 32)
    )
  }

  test("view/2.toString") {
    assertEquals(
      CharSeqUtil.view(str0, 29).toString,
      str0.substring(29)
    )
  }

}
