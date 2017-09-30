package com.github.vibornoff.asmcryptojs

import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.09.17 15:49
  * Description: Tests for [[AsmCrypto]].
  */
object AsmCryptoSpec extends SimpleTestSuite {

  // TODO All these tests fails on node.js: https://github.com/vibornoff/asmcrypto.js/issues/114

  test("asmCrypto.SHA1.hex") {
    assertEquals(
      AsmCrypto.SHA1.hex("asd"),
      "f10e2821bbbea527ea02200352313bc059445190"
    )
  }

  test("asmCrypto.SHA1.base64") {
    assertEquals(
      AsmCrypto.SHA1.hex("asd"),
      "8Q4oIbu+pSfqAiADUjE7wFlEUZA="
    )
  }

}
