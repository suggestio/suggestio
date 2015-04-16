package util

import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.14 9:07
 * Description: Тесты для шифровалки-дешифровалки.
 */
class CipherUtilTest extends PlaySpec {

  private def newCu = new CipherUtilAddon {
    override val IV_MATERIAL_DFLT = CipherUtil.generateSecretKey(256)
    override val SECRET_KEY = CipherUtil.generateSecretKey(256)
  }

  CipherUtil.ensureBcJce()

  "CipherUtil" should {

    "encrypt and decrypt short ASCII strings" in {
      val cu = newCu
      val ivData = "asdasd".getBytes
      // Быстрый враппер для шифрования и дешифровки сразу.
      def ed(s: String) = {
        import cu._
        decryptPrintable(encryptPrintable(s, ivData), ivData) mustEqual s
      }

      // Пошли тесты.
      ed("A5G6XZ")
      ed("%^sge%^")
      ed("DGH^$")
      ed("123e4567-e89b-12d3-a456-426655440000")
    }

  }

}
