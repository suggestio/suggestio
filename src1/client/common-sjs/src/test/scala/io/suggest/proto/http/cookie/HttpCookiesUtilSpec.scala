package io.suggest.proto.http.cookie

import java.time.{DayOfWeek, Month}

import io.suggest.text.StringUtil
import minitest._

import scala.util.Random

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.09.2020 18:50
  * Description: Тесты для cookies-парсеров.
  */
object HttpCookiesUtilSpec extends SimpleTestSuite {

  private val rnd = new Random()

  private def _cookieK() = StringUtil.randomId( rnd.nextInt(20) + 1 )
  private def _cookieV() = "cookie-value.243525.rs44Ad1" + StringUtil.randomId( rnd.nextInt(100) )


  private def _manyTimes[U](f: => U): Unit = {
    (1 to (rnd.nextInt(400) + 300))
      .foreach(_ => f)
  }


  test(s"Set-Cookie: k=v") {
    _manyTimes {
      val k = _cookieK()
      val v = _cookieV()
      val res = HttpCookieUtil.parseCookies( s"$k=$v" )
      assert( res.isRight, res.toString )
      val hs = res.toOption.get
      assert( hs.lengthIs == 1, s"Expected lenght=1, but really is ${hs.length}")
      val h = hs.head
      assertEquals( h.name, k )
      assertEquals( h.value, v )
      assert( h.attrs.isEmpty, "Unexpected attrs" )
    }
  }


  test(s"Set-Cookie: k=v; Expires=<valid date>") {
    _manyTimes {
      val k = _cookieK()
      val v = _cookieV()
      val resE = HttpCookieUtil.parseCookies( s"$k=$v; Expires=Wed, 21 Oct 2015 07:28:00 GMT" )
      assert(resE.isRight, resE.toString)
      val res = resE.toOption.get
      assert( res.lengthIs == 1, res.toString() )
      val h = res.head
      assertEquals( h.name, k )
      assertEquals( h.value, v )

      assert( h.expires.nonEmpty, "Expires= not parsed" )
      val d = h.expires.get
      assertEquals( d.getYear, 2015 )
      assertEquals( d.getMonth, Month.OCTOBER )
      assertEquals( d.getDayOfMonth, 21 )
      assertEquals( d.getHour, 7 )
      assertEquals( d.getMinute, 28 )
      assertEquals( d.getSecond, 0 )

      assert( h.sameSite.isEmpty, "SameSite= must be empty" )
      assert( h.path.isEmpty, "Path= must be empty" )
      assert( !h.httpOnly, "HttpOnly must be empty" )
      assert( !h.secure, "Secure must be empty" )
      assert( h.domain.isEmpty, "Domain= must be empty" )
      assert( h.maxAge.isEmpty, "Max-Age unexpected" )
    }
  }


  test(s"Set-Cookie: k=v; Domain=...") {
    _manyTimes {
      val k = _cookieK()
      val v = _cookieV()
      val resE = HttpCookieUtil.parseCookies( s"$k=$v; Domain=suggest.io" )
      assert(resE.isRight, resE.toString)
      val res = resE.toOption.get
      assert( res.lengthIs == 1, res.toString() )
      val h = res.head
      assertEquals( h.name, k )
      assertEquals( h.value, v )

      assert( h.domain contains[String] "suggest.io", s"Domain is missing/invalid: ${h.domain}" )
      assert( !h.secure, "Secure flag must be empty" )
      assert( !h.httpOnly, "HttpOnly unexpected" )
      assert( h.expires.isEmpty, "Expires= unexpected" )
      assert( h.maxAge.isEmpty, "Max-Age unexpected" )
    }
  }


