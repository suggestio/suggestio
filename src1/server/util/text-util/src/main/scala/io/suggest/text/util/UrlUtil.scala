package io.suggest.text.util

import java.io.UnsupportedEncodingException
import java.net.{MalformedURLException, URL, URLDecoder}

import gnu.inet.encoding.IDNA
import io.suggest.util.logs.MacroLogsImplLazy

import scala.annotation.tailrec
import scala.collection.SortedSet
import scala.util.matching.Regex

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.03.13 14:05
 * Description: Переписанный на Scala неподдерживаемый bixo.utils.UrlUtils.
 */
object UrlUtil extends Serializable with MacroLogsImplLazy  {

  import LOGGER._

  // Ссылки, которые не должны преобразовываться при абсолютизации ссылки.
  private val IGNORED_PROTOCOL_PATTERN = "(?i)^\\s*(javascript|mailto|about|ftp|feed|news):".r.pattern

  // Тут какие-то костыли для percent-encoding http://en.wikipedia.org/wiki/Percent-encoding
  private val RESERVED_QUERY_CHARS = "%&;=:?#"
  private val RESERVED_PATH_CHARS  = "%/?#"

  // Для матчинга относительного пути типа "/xx/../" внутри URL path.
  // Также матчит /../ в начале URL path. Оба должны быть отреплейсены в "/"
  private val RELATIVE_PATH_PATTERN = "(/[^/]*[^/.]{1}[^/]*/\\.\\./|^(/\\.\\./)+)".r.pattern

  // Мусорные ссылки типа index.html в корне, на которые корень редиректит.
  private val DEFAULT_PAGE_PATTERN = "/((?i)index|default)\\.((?i)js[pf]{1}?[afx]?|cgi|cfm|asp[x]?|[psx]?htm[l]?|php[3456]?)(\\?|&|#|$)".r.pattern

  // rm ;jsessionid=
  private val JSESSION_ID_PATTERN = "(?:;jsessionid=.*?)(\\?|&|#|$)".r.pattern

  // Тут собраны все возможные некорректные имена qs. В SimpleUrlNormalizer использовались позиционно-зависимые трудноулучшаемые регэкспы SESSION_ID_PATTERN и т.д.
  private val QS_BAD_KEY_PATTERN = "(?i)([sc]id|(bv|php|js?)?[_-]?sess(ion)?[-_]?(id|key)?|s?ra?nd|cache|from|lastmod|width|format|country|height|src|user|username|uname|return_url|returnurl|sort|sort_by|sortby|sort_direction|sort_key|order_by|orderby|sortorder|collate|r(e?di?r(ect)?|et(urn)?))".r.pattern


  // Регэксп для отсеивания нежелательных хостов.
  private val INVALID_HOSTNAME_RE = {
    // TODO вынести reList в файл и сделать возможность периодического перечитывания файла.
    val reList = List(
      // запрещаем ip-адреса
      "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}",			        // Decimal ip4: 127.0.0.1
      "0[0-7]{1,3}\\.0[0-7]{1,3}\\.0[0-7]{1,3}\\.0[0-7]{1,3}",	// Octal ip4:	0305.0151.0043.0236
      "0x[0-9a-f]",							                                // Hexadecimal ip4: 0xD65C238E
      "\\d+",								                                    // Integer IPv4: 123124234
      // TODO 158.35.108.213.hl.ru, ipv6
      // Отечественное добро
      "(odn[oa]klass?niki|vk([oau]ntakte|avkaze)?|mo[iy][kc]rug)\\.(ru|com|net|kz|by)",
      // Домены, которые слишком большие и/или не нуждаются в поиске:
      "sugg?est\\.io",
      "(git(hub|orio?us)|bitbucket)\\.(com|org)",
      // Всякие транснациональные порталы имеют много адресов в разных доменах
      "goo\\.gl",
      "(ya(ndex)?|google|(you|ru)tu(be)?|bing|yahoo|msn|microsoft)(\\.[a-z]{2,3})+",
      "(facebook|twitter|linkedin)\\.com", "t\\.co",
      "(paypal|moneybookers|ebay|amazon|(hot|g|x)?mail)\\.(com|ru|org|net|co\\.uk)",
      "wikipedia\\.org",
      "archive\\.org",
      "paste(bin)?\\.(ca|org)",
      "asdasd\\.ru"
    )
    val reStr = "^(?i)([^/]+\\.)?((" + reList.mkString(")|(") + "))$"
    reStr.r
  }

  // Регэксп для отсеивания ссылок/линков на картинки.
  private val INVALID_IMAGE_RE = "\\.(bmp|raw)$".r

