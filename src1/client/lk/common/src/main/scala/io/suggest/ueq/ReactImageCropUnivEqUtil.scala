package io.suggest.ueq

import com.github.dominictobias.react.image.crop.{PercentCrop, PixelCrop}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 11:34
  * Description: UnivEq support for react-image-crop types.
  */
object ReactImageCropUnivEqUtil {

  @inline implicit def percentCropUe: UnivEq[PercentCrop] = UnivEq.force

  @inline implicit def pixelCropUe: UnivEq[PixelCrop] = UnivEq.force

}
