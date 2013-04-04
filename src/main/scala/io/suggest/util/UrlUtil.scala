package io.suggest.util

import java.net.{URLDecoder, MalformedURLException, URL}
import java.io.UnsupportedEncodingException
import collection.SortedSet
import gnu.inet.encoding.IDNA

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.03.13 14:05
 * Description: Переписанный на Scala неподдерживаемый bixo.utils.UrlUtils.
 */
object UrlUtil extends Logs {

  val INSTANCE = this

  // Ссылки, которые не должны преобразовываться при абсолютизации ссылки.
  private val IGNORED_PROTOCOL_PATTERN = "(?i)^\\s*(javascript|mailto|about|ftp|feed|news):".r.pattern

  // Тут какие-то костыли для percent-encoding http://en.wikipedia.org/wiki/Percent-encoding
  private val RESERVED_QUERY_CHARS = "%&;=:?#"
  private val RESERVED_PATH_CHARS  = "%/?#"
  private val HEX_CODES = "0123456789abcdefABCDEF"

  // Для матчинга относительного пути типа "/xx/../" внутри URL path.
  // Также матчит /../ в начале URL path. Оба должны быть отреплейсены в "/"
  private val RELATIVE_PATH_PATTERN = "(/[^/]*[^/.]{1}[^/]*/\\.\\./|^(/\\.\\./)+)".r.pattern

  // Мусорные ссылки типа index.html в корне, на которые корень редиректит.
  private val DEFAULT_PAGE_PATTERN = "/((?i)index|default)\\.((?i)js[pf]{1}?[afx]?|cgi|cfm|asp[x]?|[psx]?htm[l]?|php[3456]?)(\\?|&|#|$)".r.pattern

  // rm ;jsessionid=
  private val JSESSION_ID_PATTERN = "(?:;jsessionid=.*?)(\\?|&|#|$)".r.pattern

  // Тут собраны все возможные некорректные имена qs. В SimpleUrlNormalizer использовались позиционно-зависимые трудноулучшаемые регэкспы SESSION_ID_PATTERN и т.д.
  val QS_BAD_KEY_PATTERN = "(?i)([sc]id|(bv|php|js?)?[_-]?sess(ion)?[-_]?(id|key)?|s?ra?nd|cache|from|lastmod|width|format|country|height|src|user|username|uname|return_url|returnurl|sort|sort_by|sortby|sort_direction|sort_key|order_by|orderby|sortorder|collate|r(e?di?r(ect)?|et(urn)?))".r.pattern


  /**
   * Сделать ссылку абсолютной.
   * @param baseUrl Базовая ссылка.
   * @param relativeUrl Относительная ссылка, найденная на странице.
   * @return Option[URL].
   */
  def ensureAbsoluteUrl(baseUrl:URL, relativeUrl:String) : Option[URL] = {
    if (IGNORED_PROTOCOL_PATTERN.matcher(relativeUrl).find)
      return None

    // Also need to handle special case: relativeUrl is just a query string (like "?id=1")
    // and baseUrl doen not end with a '/'.

    try {
      val url = if (!relativeUrl.startsWith("?") || baseUrl.getPath.length == 0 || baseUrl.getPath.endsWith("/") )
        new URL(baseUrl, relativeUrl)
      else
        new URL(baseUrl.getProtocol, baseUrl.getHost, baseUrl.getPort, baseUrl.getPath + relativeUrl)
      Some(url)
    } catch {
      case me:MalformedURLException =>
        warn("Cannot construct URL from '" + relativeUrl + "'", me)
        None
    }
  }


  /**
   * Сгенерить типа ~baseUrl в виде строки.
   * @param url :: String | URL
   * @return вернуть строку.
   */
  def makeProtocolAndDomain(url:String):String = makeProtocolAndDomain(new URL(url))
  def makeProtocolAndDomain(url:URL) : String = {
    val result = new StringBuilder(url.getProtocol)
    result.append("://")

    val host = url.getHost
    if (host.length == 0)
      throw new MalformedURLException("URL w/o domain: " + url.toString)

    result.append(host)
    val port = url.getPort
    if (port > 0 && port != url.getDefaultPort) {
      result.append(':').append(port)
    }
    result.toString()
  }


