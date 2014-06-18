package util.captcha

import org.specs2.mutable._
import CipherUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.14 9:07
 * Description:
 */
class CipherUtilTest extends Specification {

  private def ed(s: String) = decryptPrintable(encryptPrintable(s)) mustEqual s

  ensureBcJce()

  "CipherUtil" should {

    "encrypt and decrypt short ASCII strings" in {
      ed("A5G6XZ")
      ed("%^sge%^")
      ed("DGH^$")
    }

  }

}
