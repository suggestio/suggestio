package io.suggest.vid.ext.vimeo

import io.suggest.url.UrlParsers
import io.suggest.vid.ext.{MVideoExtInfo, MVideoServices}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.10.17 16:55
  * Description: Парсер для ссылок vimeo.
  */
trait VimeoUrlParsers extends UrlParsers {

  private def vmUrlP: Parser[String] = {
    "(?:player\\.)?vimeo.com/(?:channels/(?:\\w+/)?|groups/([^/]*)/videos/|album/(\\d+)/video/|video/|)".r
  }

  private def vmVideoIdP: Parser[MVideoExtInfo] = {
    "\\d+".r ^^ { videoIdStr =>
      MVideoExtInfo(
        videoService = MVideoServices.Vimeo,
        remoteId     = videoIdStr
      )
    }
  }

  /** Парсер ссылки на видео, размещенное в vimeo. */
  def vimeoUrl2VideoP: Parser[MVideoExtInfo] = {
    protoOpt ~> wwwPrefixOpt ~> vmUrlP ~> vmVideoIdP
  }

}