  // URL normalizer - система нормализации ссылок, которая будет далее использоваться в энтерпрайзе.
  // Является статическим подобием SimpleUrlNormalizer, но часть функций дополнена из sio_url_norm и в целом допилена.

  /**
   * Превратить юникодный символ в виде цифры в валидную escape-последовательность.
   * @param codepoint Int
   * @return "%0D%AF"
   */
  private def encodeCodePoint(codepoint:Int) : String = {
    try {
      val codepoints = Array(codepoint)
      val bytes = new String(codepoints, 0, 1).getBytes("UTF-8")
      val result = new StringBuilder
      bytes.foreach {b:Byte => result.append( formatPc(b) )}
      result.toString()
    } catch {
      case e:UnsupportedEncodingException =>
        error("failed URL encoding: ", e)
        ""
    }
  }


  /**
   * Враппер над encodeCodePoint. Закодировать целый компонент ссылки. В зависимости от компонента используются те или иные
   * наборы reservedChars. Функция посимвольно обходит component и генерит результат через StringBuilder.
   * @param component
   * @param reservedChars
   * @return
   */
  private def encodeUrlComponent(component:String, reservedChars:String) : String = {
    component.foldLeft(new StringBuilder) {
      (sb:StringBuilder, c:Char) =>
        val cp:Int = c.asInstanceOf[Int]
        val a = if (cp == 0x0020)
          '+'
        else if (cp >= 0x007F)
          encodeCodePoint(cp)
        else if (cp < 0x0020 || reservedChars.indexOf(c) != -1)
          formatPc(cp)
        else
          c
        sb.append(a)
    }.toString()
  }


  /**
   * Генератор percent-encoding последовательности. Вызывается из encoding-функций.
   * @param x
   * @return
   */
  private def formatPc(x:Any) = "%%%02X".format(x)


  /**
   * Раскодировать ссылку, закодированную в percent-encoding.
   * @param url
   */
  def decodeUrl(url:String) : String = {
    var offset = 0
    var url1:String = null
    while ({offset = url.indexOf('%', offset); offset != -1}) {
      offset += 1
      val needEscaping = if (offset > url.length - 2)
        true
      else if (HEX_CODES.indexOf(url.charAt(offset)) == -1 || HEX_CODES.indexOf(url.charAt(offset+1)) == -1)
        true
      else
        false
      if (needEscaping) {
        url1 = url.substring(0, offset) + "25" + url.substring(offset)
        offset += 1
      }
    }

    try {
      URLDecoder.decode(url, "UTF-8")
    } catch {
      case e:UnsupportedEncodingException =>
        error("Cannot decode " + url + " , returning as-is.", e)
        url
    }
  }


  /**
   * Нормализация хостнейма. Пока в lower-case и убираются точка в конце.
   * @param hostname
   * @return
   */
  def normalizeHostname(hostname:String) : String = {
    var result = hostname.toLowerCase
    if (result.endsWith(".")) {
      result = result.substring(0, result.length - 1)
    }
    IDNA.toASCII(result)
  }


  /**
   * Re-encode path/query portions of URL.
   * @param path
   * @return
   */
  def normalizePath(path:String) : String = {
    // First, handle relative paths
    var matcher = RELATIVE_PATH_PATTERN.matcher(path)
    var path1 : String = path
    while(matcher.find) {
      path1 = path1.substring(0, matcher.start) + "/" + path1.substring(matcher.end)
      matcher = RELATIVE_PATH_PATTERN.matcher(path1)
    }

    // Next, get rid of any default page.
    matcher = DEFAULT_PAGE_PATTERN.matcher(path1)
    if (matcher.find)
      path1 = path.substring(0, matcher.start) + "/" + matcher.group(3) + path.substring(matcher.end)

    val newPath = path.split('/').foldLeft(new StringBuilder) { (acc, pathPart) =>
      if (pathPart.length > 0)
        acc.append('/').append(encodeUrlComponent(decodeUrl(pathPart), RESERVED_PATH_CHARS))
      else
        acc
    }

    if (newPath.length == 0)
      "/"
    else {
      if (path.endsWith("/") && newPath.charAt(newPath.length -1) != '/')
        newPath.append('/')
      newPath.toString()
    }
  }


