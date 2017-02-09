package io.suggest.sec.util

import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.14 9:07
 * Description: Тесты для шифровалки-дешифровалки.
 */
class CipherUtilSpec extends PlaySpec {

  private lazy val cipherUtil = new CipherUtil

  private def newCu = cipherUtil.Cipherer(
    IV_MATERIAL_DFLT = cipherUtil.generateSecretKey(32),
    SECRET_KEY       = cipherUtil.generateSecretKey(32)
  )

  private def _mkTest(str: String): Unit = {
    val cu = newCu
    val ivData = "asdasd".getBytes

    cu.decryptPrintable(cu.encryptPrintable(str, ivData), ivData)  mustEqual  str
  }

  "CipherUtil" should {

    "encrypt and decrypt A5G6XZ" in {
      _mkTest("A5G6XZ")
    }

    "encrypt and decrypt %^sge%^" in {
      _mkTest("%^sge%^")
    }

    "encrypt and decrypt DGH^$" in {
      _mkTest("DGH^$")
    }

    "encrypt and decrypt 123e4567-e89b-12d3-a456-426655440000" in {
      _mkTest("123e4567-e89b-12d3-a456-426655440000")
    }

  }

}
