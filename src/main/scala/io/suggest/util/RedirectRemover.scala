package io.suggest.util

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.02.13 11:16
 * Description: Removing redirects from URLs. Algorithm originally based on firefox thread-unsafe addon "redirect remover".
 * TODO STUB !!!!
 */
object RedirectRemover {
  val rot13a = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
  val rot13b = "nopqrstuvwxyzabcdefghijklmNOPQRSTUVWXYZABCDEFGHIJKLM"

  def do_rdr(url:String, goDeep:Boolean = true) : String = {
    val rdrUrl = new RedirectRemoverUrl(url, goDeep)
    rdrUrl.doRdr
  }

  val rot13re = "(?i)(?:uggcf?|sgc)(?:://|%3a%2f%2f|%253a%252f%252f)".r

  // Далее - TODO. Нужно портировать сюда sio_url_norm_rdr или же целиком redirect remover запускать в js-vm
  class RedirectRemoverUrl(url0:String, goDeep:Boolean) {
    /*var ret : AnyRef = _
    var re1 : String = _
    var link = url0 */

    def doRdr() : String = {
      url0
    }
      /*
      link match {
        case rot13re(matched, _) =>
          re1 = matched
          link = deRot13(link)

        case _ =>
      }


    }

    // деротация скрытой ссылки
    private def deRot13(input:String) : String = {

    }*/
  }

}
