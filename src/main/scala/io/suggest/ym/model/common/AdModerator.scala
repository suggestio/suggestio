package io.suggest.ym.model.common

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:11
 * Description: Звено, имеющее возможность модерировать рекламные карточки. Например ТЦ.
 */

trait AdModerator[T <: AdNetMemberCombo[T]] extends AdNetMemberCombo {
}
