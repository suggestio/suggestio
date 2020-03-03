package models.im

import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.html.HtmlConstants
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

  /** Спека для статических link-rel-иконок. */
  case class Icons() {

    val FAVICON_URL_PREFIX = "images/favicon/"

    val rels = HtmlConstants.Links.Rels

    val appleTouchIconRels = rels.APPLE_TOUCH_ICON :: Nil
    val png = MImgFmts.PNG
    val pngMime = png.mime
    val pngFileExt = png.fileExt


    lazy val appleTouchIcon512 ={
      val sideSzPx = 512
      MLinkRelIcon(
        icon = MIconInfo(
          src       = s"$FAVICON_URL_PREFIX$sideSzPx-${rels.APPLE_TOUCH}.$pngFileExt",
          sizes     = MSize2di.square(sideSzPx) :: Nil,
          mimeType  = pngMime,
        ),
        rels = appleTouchIconRels,
        imgFmt = png,
      )
    }

    val iconRels = rels.ICON :: Nil

    // 192x192 png -- обязательный размер для андройда/хрома https://developers.google.com/web/fundamentals/app-install-banners/
    lazy val pngIcons = (for {
      sideSz <- (192 :: 228 :: 512 :: Nil).iterator
    } yield {
      MLinkRelIcon(
        icon = MIconInfo(
          src       = s"$FAVICON_URL_PREFIX$sideSz.$pngFileExt",
          sizes     = MSize2di.square(sideSz) :: Nil,
          mimeType  = pngMime,
        ),
        rels = iconRels,
        imgFmt = png,
      )
    })
      .to( LazyList )

    // Иконка SVG.
    lazy val svgIcon = {
      val svg = MImgFmts.SVG
      val svgMime = svg.mime
      MLinkRelIcon(
        icon = MIconInfo(
          src      = s"${FAVICON_URL_PREFIX}sio.${svg.fileExt}",
          sizes    = Nil,
          mimeType = svgMime,
        ),
        rels = iconRels,
        imgFmt = svg,
      )
    }

    /** Список иконок для link-rel-рендера. */
    val allIcons: Seq[MLinkRelIcon] = {
      svgIcon #::
      appleTouchIcon512 #::
      pngIcons
    }

  }

}