  test(s"Set-Cookie: k=v; Secure; HttpOnly") {
    _manyTimes {
      val k = _cookieK()
      val v = _cookieV()
      val resE = HttpCookieUtil.parseCookies( s"$k=$v; Secure; HttpOnly" )
      assert(resE.isRight, resE.toString)
      val res = resE.toOption.get
      assert( res.lengthIs == 1, res.toString() )
      val h = res.head
      assertEquals( h.name, k )
      assertEquals( h.value, v )

      assert( h.domain.isEmpty, "Domain unexpected" )
      assert( h.secure, "Secure flag not parsed" )
      assert( h.httpOnly, "HttpOnly flag not parsed" )
      assert( h.path.isEmpty, "Path unexpected" )
      assert( h.expires.isEmpty, "Expires unexpected" )
      assert( h.sameSite.isEmpty, "SameSite unexpected" )
      assert( h.maxAge.isEmpty, "Max-Age unexpected" )
    }
  }


  test("Set-Cookie: k=v; Expires=...; Max-Age=...; Domain=...; Secure; HttpOnly; SameSite=Lax") {
    _manyTimes {
      val k = _cookieK()
      val v = _cookieV()
      val resE = HttpCookieUtil.parseCookies( s"$k=$v; Expires=Mon, 09 Sep 2020 08:13:24 GMT; Max-Age=600; Domain=suggest.io; Secure; HttpOnly; SameSite=Lax" )
      assert(resE.isRight, resE.toString)
      val res = resE.toOption.get
      assert( res.lengthIs == 1, res.toString() )
      val h = res.head
      assertEquals( h.name, k )
      assertEquals( h.value, v )

      assert( h.expires.nonEmpty , "Expires= not parsed" )
      h.expires.foreach { d =>
        assertEquals( d.getYear, 2020 )
        assertEquals( d.getMonth, Month.SEPTEMBER )
        assertEquals( d.getDayOfMonth, 9 )
        assertEquals( d.getHour, 8 )
        assertEquals( d.getMinute, 13 )
        assertEquals( d.getSecond, 24 )
        assertEquals( d.getDayOfWeek, DayOfWeek.WEDNESDAY )
      }

      assert( h.maxAge.nonEmpty, "Max-Age not parsed" )
      h.maxAge.foreach { maxAge =>
        assertEquals[Long]( maxAge, 600L )
      }

      assert( h.domain.nonEmpty, "Domain not parsed" )
      h.domain.foreach { domain =>
        assertEquals[String]( domain, "suggest.io" )
      }

      assert( h.secure, "Secure flag not parsed" )
      assert( h.httpOnly, "HttpOnly flag not parsed" )
      assert( h.sameSite contains[String] "Lax", "SameSite not parsed" )
    }
  }


  test("Set-Cookie: k1=v1, k2=v2") {
    _manyTimes {
      val k1 = _cookieK()
      val v1 = _cookieV()

      val k2 = _cookieK()
      val v2 = _cookieV()

      for {
        // По Fetch API должен быть ", ", но cordova-fetch возвращает ",\n". Тестим по-разным направлениям:
        cookieDelim <- " " :: "\n" :: " \n " :: Nil
      } {
        val resE = HttpCookieUtil.parseCookies( s"$k1=$v1,$cookieDelim$k2=$v2" )
        assert(resE.isRight, resE.toString)

        val res = resE.toOption.get
        assert( res.lengthIs == 2, s"Expected 2 cookies, but ${res.length} parsed")

        val c1 = res(0)
        assertEquals( c1.name, k1 )
        assertEquals( c1.value, v1 )
        assert( !c1.secure )
        assert( !c1.httpOnly )

        val c2 = res(1)
        assertEquals( c2.name, k2 )
        assertEquals( c2.value, v2 )
        assert( !c2.secure )
        assert( !c2.httpOnly )
      }
    }
  }


