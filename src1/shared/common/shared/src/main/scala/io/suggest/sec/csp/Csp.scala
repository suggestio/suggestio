package io.suggest.sec.csp

import io.suggest.common.empty.{EmptyProduct, OptionUtil}
import io.suggest.common.html.HtmlConstants
import io.suggest.proto.HttpConst

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
  val CONTENT_SECURITY_POLICY = "Content-Security-Policy"

  /** Заголовок для разрешающей всё политики, но ругающейся в логи. */
  val CONTENT_SECURITY_POLICY_REPORT_ONLY = CONTENT_SECURITY_POLICY + "-Report-Only"


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
    def DATA = HtmlConstants.Proto.DATA_

    def BLOB = HtmlConstants.Proto.BLOB_

    /** Allows loading resources only over HTTPS on any domain. */
    def HTTPS = HttpConst.Proto.HTTPS_

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
      Csp.CONTENT_SECURITY_POLICY_REPORT_ONLY
    else
      Csp.CONTENT_SECURITY_POLICY
  }

  def headerValue = policy.toString

  def headerOpt: Option[(String, String)] = {
    OptionUtil.maybe( policy.nonEmpty )(header)
  }

  def header = headerName -> headerValue

  def withPolicy(policy2: CspPolicy) = copy(policy = policy2)

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


    // CSP L3
    if (policy.workerSrc.nonEmpty)
      __append("worker-src", policy.workerSrc)


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
  * @param workerSrc Valid sources for Worker, SharedWorker, or ServiceWorker scripts.
  */
case class CspPolicy(
                      // TODO Заменить Seq на Set? Это будет удобно для сложных случаев, но нарушает порядок.
                      // CSP Level 1
                      defaultSrc        : Set[String]     = Set.empty,
                      scriptSrc         : Set[String]     = Set.empty,
                      styleSrc          : Set[String]     = Set.empty,
                      imgSrc            : Set[String]     = Set.empty,
                      connectSrc        : Set[String]     = Set.empty,
                      fontSrc           : Set[String]     = Set.empty,
                      objectSrc         : Set[String]     = Set.empty,
                      mediaSrc          : Set[String]     = Set.empty,
                      @deprecated("Directive ‘frame-src’ has been deprecated. Please use directive ‘child-src’ instead.", "CSP Level 2")
                      frameSrc          : Set[String]     = Set.empty,
                      reportUri         : Option[String]  = None,
                      // CSP Level 2
                      childSrc          : Set[String]     = Set.empty,
                      formAction        : Set[String]     = Set.empty,
                      frameAncestors    : Set[String]     = Set.empty,
                      pluginTypes       : Set[String]     = Set.empty,
                      workerSrc         : Set[String]     = Set.empty,
                    )
  extends EmptyProduct
{

  // CSP L1
  def withDefaultSrc(defaultSrcs: Set[String]) = copy(defaultSrc = defaultSrcs)
  def addDefaultSrc(defaultSrcs: String*) = withDefaultSrc( defaultSrc ++ defaultSrcs )

  def withScriptSrc(scriptSrcs: Set[String]) = copy(scriptSrc = scriptSrcs)
  def addScriptSrc(scriptSrcs: String*) = withScriptSrc( scriptSrc ++ scriptSrcs )

  def withStyleSrc(styleSrcs: Set[String]) = copy(styleSrc = styleSrcs)
  def addStyleSrc(styleSrcs: String*) = withStyleSrc( styleSrc ++ styleSrcs )

  def withImgSrc(imgSrcs: Set[String]) = copy(imgSrc = imgSrcs)
  def addImgSrc(imgSrcs: String*) = withImgSrc( imgSrc ++ imgSrcs )

  def withConnectSrc(connectSrcs: Set[String]) = copy(connectSrc = connectSrcs)
  def addConnectSrc(connectSrcs: String*) = withConnectSrc( connectSrc ++ connectSrcs )

  def withFontSrc(fontSrcs: Set[String]) = copy(fontSrc = fontSrcs)
  def withObjectSrc(objectSrcs: Set[String]) = copy(objectSrc = objectSrcs)
  def withMediaSrc(mediaSrcs: Set[String]) = copy(mediaSrc = mediaSrcs)
  @deprecated("Directive ‘frame-src’ has been deprecated. Please use directive ‘child-src’ instead.", "CSP Level 2")
  def withFrameSrc(frameSrcs: Set[String]) = copy(frameSrc = frameSrcs)
  def withReportUri(reportUri1: String = null) = copy(reportUri = Option(reportUri1))

  // CSP L2
  def withChildSrc(childSrcs: Set[String]) = copy(childSrc = childSrcs)
  def addChildSrc(childSrcs: String*) = withChildSrc( childSrc ++ childSrcs )

  def withFormAction(formActions: Set[String]) = copy(formAction = formActions)
  def withFrameAncestors(frameAncestors1: Set[String]) = copy(frameAncestors = frameAncestors1)
  def withPluginTypes(pluginTypes1: Set[String]) = copy(pluginTypes = pluginTypes1)

  // TODO Вынести весь код ниже в контроллер или утиль.


  /** Разрешить доступ для OSM-карт для leaflet.js. */
  def allowOsmLeaflet: CspPolicy = {
    addImgSrc( "*.tile.openstreetmap.org" )
  }

  /** Umap-карта на базе Leaflet. */
  def allowUmap: CspPolicy = {
    allowOsmLeaflet
      .addScriptSrc( Csp.Sources.UNSAFE_EVAL, Csp.Sources.UNSAFE_INLINE )
      // Чтобы можно было менять подложку карты на любую: их много, они перечислены в шаблоне mapBaseTpl.
      .withImgSrc( Set( Csp.Sources.* ) )
  }

  def jsUnsafeInline: CspPolicy = {
    addScriptSrc( Csp.Sources.UNSAFE_INLINE )
  }

  def styleUnsafeInline: CspPolicy = {
    addStyleSrc( Csp.Sources.UNSAFE_INLINE )
  }

  override def toString = CspPolicy.format(this)

}


/** Модель отчёта о нарушении безопасности.
  *
  * @param documentUri URL страницы.
  * @param referrer Реферер, если есть.
  * @param blockedUri Заблокированный запрос.
  * @param violatedDirective Нарушенная директива.
  * @param originalPolicy Политика безопасности.
  */
case class CspViolationReport(
                               documentUri        : String,
                               referrer           : Option[String],
                               blockedUri         : Option[String],
                               violatedDirective  : String,
                               originalPolicy     : String
                             )
