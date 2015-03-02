package io.suggest.xadv.ext.js.vk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 14:48
 */
package object m {

  type VkTargetType = VkTargetTypes.T

  /** Тип для хранения и передачи значения vk id. Это целое число, которое *в теории* может быть больше чем 2,4 млрд.
   * Long нельзя (sjs unsupported), String нельзя (гемора много). Можно Double, если int не хватит. */
  type UserId_t     = Int

}
