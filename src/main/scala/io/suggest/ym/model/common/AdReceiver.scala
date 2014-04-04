package io.suggest.ym.model.common

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:09
 * Description: Абстрактный получатель публикуемой рекламы. Например ТЦ.
 */

trait AdReceiverStatic[T <: AdNetMemberCombo[T]] extends AdNetMemberComboStatic[T]

trait AdReceiver[T <: AdNetMemberCombo[T]] extends AdNetMemberCombo[T] {
  def isAdReceiver: Boolean = true
}