  test("Set-Cookie: k1=v1; Expires=..., k2=v2; Secure; Path=/; Max-Age=...") {
    _manyTimes {
      val k1 = _cookieK()
      val v1 = _cookieV()

      val k2 = _cookieK()
      val v2 = _cookieV()

      val resE = HttpCookieUtil.parseCookies( s"$k1=$v1; Expires=Mon, 09 Sep 2020 08:13:24 GMT, $k2=$v2; Secure; Path=/; Max-Age=555" )
      assert(resE.isRight, resE.toString)

      val res = resE.toOption.get
      assert( res.lengthIs == 2, s"Expected 2 cookies, but ${res.length} parsed")

      val c1 = res(0)
      assertEquals( c1.name, k1 )
      assertEquals( c1.value, v1 )
      assert( !c1.secure )
      assert( !c1.httpOnly )
      assert( c1.expires.nonEmpty, s"Expires missing" )
      c1.expires.foreach { d =>
        assertEquals( d.getYear, 2020 )
        assertEquals( d.getMonth, Month.SEPTEMBER )
        assertEquals( d.getDayOfMonth, 9 )
        assertEquals( d.getHour, 8 )
        assertEquals( d.getMinute, 13 )
        assertEquals( d.getSecond, 24 )
        assertEquals( d.getDayOfWeek, DayOfWeek.WEDNESDAY )
      }
      assert( c1.path.isEmpty, s"Path unexpected: ${c1.path}" )
      assert( c1.maxAge.isEmpty, s"Max-Age unexpected: ${c1.maxAge}" )

      val c2 = res(1)
      assertEquals( c2.name, k2 )
      assertEquals( c2.value, v2 )
      assert( c2.expires.isEmpty, s"Expires unexpected: ${c2.expires}")
      assert( c2.secure, "Secure not parsed" )
      assert( !c2.httpOnly, "HttpOnly unexpected" )
      assert( c2.path contains[String] "/", s"Path unparsed/invalid: ${c2.path}" )
      assert( c2.maxAge contains[Long] 555L, s"Max-Age missing/invalid: ${c2.maxAge}" )
    }
  }


  test("Set-Cookie: Play session cookie #1") {
    val resE = HttpCookieUtil.parseCookies( "s=eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7ImNzcmZUb2tlbiI6IjUyZDlkOGJhODFhY2JhM2UxMjQyYjE1NDNmMjIxNzc5ZWE5NGMxMmEtMTU5OTIyOTUxNTk4OC01YzMxNTQ5OWRlZjM2YjAwODAzMGFmM2QifSwiZXhwIjoxNjAxODIxNTE1LCJuYmYiOjE1OTkyMjk1MTUsImlhdCI6MTU5OTIyOTUxNX0.xdfgs2252_tLYvdxGBGhWHDRt6uh6rHRTH-6vb_le55H6e04; Max-Age=2592000; Expires=Sun, 04 Oct 2020 14:25:15 GMT; SameSite=Strict; Path=/; HTTPOnly" )
    assert( resE.isRight, s"Cookie not parsed: $resE")

    val res = resE.toOption.get
    assert( res.lengthIs == 1, s"Expected 1 cookie, but ${res.length} parsed")

    val c1 = res(0)
    assertEquals( c1.name, "s" )
    assert( c1.value.length > 100, s"Value not parsed properly, length=${c1.value.length}")

    assert( !c1.secure, "Secure unexpected" )
    assert( c1.httpOnly, "HttpOnly not parsed" )

    // Max-Age=2592000
    assert( c1.maxAge contains[Long] 2592000, s"Max-Age=${c1.maxAge} not parsed/invalid" )

    // Expires=Sun, 04 Oct 2020 14:25:15 GMT
    assert( c1.expires.nonEmpty, "Expires= not parsed" )
    c1.expires.foreach { d =>
      assertEquals( d.getYear, 2020 )
      assertEquals( d.getMonth, Month.OCTOBER )
      assertEquals( d.getDayOfMonth, 4 )
      assertEquals( d.getDayOfWeek, DayOfWeek.SUNDAY )
      assertEquals( d.getHour, 14 )
      assertEquals( d.getMinute, 25 )
      assertEquals( d.getSecond, 15 )
    }

    // SameSite=Strict; Path=/
    assert( c1.sameSite contains[String] "Strict", s"SameSite=${c1.sameSite} invalid" )
    assert( c1.path contains "/", s"Path=${c1.path} invalid" )
  }

}
