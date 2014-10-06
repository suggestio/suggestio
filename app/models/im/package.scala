package models

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.14 15:50
 * Description:
 */
package object im {

  // Im enum-типы для параметров трансформации картинок.
  type ImGravity            = ImGravities.ImGravity
  type ImResizeFlag         = ImResizeFlags.ImResizeFlag
  type ImInterlacing        = ImInterlace.ImInterlacing
  type ImOpCode             = ImOpCodes.ImOpCode

  type BasicScreenSize      = BasicScreenSizes.BasicScreenSize

  type ImFilter             = ImFilters.ImFilter

  type DevPixelRatio        = DevPixelRatios.DevPixelRatio

}
