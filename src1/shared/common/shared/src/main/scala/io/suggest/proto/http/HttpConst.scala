package io.suggest.proto.http

import io.suggest.common.html.HtmlConstants

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.09.17 22:11
  * Description: Константы HTTP-протокола.
  */
object HttpConst {

  object Words {
    final def Content_ = "Content-"
  }


  object Headers {
    final def ACCEPT                = "Accept"
    final def ACCEPT_ENCODING       = ACCEPT + "-Encoding"
    def accept(mimeTypes: String*)  = ACCEPT -> mimeTypes.mkString(", ")
    final def LOCATION              = "Location"
    final def CONTENT_TYPE          = Words.Content_ + "Type"
    final def CONTENT_LENGHT        = Words.Content_ + "Lenght"
    final def CONNECTION            = "Connection"
    final def BOUNDARY              = "Boundary"
    final def RANGE                 = "Range"
    final def CONTENT_RANGE         = Words.Content_ + RANGE
    final def COOKIE                = "Cookie"
    final def SET_COOKIE            = "Set-Cookie"
    final def IDEMPOTENCE_KEY       = "Idempotence-Key"

    object XRequestedWith {
      final def XRW_VALUE           = "XHR"
      final def XRW_APP_SUFFIX      = "+APP"
      final def XRW_NAME            = "X-Requested-With"
    }
  }


  object Methods {
    final def GET     = "GET"
    final def POST    = "POST"
    final def PUT     = "PUT"
    final def DELETE  = "DELETE"
  }


  /** See StandardValues.scala */
  object Status {

    final val OK = 200
    final val CREATED = 201
    final val ACCEPTED = 202
    final val NON_AUTHORITATIVE_INFORMATION = 203
    final val NO_CONTENT = 204
    final val RESET_CONTENT = 205
    final val PARTIAL_CONTENT = 206
    final val MULTI_STATUS = 207

    final val MULTIPLE_CHOICES = 300
    final val MOVED_PERMANENTLY = 301
    final val FOUND = 302
    final val SEE_OTHER = 303
    final val NOT_MODIFIED = 304
    final val USE_PROXY = 305
    final val TEMPORARY_REDIRECT = 307
    final val PERMANENT_REDIRECT = 308

    final val BAD_REQUEST = 400
    final val UNAUTHORIZED = 401
    final val PAYMENT_REQUIRED = 402
    final val FORBIDDEN = 403
    final val NOT_FOUND = 404
    final val METHOD_NOT_ALLOWED = 405
    final val NOT_ACCEPTABLE = 406
    final val PROXY_AUTHENTICATION_REQUIRED = 407
    final val REQUEST_TIMEOUT = 408
    final val CONFLICT = 409
    final val GONE = 410
    final val LENGTH_REQUIRED = 411
    final val PRECONDITION_FAILED = 412
    final val REQUEST_ENTITY_TOO_LARGE = 413
    final val REQUEST_URI_TOO_LONG = 414
    final val UNSUPPORTED_MEDIA_TYPE = 415
    final val REQUESTED_RANGE_NOT_SATISFIABLE = 416
    final val EXPECTATION_FAILED = 417
    final val UNPROCESSABLE_ENTITY = 422
    final val LOCKED = 423
    final val FAILED_DEPENDENCY = 424
    final val UPGRADE_REQUIRED = 426
    final val TOO_MANY_REQUESTS = 429

  }


  object Proto {

    final def COLON = HtmlConstants.COLON

    private def _coloned(s: String) = s + COLON

    final def HTTP = "http"
    final def HTTP_ = _coloned(HTTP)

    final def SECURE_SUFFIX = "s"

    final def HTTPS = HTTP + SECURE_SUFFIX
    final def HTTPS_ = _coloned(HTTPS)

    def httpOrHttps(secure: Boolean): String = if (secure) HTTPS else HTTP

    final def CURR_PROTO = {
      val s = HtmlConstants.SLASH
      s + s
    }
    final def DELIM = COLON + CURR_PROTO

    final def WS = "ws"
    final def WSS = WS + SECURE_SUFFIX

    def wsOrWss(secure: Boolean): String = if (secure) WSS else WS

    def BLOB = "blob"
    def BLOB_ = BLOB + COLON

  }

}
