package io.suggest.text

import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.10.17 18:14
  * Description: Тесты для [[StringUtil]].
  */
object StringUtilSpec extends SimpleTestSuite {

  import StringUtil.StringCollUtil

  test("mkStringLimitLen() must lazy concat seq into full long string") {
    val strings = Seq(
      "a sd",
      "12213 ",
      "6666 ",
      "srs rvs ",
      " srg 345y dfhdfh",
      "y 5gdegsr s"
    )
    val res = strings.mkStringLimitLen(100, "|")
    assertEquals(res, strings.mkString)
  }

}