  /**
   * Normalize query portions: handle decoding and re-encoding of qs portions.
   * @param query
   * @return
   */
  def normalizeQuery(query:String) : String = {
    if (query == null || query == "")
      return ""

    // Разбить строку по &. Для каждого элемента выполнить функцию нормализацию, кот. генерит токены '&','asd','=','1'
    // Затем выпилить leading '&' и превратить в строку.
    // Вместо sort+distinct используем SortedSet как более быструю замену usort.
    val resultSb = SortedSet(query.split('&') : _*)
      .foldLeft(new StringBuilder) { (sb, queryPart) =>
        if (queryPart.length == 0 || QS_BAD_KEY_PATTERN.matcher(queryPart).find)
          sb
        else {
          // Есть ли знак '=' в данном qs?
          queryPart.indexOf('=') match {
            // аргумент-флаг. Типа ...&arg&...
            case -1 =>
              sb.append('&').append( normalizeQueryPart(queryPart) )

            // Есть '=' в куске qs.  Разбить строку по первому найденному символу '=', нормализовать и склеить назад
            case i  =>
              val (qk, _qv) = queryPart.splitAt(i)
              val qv = _qv.tail // из-за символа '=' в начале
              if (CryptoUtil.isHexHash(qv))
                sb
              else
                sb.append('&')
                  .append(normalizeQueryPart(qk))
                  .append('=')
                  .append(normalizeQueryPart(qv))
          }
        }
    }
    if(resultSb.length > 0)
       resultSb.tail.toString()
    else
      ""
  }

  private def normalizeQueryPart(qpart:String) = encodeUrlComponent(decodeUrl(qpart) , RESERVED_QUERY_CHARS)


  /**
   * Нормализовать строку ссылки. Нормализация включает в себя выпиливание лишних аргументов, нормализацию всего остального.
   * @param url - URL to normalize. Might not be valid, e.g. missing a protocol
   * @return - normalized URL. Still might not be valid, if input URL (for example)
   */
 def normalize(url: String): String = {
    var result = url.trim

    // First, see if there is any protocol. If not - append http:// by default.
    result = result.indexOf("://") match {
      case -1 => "http://" + result
      case 0  => "http" + result
      case _  => result
    }

    // Strip session_ids and other garbage from URL
    val jsid_matcher = JSESSION_ID_PATTERN.matcher(result)
    if (jsid_matcher.find)
      result = result.substring(0, jsid_matcher.start) + jsid_matcher.group(1) + result.substring(jsid_matcher.end)

    val decodedUrl = result.replace("+", "%20")

    var url1:String = null
    var testUrl:URL = null
    try {
      testUrl = new URL(decodedUrl)
      url1 = testUrl.toExternalForm
    } catch {
      case ex:MalformedURLException =>
        debug("Malformed URL " + result + " returning as-is", ex)
        return result
    }

    val proto = testUrl.getProtocol.toLowerCase
    if (proto != "http" && proto != "https")
      return result

    val hostname = normalizeHostname(testUrl.getHost)

    var port = testUrl.getPort
    if (port == testUrl.getDefaultPort)
      port = -1

    val path = normalizePath(testUrl.getPath)

    val urlRest = new StringBuilder
    urlRest.append(path)

    val query = normalizeQuery( testUrl.getQuery )
    if (query.length > 0)
      urlRest.append('?').append(query)

    testUrl.getRef match {
      case ref:String if ref.length > 0 && ref.head == '!'  =>
        urlRest.append('#').append(ref)

      case _ =>
    }

    try{
      new URL(proto, hostname, port, urlRest.toString()).toExternalForm
    } catch {
      case ex:MalformedURLException =>
        error("Cannot build final URL from " + url, ex)
        result
    }
  }


  //
  //def url_to_crawldb_hostless_key()
}
