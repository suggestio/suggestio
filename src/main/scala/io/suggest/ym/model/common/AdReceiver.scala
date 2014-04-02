package io.suggest.ym.model.common

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:09
 * Description: Абстрактный получатель публикуемой рекламы. Например ТЦ.
 */

trait AdReceiverStatic[T <: AdNetMember[T]] extends AdNetMemberStatic[T]

trait AdReceiver[T <: AdNetMember[T]] extends AdNetMember[T]
