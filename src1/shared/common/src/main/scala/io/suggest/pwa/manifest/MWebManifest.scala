package io.suggest.pwa.manifest

import io.suggest.ico.MIconInfo
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 17:43
  * Description: JSON-модель для стандартного json-манифеста вёб-приложения.
  *
  * {{{
  * {
  *  "name": "HackerWeb",
  *  "short_name": "HackerWeb",
  *  "start_url": ".",
  *  "display": "standalone",
  *  "background_color": "#fff",
  *  "description": "A simply readable Hacker News app.",
  *  "icons": [{
  *      "src": "images/touch/homescreen48.png",
  *      "sizes": "48x48",
  *      "type": "image/png"
  *    }, {
  *      "src": "icon/hd_hi.ico",
  *      "sizes": "72x72 96x96 128x128 256x256"
  *    }, ...
  *  ],
  *  "related_applications": [{
  *    "platform": "play",
  *    "url": "https://play.google.com/store/apps/details?id=cheeaun.hackerweb"
  *  }]
  * }
  * }}}
  *
  */
object MWebManifest {

  implicit def univEq: UnivEq[MWebManifest] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  implicit def MWEB_MANIFEST_WRITES: OWrites[MWebManifest] = (
    (__ \ "name").write[String] and
    (__ \ "short_name").writeNullable[String] and
    (__ \ "start_url").write[String] and
    (__ \ "display").writeNullable[MPwaDisplayMode] and
    (__ \ "background_color").writeNullable[String] and
    (__ \ "description").writeNullable[String] and
    (__ \ "dir").writeNullable[MPwaTextDirection] and
    (__ \ "icons").write[Seq[MIconInfo]] and
    (__ \ "related_applications")
      .writeNullable[Seq[MPwaRelApp]]
      .contramap[Seq[MPwaRelApp]] { relApps =>
        if (relApps.isEmpty) None else Some(relApps)
      }
  )(unlift(unapply))

}

/** Класс манифеста для web-приложения.
  *
  * @param name Название приложения.
  * @param shortName Короткое название.
  * @param startUrl Ссылка при запуске.
  * @param display Режим отображения на экране устройства.
  * @param backgroundColor Цвет фона.
  * @param description Описание приложения.
  * @param textDirection Направление текста.
  * @param icons Иконки вёб-приложения.
  * @param relatedApps Связанные (по смыслу) приложения.
  */
case class MWebManifest(
                         name             : String,
                         shortName        : Option[String]              = None,
                         startUrl         : String,
                         display          : Option[MPwaDisplayMode]     = None,
                         backgroundColor  : Option[String]              = None,
                         description      : Option[String]              = None,
                         textDirection    : Option[MPwaTextDirection]   = None,
                         icons            : Seq[MIconInfo],
                         relatedApps      : Seq[MPwaRelApp]             = Nil
                       )
