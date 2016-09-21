package models

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.14 15:50
 */
package object im {

  // Im enum-типы для параметров трансформации картинок.
  type ImGravity            = ImGravities.T
  type ImResizeFlag         = ImResizeFlags.ImResizeFlag
  type ImInterlacing        = ImInterlace.T
  type ImOpCode             = ImOpCodes.T

  type BasicScreenSize      = BasicScreenSizes.BasicScreenSize

  type ImFilter             = ImFilters.ImFilter

  type DevPixelRatio        = DevPixelRatios.T

  type ImSamplingFactor     = ImSamplingFactors.ImSamplingFactor

  type OutImgFmt            = OutImgFmts.T

  type CompressMode         = CompressModes.T

}
