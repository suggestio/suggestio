package io.suggest.vid.ext

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.10.17 15:02
  * Description: Модель поддерживаемых видео-сервисов.
  */
object MVideoServices extends StringEnum[MVideoService] {

  case object YouTube extends MVideoService("yt") {
    override def iframeSrc(videoId: String): String = {
      "https://www.youtube.com/embed/" + videoId
    }
  }

  case object Vimeo extends MVideoService("vm") {
    override def iframeSrc(videoId: String): String = {
      "https://player.vimeo.com/video/" + videoId
    }
  }


  override def values = findValues

}


sealed abstract class MVideoService(override val value: String) extends StringEnumEntry {

  /** Сборка iframe-ссылки. */
  def iframeSrc(videoId: String): String

}


object MVideoService {

  @inline implicit def univEq: UnivEq[MVideoService] = UnivEq.derive

  /** Поддержка play-json, если вдруг когда-нибудь пригодится. */
  implicit def MVIDEO_SERVICE_FORMAT: Format[MVideoService] = {
    EnumeratumUtil.valueEnumEntryFormat( MVideoServices )
  }

}

