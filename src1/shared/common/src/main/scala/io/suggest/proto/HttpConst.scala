package io.suggest.proto

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
    final def LOCATION              = "Location"
    final def CONTENT_TYPE          = Words.Content_ + "Type"
    final def CONTENT_LENGHT        = Words.Content_ + "Lenght"
    final def CONNECTION            = "Connection"
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

    private def _coloned(s: String) = s + HtmlConstants.COLON

    final def HTTP = "http"
    final def HTTP_ = _coloned(HTTP)

    final def HTTPS = HTTP + "s"
    final def HTTPS_ = _coloned(HTTPS)

    final def CURR_PROTO = "//"
    final def DELIM = ":" + CURR_PROTO

  }

}
