package io.suggest.ym.model.common

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:10
 * Description: Некто, имеющий право влиять на состав рекламной подсети. Супервайзер "сети", например ТЦ.
 */

trait AdNetSupervisor[T <: AdNetMember[T]] extends AdNetMember[T] {
}
