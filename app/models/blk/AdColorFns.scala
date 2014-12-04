package models.blk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.11.14 10:51
 * Description: MAd.colors содержит карту цветов. Здесь перечислены ключи этой карты цветов.
 */
object AdColorFns extends Enumeration {

  type AdColorFn = Value

  val IMG_BG_COLOR_FN           : AdColorFn = Value("ibgc")
  val WIDE_IMG_PATTERN_COLOR_FN : AdColorFn = Value("iwp")

}
