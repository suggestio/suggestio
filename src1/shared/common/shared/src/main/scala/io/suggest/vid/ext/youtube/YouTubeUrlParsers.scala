package io.suggest.vid.ext.youtube

import io.suggest.url.UrlParsers
import io.suggest.vid.ext.{MVideoExtInfo, MVideoServices}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.10.17 14:28
  * Description: Парсинг ссылок на ютуб.
  * @see [[https://stackoverflow.com/a/6904504]]
  */
trait YouTubeUrlParsers extends UrlParsers {

  private def ytDomainPathGarbageP: Parser[String] = {
    "(?i)(?:youtube(?:-nocookie)?\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)".r
  }

  private def ytVideoIdP: Parser[MVideoExtInfo] = {
    """[^"&?/ ]{6,12}""".r ^^ { videoId =>
      MVideoExtInfo(
        videoService = MVideoServices.YouTube,
        remoteId     = videoId
      )
    }
  }

  /** Итоговый парсер различных youtube-ссылок на видео. */
  def youtubeUrl2videoIdP: Parser[MVideoExtInfo] = {
    protoOpt ~> wwwPrefixOpt ~> ytDomainPathGarbageP ~> ytVideoIdP
  }

}