  // Список регэкспов для отсеивания нежелателньых и мусорных ссылок.
  private val INVALID_URL_PATTERNS : List[Regex] = {
    val reStrList = List(
      // есть много сайтов, которые комменты делают отдельными страницами (news2.ru, snob.ru и другие).
      "://[^/]+/.+comment(([.a-z]{4,6})?\\?.+=[0-9]+|/?[0-9]+/?$)",
      "//.+//",
      "^https?://[^/]+/se[ea]*r?t?[ch][hc][/?]",
      "/bugs/",
      "viewcvs",

      // Skipping Apache.org urls
      "\\.apache\\..+/dist/",
      "/snapshots/",
      "^https?://mail-archives",
      // TODO apache mirror sites..
      "apache\\.fastbull\\.org/.+",

      // wiki
      "wiki.*/+.*(E?Spe[czs]i(aa?|ie)le?|Specjalna|Istimewa|Toiminnot|%D0%A1%D0%BF%D0%B5%D1%86%D0%B8%D0%B0%D0%BB%D1%8C%D0%BD%D1%8B%D0%B5|MediaWiki(_talk)?)(:|%3A)",
      "wiki.*/+.*/index.php\\?.*((oldid=[0-9]+)|(bookcmd=[a-z])|(printable=(yes|true|1))|(action=[a-z]))",

      // livejournal
      "livejournal.(com|ru)/[0-9]+\\..+\\?.*(thread=|reply|poster=)",

      // phpBB
      "\\?.*do=reply",

      // TODO удалять отсюда расширения файлов по мере добавления функционала в парсеры.
      // TODO Сделать бы это отдельным списком и чтоб был автогенератор регэкспа при компиляции или $init$...
      "://[^/]+/[^.]+\\.(jpe?g|j2k|gif|png|bmp|exe|[gbx]?z(ip|2)?|dll|dat|avi|mkv|rtf|dot[xm]?|xl([scmwkt]|s[xbm])|f?o[dt][ts]|od[bmf]|s[tx][wc]|t[gbx]z2?|[tjr]?ar|so|csv|deb|rpm|git|i|raw|arw|bin|crt|pem|gpg|pgp|pub)\\b",
      "[Mmr]akefile\\b",

      // Skipping CVS repositories
      "/cvs/\\.",

      // Skipping Gitorious unseful pages
      "git.+/(merge_requests|commits?|trees?)/",

      // HG repositories
      "/changeset/",

      // Skipping SVN repositories
      "svn.+/viewvc/.+/",
      "/svn[\\./]",
      "/branches",
      "/trunk",
      "/tags"
    )
    reStrList.map(reStr => ("(?i)" + reStr).r)
  }



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

  def humanizeUrlAggressive(url: String): String = {
    try {
      humanizeUrlAggressive( new URL(url) )
    } catch {
      case ex: Exception => url
    }
  }
  def humanizeUrlAggressive(url: URL): String = {
    val sb = new StringBuilder(128)
    val proto = url.getProtocol
    if ( !((proto equalsIgnoreCase "http") || (proto equalsIgnoreCase "https")) ) {
      sb.append(proto.toUpperCase).append("://")
    }
    // Резать www. в начале.
    val host = stripHostnameWww(url.getHost)
    humanizeUrlHost(host, sb)
    humanizeUrlPort(url, sb)
    val file = url.getFile
    if (file != null && !file.isEmpty && file != "/") {
      humanizeUrlFile(file, sb)
    }
    sb.toString()
  }


  def humanizeUrl(url: String): String = {
    try {
      humanizeUrl( new URL(url) )
    } catch {
      case ex: Exception => url
    }
  }
  def humanizeUrl(url: URL): String = {
    val sb = new StringBuilder(128)
    sb.append(url.getProtocol).append("://")
    humanizeUrlHost(url.getHost, sb)
    humanizeUrlPort(url, sb)
    humanizeUrlFile(url.getFile, sb)
    sb.toString()
  }

  private def humanizeUrlHost(host: String, sb: StringBuilder) {
    sb append IDNA.toUnicode(host)
  }

  private def humanizeUrlPort(url: URL, sb: StringBuilder): Unit = {
    val port = url.getPort
    if (port > 0 && port != url.getDefaultPort) {
      sb.append(":").append(port)
    }
  }

  /** Хумнизация части ссылки, которая path + query. */
  private def humanizeUrlFile(urlFile: String, sb: StringBuilder): Unit = {
    sb append pcDecodeSafe(urlFile)
  }

  // URL normalizer - система нормализации ссылок, которая будет далее использоваться в энтерпрайзе.
  // Является статическим подобием SimpleUrlNormalizer, но часть функций дополнена из sio_url_norm и в целом допилена.

