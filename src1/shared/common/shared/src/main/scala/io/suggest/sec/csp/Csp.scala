package io.suggest.sec.csp

import io.suggest.common.html.HtmlConstants
import io.suggest.proto.http.HttpConst

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
