package io.suggest.sec.csp

import io.suggest.common.empty.EmptyProduct

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.05.17 12:47
  * Description: CSP-заголовки имеют нетривиальный синтаксис и требуют кое-какого синтаксиса,
  * чтобы удобно их генерить.
  *
  * @see [[https://content-security-policy.com/]]
  */

object Csp {

  /** Название заголовка. */
  val HDR_NAME = "Content-Security-Policy"

  /** Заголовок для разрешающей всё политики, но ругающейся в логи. */
  val HDR_NAME_REPORT_ONLY = HDR_NAME + "-Report-Only"


  /** Синтаксис списков источников. */
  object Sources {

    /** Разделитель элементов списка источников. */
    val DELIM = ' '


    /** Allows loading resources from the same origin (same scheme, host and port). */
    val SELF = "'self'"

    /** Wildcard, allows any URL except data: blob: filesystem: schemes. */
    val * = "*"

    /** Prevents loading resources from any source. */
    val NONE = "'none'"

    /** Allows loading resources via the data scheme (eg Base64 encoded images). */
    def DATA = "data:"

    /** Allows loading resources only over HTTPS on any domain. */
    def HTTPS = "https:"

    /** Allows use of inline source elements such as style attribute, onclick, or script tag bodies (depends on the context of the source it is applied to) and javascript: URIs */
    def UNSAFE_INLINE = "'unsafe-inline'"

    /** Allows unsafe dynamic code evaluation such as JavaScript eval(). */
    def UNSAFE_EVAL = "'unsafe-eval'"

    /** Allows script or style tag to execute if the nonce attribute value matches the header value. For example: <script nonce="2726c7f26c">alert("hello");</script> */
    def NONCE(nonce: String) = "'nonce-" + nonce + "'"

    /** Allow a specific script or style to execute if it matches the hash. Doesn't work for javascript: URIs. For example: sha256-qznLcsROx4GACP2dm0UCKCzCG+HiZ1guq6ZZDob/Tng= will allow alert("Hello, world."); */
    def SHA256(sha256: String) = "'sha256-" + sha256 + "'"

  }


  object SandBox {

    object Allow {

      def `forms` = "allow-forms"
      def `same-origin` = "allow-same-origin"
      def `scripts` = "allow-scripts"
      def `popups` = "allow-popups"
      def `modals` = "allow-modals"
      def `orientation-lock` = "allow-orientation-lock"
      def `pointer-lock` = "allow-pointer-lock"
      def `presentation` = "allow-presentation"
      def `popups-to-escape-sandbox` = "allow-popups-to-escape-sandbox"
      def `top-navigation` = "allow-top-navigation"

    }

  }


  /** Разделитель между кусками. */
  val DELIM = "; "

}


/**
  *
  * @param policy Политика безопасности. Содержит значения хидера.
  * @param reportOnly Режим работы. Если true, значит всё разрешено, будут только report'ы. [false]
  */
case class CspHeader(
                      policy      : CspPolicy,
                      reportOnly  : Boolean   = false
                    ) {

  def headerName: String = {
    if (reportOnly)
      Csp.HDR_NAME_REPORT_ONLY
    else
      Csp.HDR_NAME
  }

  def headerValue = policy.toString

  def headerOpt: Option[(String, String)] = {
    if (policy.isEmpty)
      None
    else
      Some(headerName -> headerValue)
  }

}


object CspPolicy {

  /** Рендер CSP-политики в строку-значение HTTP-заголовка.
    *
    * @param policy Инстанс [[CspPolicy]] с данными политики безопасности.
    * @return Строка значения HTTP-заголовка, совместимая с
    *         [[http://www.w3.org/TR/2012/CR-CSP-20121115/ Content Security Policy 1.0 W3C Candidate Recommendation]].
    */
  def format(policy: CspPolicy): String = {
    val sb = new StringBuilder(64)

    val srcDelim = Csp.Sources.DELIM
    val rulesDelim = Csp.DELIM

    // Добавить список значений в аккамулятор.
    def __append(k: String, vs: TraversableOnce[String]): Unit = {
      sb.append(k)
      for (v <- vs) {
        sb.append( srcDelim )
          .append(v)
      }
      sb.append( rulesDelim )
    }


    // CSP L1
    if (policy.defaultSrc.nonEmpty)
      __append("default-src", policy.defaultSrc)

    if (policy.scriptSrc.nonEmpty)
      __append("script-src", policy.scriptSrc)

    if (policy.styleSrc.nonEmpty)
      __append("style-src", policy.styleSrc)

    if (policy.imgSrc.nonEmpty)
      __append("img-src", policy.imgSrc)

    if (policy.connectSrc.nonEmpty)
      __append("connect-src", policy.connectSrc)

    if (policy.fontSrc.nonEmpty)
      __append("font-src", policy.fontSrc)

    if (policy.objectSrc.nonEmpty)
      __append("object-src", policy.objectSrc)

    if (policy.mediaSrc.nonEmpty)
      __append("media-src", policy.mediaSrc)

    if (policy.frameSrc.nonEmpty)
      __append("frame-src", policy.frameSrc)

    for (reportUri <- policy.reportUri)
      __append("report-uri", reportUri :: Nil)


    // CSP L2
    if (policy.childSrc.nonEmpty)
      __append("child-src", policy.childSrc)

    if (policy.formAction.nonEmpty)
      __append("form-action", policy.formAction)

    if (policy.frameAncestors.nonEmpty)
      __append("frame-ancestors", policy.frameAncestors)

    if (policy.pluginTypes.nonEmpty)
      __append("plugin-types", policy.pluginTypes)


    sb.toString()
  }

}


/** Класс-билдер данных заголовка CSP.
  *
  * @param defaultSrc The default-src is the default policy for loading content such as JavaScript, Images, CSS, Font's, AJAX requests, Frames, HTML5 Media. See the Source List Reference for possible values.
  * @param scriptSrc Defines valid sources of JavaScript.
  * @param styleSrc Defines valid sources of stylesheets.
  * @param imgSrc Defines valid sources of images.
  * @param connectSrc Applies to XMLHttpRequest (AJAX), WebSocket or EventSource. If not allowed the browser emulates a 400 HTTP status code.
  * @param fontSrc Defines valid sources of fonts.
  * @param objectSrc Defines valid sources of plugins, eg <object>, <embed> or <applet>.
  * @param frameSrc Defines valid sources for loading frames. child-src is preferred over this deprecated directive.
  *                 Deprecated, but CSP L1 compatible.
  * @param reportUri Instructs the browser to POST a reports of policy failures to this URI.
  *
  * @param childSrc Defines valid sources for web workers and nested browsing contexts loaded using elements such as <frame> and <iframe>.
  * @param formAction Defines valid sources that can be used as a HTML <form> action.
  * @param frameAncestors Defines valid sources for embedding the resource using <frame> <iframe> <object> <embed> <applet>. Setting this directive to 'none' should be roughly equivalent to X-Frame-Options: DENY
  * @param pluginTypes Defines valid MIME types for plugins invoked via <object> and <embed>. To load an <applet> you must specify application/x-java-applet.
  */
case class CspPolicy(
                      // TODO Заменить Seq на Set? Это будет удобно для сложных случаев, но нарушает порядок.
                      // CSP Level 1
                      defaultSrc        : Seq[String]     = Nil,
                      scriptSrc         : Seq[String]     = Nil,
                      styleSrc          : Seq[String]     = Nil,
                      imgSrc            : Seq[String]     = Nil,
                      connectSrc        : Seq[String]     = Nil,
                      fontSrc           : Seq[String]     = Nil,
                      objectSrc         : Seq[String]     = Nil,
                      mediaSrc          : Seq[String]     = Nil,
                      frameSrc          : Seq[String]     = Nil,
                      reportUri         : Option[String]  = None,
                      // CSP Level 2
                      childSrc          : Seq[String]     = Nil,
                      formAction        : Seq[String]     = Nil,
                      frameAncestors    : Seq[String]     = Nil,
                      pluginTypes       : Seq[String]     = Nil
                    )
  extends EmptyProduct
{

  // CSP L1
  def withDefaultSrc(defaultSrc1: String*) = copy(defaultSrc = defaultSrc1)
  def withScriptSrc(scriptSrc1: String*) = copy(scriptSrc = scriptSrc1)
  def withStyleSrc(styleSrc1: String*) = copy(styleSrc = styleSrc1)
  def withImgSrc(imgSrc1: String*) = copy(imgSrc = imgSrc1)
  def withConnectSrc(connectSrc1: String*) = copy(connectSrc = connectSrc1)
  def withFontSrc(fontSrc1: String*) = copy(fontSrc = fontSrc1)
  def withObjectSrc(objectSrc1: String*) = copy(objectSrc = objectSrc1)
  def withMediaSrc(mediaSrc1: String*) = copy(mediaSrc = mediaSrc1)
  def withFrameSrc(frameSrc1: String*) = copy(frameSrc = frameSrc1)
  def withReportUri(reportUri1: String = null) = copy(reportUri = Option(reportUri1))

  // CSP L2
  def withChildSrc(childSrcs: String*) = copy(childSrc = childSrcs)
  def withFormAction(formActions: String*) = copy(formAction = formActions)
  def withFrameAncestors(frameAncestors1: String*) = copy(frameAncestors = frameAncestors1)
  def withPluginTypes(pluginTypes1: String*) = copy(pluginTypes = pluginTypes1)


  override def toString = CspPolicy.format(this)

}
