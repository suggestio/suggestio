package util.geo

import java.net.InetAddress
import org.jboss.netty.handler.ipfilter.CIDR
import org.scalatestplus.play._
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.14 15:11
 * Description: Тесты для утили ipgeobase.
 */
class IpGeoBaseSpec extends PlaySpec {

  import IpGeoBase._

  implicit private def str2ip(s: String): InetAddress = InetAddress getByName s

  "range2cidr" must {
    "convert ip ranges into CIDR notation" in {
      val cmp = new InetAddressComparator
      range2cidr("2.0.0.0", "2.15.255.255", cmp) mustBe CIDR.newCIDR("2.0.0.0/12")
    }
  }

}
