package io.suggest.tv

import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.2019 17:10
  */
object SmartTvUtilSpec extends SimpleTestSuite {

  // Список User-Agent'ов для Smart-TV браузеров:
  // https://developers.whatismybrowser.com/useragents/explore/hardware_type_specific/tv/

  test("isSmartTv for real SmartTV") {
    val ua = """Mozilla/5.0 (Web0S; Linux/SmartTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.34 Safari/537.36 DMOST/1.0.1 (; LGE; webOSTV; WEBOS4.0.0 03.01.00; W4_lm18a;)"""
    assert( SmartTvUtil.isSmartTvUserAgent(ua) )
  }

  test("isSmartTv for Samsung Browser 1.1") {
    val ua = """Mozilla/5.0 (SMART-TV; Linux; Tizen 2.4.0) AppleWebkit/538.1 (KHTML, like Gecko) SamsungBrowser/1.1 TV Safari/538.1"""
    assert( SmartTvUtil.isSmartTvUserAgent(ua) )
  }

  test("!isSmartTv for PC Chrome") {
    val ua = """Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36"""
    assert( !SmartTvUtil.isSmartTvUserAgent(ua) )
  }

  test("!isSmartTv for firefox") {
    val ua = """Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0"""
    assert( !SmartTvUtil.isSmartTvUserAgent(ua) )
  }

  test("isSmartTv for WebTV") {
    val ua = "Mozilla/4.0 WebTV/2.6 (compatible; MSIE 4.0)"
    assert( SmartTvUtil.isSmartTvUserAgent(ua) )
  }

  test("isSmartTv for GoogleTV") {
    val ua = """Mozilla/5.0 (Linux; GoogleTV 3.2; NSZ-GS7/GX70 Build/MASTER) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.77 Safari/534.24"""
    assert( SmartTvUtil.isSmartTvUserAgent(ua) )
  }

  test("isSmartTv for Sony opera inettv") {
    val ua = "Opera/9.80 (Linux armv7l; InettvBrowser/2.2 (00014A;SonyDTV140;0001;0001) KDL60W605B; CC/GBR) Presto/2.12.407 Version/12.50"
    assert( SmartTvUtil.isSmartTvUserAgent(ua) )
  }

}
