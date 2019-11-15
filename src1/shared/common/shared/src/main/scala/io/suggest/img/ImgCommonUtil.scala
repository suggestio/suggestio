package io.suggest.img

import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.MSzMult
import io.suggest.jd.MJdConf
import io.suggest.jd.tags.JdTag

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.2019 23:26
  * Description:
  */
object ImgCommonUtil {

  /** Как выравнивать фон блока? По горизонтали или по вертикали?
    * С доступом к промежуточным вычислениям.
    *
    * @param blockHeightPx
    * @param origWh
    * @param jdt
    * @param jdConf
    * @param wideSzMultOpt
    */
  case class isUseWidthForBlockBg( blockHeightPx: Int,
                                   origWh: MSize2di,
                                   jdt: JdTag,
                                   jdConf: MJdConf,
                                   wideSzMultOpt: Option[MSzMult],
                                 ) {


    // Нужно понять, как правильно выравнивать картинку по размерам: по ширине или по высоте.
    // Сопоставить размеры контейнера и размеры отмасштабированной под контейнер картинки.
    val contWidthPx = jdt.props1.widthPx
      .filter { _ => jdt.props1.expandMode.isEmpty }
      .fold( jdConf.plainWideBlockWidthPx )( jdConf.szMultF(_, wideSzMultOpt) )

    // Отношение размера блока и размера картинки по горизонтали.
    val img2BlockRatioW = contWidthPx.toDouble / origWh.width.toDouble

    // Отношение размера блока и размера картинки по вертикали.
    val blockHeightMultedPx = jdConf.szMultF(blockHeightPx, wideSzMultOpt)
    val img2BlockRatioH = blockHeightMultedPx.toDouble / origWh.height.toDouble

    val isUseWidth = img2BlockRatioW > img2BlockRatioH

    println( productIterator.mkString(" | "), "contWidthPx=" + contWidthPx.toString )

  }

}
