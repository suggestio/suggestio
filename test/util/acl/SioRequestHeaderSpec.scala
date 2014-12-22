package util.acl

import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.12.14 17:46
 * Description:
 */
class SioRequestHeaderSpec extends PlaySpec {

  import SioRequestHeader._

  "firstForwarded()" must {
    "extract real client ip from X-Forwarded-For: 213.108.35.158, 91.231.236.6, 91.231.236.8" in {
      firstForwardedAddr("213.108.35.158, 91.231.236.6, 91.231.236.8") mustBe "213.108.35.158"
    }

    "extract real client ip from X-Forwarded-For: 213.108.35.158, 151.236.121.23, 151.236.121.11" in {
      firstForwardedAddr("213.108.35.158, 151.236.121.23, 151.236.121.11") mustBe "213.108.35.158"
    }

    "extract real client ip from X-Forwarded-For: 82.146.33.55, 151.236.64.11, 91.231.236.8" in {
      firstForwardedAddr("82.146.33.55, 151.236.64.11, 91.231.236.8") mustBe "82.146.33.55"
    }
  }

}
