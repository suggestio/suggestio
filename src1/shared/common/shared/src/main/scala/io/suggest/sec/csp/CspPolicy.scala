package io.suggest.sec.csp

import io.suggest.common.empty.EmptyProduct
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.05.2020 15:31
  * Description: Модель описания CSP-политики.
  */
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
    def __append(k: String, vs: IterableOnce[String]): Unit = {
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


  // CSP Level 1
  def defaultSrc = GenLens[CspPolicy]( _.defaultSrc )
  def scriptSrc = GenLens[CspPolicy]( _.scriptSrc )
  def styleSrc = GenLens[CspPolicy]( _.styleSrc )
  def imgSrc = GenLens[CspPolicy]( _.imgSrc )
  def connectSrc = GenLens[CspPolicy]( _.connectSrc )
  def fontSrc = GenLens[CspPolicy]( _.fontSrc )
  def objectSrc = GenLens[CspPolicy]( _.objectSrc )
  def mediaSrc = GenLens[CspPolicy]( _.mediaSrc )
  @deprecated("Directive ‘frame-src’ has been deprecated. Please use directive ‘child-src’ instead.", "CSP Level 2")
  def frameSrc = GenLens[CspPolicy]( _.frameSrc )
  def reportUri = GenLens[CspPolicy]( _.reportUri )
  // CSP Level 2
  def childSrc = GenLens[CspPolicy]( _.childSrc )
  def formAction = GenLens[CspPolicy]( _.formAction )
  def frameAncestors = GenLens[CspPolicy]( _.frameAncestors )
  def pluginTypes = GenLens[CspPolicy]( _.pluginTypes )
  def workerSrc = GenLens[CspPolicy]( _.workerSrc )


  def allowOsmLeaflet =
    CspPolicy.imgSrc.modify(_ + "*.tile.openstreetmap.org")

  /** Umap-карта на базе Leaflet. */
  def allowUmap = (
    allowOsmLeaflet andThen
      CspPolicy.scriptSrc.modify(_ + Csp.Sources.UNSAFE_EVAL + Csp.Sources.UNSAFE_INLINE) andThen
      // Чтобы можно было менять подложку карты на любую: их много, они перечислены в шаблоне mapBaseTpl.
      CspPolicy.imgSrc.set( Set.empty + Csp.Sources.* )
    )

  def jsUnsafeInline =
    CspPolicy.scriptSrc.modify(_ + Csp.Sources.UNSAFE_INLINE)

  def styleUnsafeInline =
    CspPolicy.styleSrc.modify(_ + Csp.Sources.UNSAFE_INLINE)

  @inline implicit def univEq: UnivEq[CspPolicy] = UnivEq.derive

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
final case class CspPolicy(
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

  override def toString = CspPolicy.format(this)

}