  /**
   * Превратить юникодный символ в виде цифры в валидную escape-последовательность.
   * @param codepoint Int
   * @return "%0D%AF"
   */
  private def encodeCodePoint(codepoint: Int): String = {
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
   * Раскодировать ссылку или её часть, закодированную в percent-encoding.
   * При проблемах раскодирания будет возвращена исходная строка.
   * @param encoded Строка, закодированная через percent encoding.
   */
  def pcDecodeSafe(encoded: String): String = {
    try {
      URLDecoder.decode(encoded, "UTF-8")
    } catch {
      case e:UnsupportedEncodingException =>
        error("Cannot decode " + encoded + " , returning as-is.", e)
        encoded
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
    if (result.startsWith("."))
      result = result.tail
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
        acc.append('/').append(encodeUrlComponent(pcDecodeSafe(pathPart), RESERVED_PATH_CHARS))
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
              if (TextHashUtil.isHexHash(qv))
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

  private def normalizeQueryPart(qpart:String) = encodeUrlComponent(pcDecodeSafe(qpart) , RESERVED_QUERY_CHARS)


  /**
   * Нормализовать строку ссылки. Нормализация включает в себя выпиливание лишних аргументов, нормализацию всего остального.
   * @param url - URL to normalize. Might not be valid, e.g. missing a protocol
   * @return - normalized URL. Still might not be valid, if input URL (for example)
   */
  def normalize(url: String): String = {
    // TODO Следует наверное использовать Option[String] или Either[String,String] в качестве результата функции.
    var result = url
      .trim
      .replaceAll("[\r\n\t]", "")
      .replaceAll("&quot$", "")

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
        trace(s"normalize(): ${ex.getClass.getSimpleName}: ${ex.getMessage} ;; returning URL as-is: $result")
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


  /**
   * Является ли указанный хостнейм разрешенным.
   * @param hostname хост
   * @return true, если всё ок.
   */
  def isHostnameValid(hostname:String) : Boolean = {
    !INVALID_HOSTNAME_RE.pattern.matcher(hostname).matches()
  }


  /**
   * Является ли указанная ссылка/линк на картинку валидным?
   * @param imageLink ссылка/линк на картинку.
   * @return true, если всё ок.
   */
  def isImageLinkValid(imageLink:String) : Boolean = {
    !INVALID_IMAGE_RE.pattern.matcher(imageLink).matches()
  }


  /**
   * Является ли указанный URL валидным.
   * @param url URL строка.
   * @return true, если всё верно.
   */
  def isPageUrlValid(url:String): Boolean = {
    !INVALID_URL_PATTERNS.exists(_.pattern.matcher(url).find())
  }


  /**
   * Донормализовать хостнейм до dkey.
   * @param host хост.
   * @return строка dkey
   */
  def host2dkey(host:String): String = {
    stripHostnameWww(
      normalizeHostname(host)
    )
  }

  /**
   * Извлечь dkey из ссылки.
   * @param url Исходная ссылка
   * @return Строка dkey.
   */
  def url2dkey(url:String): String = url2dkey(new URL(url))
  def url2dkey(url:URL): String    = host2dkey(url.getHost)

  /**
   * Срезать все www. в начале хостнейма.
   * @param host хостнейм
   * @return
   */
  @tailrec def stripHostnameWww(host:String) : String = {
    if (host.startsWith("www."))
      stripHostnameWww(host.substring(4))
    else
      host
  }

  /**
   * Отреверсить доменное имя  (www.suggest.io -> io.suggest.www). Бывает полезно для HBase-ключей, чтобы в будущем
   * делать быстрые сканы внутри домена по префиксу ключа.
   * @param domain Исходное доменное имя.
   * @return Обратное доменное имя.
   */
  def reverseDomain(domain:String): String = {
    domain
      .split('.')
      .reverse
      .mkString(".")
  }

  /**
   * Перегнать строковую ссылку в row-key для HBase. По сути враппер над url2rowKey().
   * @param urlStr Строка URL.
   * @return Строка row-key.
   */
  def url2rowKey(urlStr:String): String = url2rowKey(new URL(urlStr))

  /**
   * Перегнать ссылку в нормальный row-key для HBase.
   * @param url Ссыль.
   * @return Строка row-key.
   */
  def url2rowKey(url:URL): String = {
    // TODO: hbase-ключ может содержать utf8 символы. Можно не дергать IDNA тут, хотя это экономии практически никакой не даст.
    val dkeyR = reverseDomain(
                 host2dkey(
                   url.getHost))
    val sb = new StringBuilder(dkeyR)
    val urlPath = normalizePath(url.getPath)
    if (urlPath != null && !urlPath.isEmpty) {
      val urlPath1 = urlPath.replaceFirst("/$", "")
      sb.append(urlPath1)
    } else {
      sb.append("/")
    }
    val urlQuery = normalizeQuery(url.getQuery)
    if (urlQuery != null && !urlQuery.isEmpty) {
      sb.append("?")
        .append(urlQuery)
    }
    sb.toString()
  }

  /** Извлечь dkey из rowKey.
   * @param rowKey rowKey в виде строки (результат url2rowKey()).
   * @return dkey.
   */
  def rowKey2dkey(rowKey:String): String = {
    val reversedDkey = rowKey.indexOf('/') match {
      case pos if pos > 3 => rowKey.substring(0, pos)
      case -1             => rowKey
    }
    reverseDomain(reversedDkey)
  }

}
