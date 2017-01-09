package io.suggest.xadv.ext.js.vk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 14:48
 */
package object m {

  type VkTargetType = VkTargetTypes.T

  /** Тип для хранения и передачи значения vk id. Это целое число, которое *в теории* может быть больше чем 2,4 млрд.
   * Long нельзя (хромает десериализация), String нельзя (гемора многовато).
   * Можно Double, если Int не хватит. */
  type UserId_t     = Int

  type VkPerm       = VkPerms.T

}
