package io.suggest.vid.ext

import io.suggest.vid.ext.vimeo.VimeoUrlParsers
import io.suggest.vid.ext.youtube.YouTubeUrlParsers

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.10.17 18:45
  * Description: Подсистема парсинга ссылок на видео-сервисы.
  */

trait VideoExtUrlParsersT
  extends YouTubeUrlParsers
  with VimeoUrlParsers
{

  /** Парсер ссылки всеми возможными сервис-парсерами. */
  def anySvcVideoUrlP: Parser[MVideoExtInfo] = {
    youtubeUrl2videoIdP | vimeoUrl2VideoP
  }

}


class VideoExtUrlParsers extends VideoExtUrlParsersT
