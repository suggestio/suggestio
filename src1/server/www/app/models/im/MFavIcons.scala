package models.im

import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.html.HtmlConstants
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.ico.{MIconInfo, MLinkRelIcon}
import io.suggest.img.MImgFmts

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 21:13
  * Description: Статическая модель иконок suggest.io.
  * Содержит в себе центральную спеку для сборки списков мета-тегов favicons, web-app манифеста, итд.
  */
object MFavIcons {

  val FAVICON_URL_PREFIX = "images/favicon/"

  /** Спека для статических link-rel-иконок.
    *
    * @return Список иконок для link-rel-рендера.
    */
  def linkRelIcons: Seq[MLinkRelIcon] = {
    val rels = HtmlConstants.Links.Rels

    val appleTouchIconRels = List( rels.APPLE_TOUCH_ICON )
    val png = MImgFmts.PNG
    val pngMime = png.mime
    val pngFileExt = png.fileExt

    // TODO Надо подчистить размеры и иконки. Выявить реально необходимые.
    var iconsAcc = for {
      sideSzPx <- List(57, 72, 114, 144, 180)
    } yield {
      MLinkRelIcon(
        icon = MIconInfo(
          src       = FAVICON_URL_PREFIX + sideSzPx + `.` + pngFileExt,
          sizes     = List( MSize2di.square(sideSzPx) ),
          mimeType  = pngMime
        ),
        rels = appleTouchIconRels
      )
    }

    val iconRels = List(rels.ICON)

    // 192x192 png -- обязательный размер для андройда/хрома https://developers.google.com/web/fundamentals/app-install-banners/
    for (sideSz <- List(192, 228, 512)) yield {
      iconsAcc ::= MLinkRelIcon(
        icon = MIconInfo(
          src       = FAVICON_URL_PREFIX + sideSz + `.` + pngFileExt,
          sizes     = List( MSize2di.square(sideSz) ),
          mimeType  = pngMime
        ),
        rels = iconRels
      )
    }

    val svg = MImgFmts.SVG
    iconsAcc ::= MLinkRelIcon(
      icon = MIconInfo(
        src      = FAVICON_URL_PREFIX + "sio" + `.` + svg.fileExt,
        sizes    = Nil,
        mimeType = svg.mime
      ),
      rels = iconRels
    )

    iconsAcc
  }

}
